/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package io.grakn.test.graql.analytics;

import io.grakn.Grakn;
import io.grakn.concept.Entity;
import io.grakn.concept.EntityType;
import io.grakn.concept.Instance;
import io.grakn.concept.RelationType;
import io.grakn.concept.Resource;
import io.grakn.concept.ResourceType;
import io.grakn.concept.RoleType;
import io.grakn.exception.GraknValidationException;
import io.grakn.graql.ComputeQuery;
import io.grakn.graql.QueryBuilder;
import io.grakn.graql.internal.analytics.Analytics;
import io.grakn.graql.internal.util.GraqlType;
import io.grakn.test.AbstractGraphTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.grakn.graql.Graql.or;
import static io.grakn.graql.Graql.var;
import static io.grakn.graql.Graql.withGraph;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class GraqlTest extends AbstractGraphTest {

    private QueryBuilder qb;

    @Before
    public void setUp() {
        // TODO: Make orientdb support analytics
        assumeFalse(usingOrientDB());
        qb = withGraph(graph);
    }

    @Test
    public void testGraqlCount() throws GraknValidationException, InterruptedException, ExecutionException {

        // assert the graph is empty
        Analytics computer = new Analytics(graph.getKeyspace(),new HashSet<>(),new HashSet<>());
        assertEquals(0, computer.count());

        // create 3 instances
        EntityType thing = graph.putEntityType("thing");
        graph.putEntity("1", thing);
        graph.putEntity("2", thing);
        graph.putEntity("3", thing);
        graph.commit();

        long graqlCount = qb.match(
                var("x").isa(var("y")),
                or(var("y").isa("entity-type"), var("y").isa("resource-type"), var("y").isa("relation-type"))
        ).stream().count();

        long computeCount = ((Long) ((ComputeQuery) qb.parse("compute count;")).execute());

        assertEquals(graqlCount, computeCount);
        assertEquals(3L, computeCount);

        computeCount = ((Long) ((ComputeQuery) qb.parse("compute count;")).execute());

        assertEquals(graqlCount, computeCount);
        assertEquals(3L, computeCount);
    }

    @Test
    public void testGraqlCountSubgraph() throws Exception {
        // create 3 instances
        EntityType thing = graph.putEntityType("thing");
        EntityType anotherThing = graph.putEntityType("another");
        graph.putEntity("1", thing);
        graph.putEntity("2", thing);
        graph.putEntity("3", anotherThing);
        graph.commit();

        long computeCount = ((Long) ((ComputeQuery) qb.parse("compute count in thing, thing;")).execute());
        assertEquals(2, computeCount);
    }


    @Test
    public void testDegrees() throws Exception {
        // TODO: Fix on TinkerGraphComputer
        assumeFalse(usingTinker());

        // create 3 instances
        EntityType thing = graph.putEntityType("thing");
        String entity1 = graph.addEntity(thing).getId();
        String entity2 = graph.addEntity(thing).getId();
        String entity3 = graph.addEntity(thing).getId();
        String entity4 = graph.addEntity(thing).getId();

        RoleType role1 = graph.putRoleType("role1");
        RoleType role2 = graph.putRoleType("role2");
        thing.playsRole(role1).playsRole(role2);
        RelationType related = graph.putRelationType("related").hasRole(role1).hasRole(role2);

        // relate them
        String id1 = graph.addRelation(related)
                .putRolePlayer(role1, graph.getInstance(entity1))
                .putRolePlayer(role2, graph.getInstance(entity2))
                .getId();

        String id2 = graph.addRelation(related)
                .putRolePlayer(role1, graph.getInstance(entity2))
                .putRolePlayer(role2, graph.getInstance(entity3))
                .getId();

        String id3 = graph.addRelation(related)
                .putRolePlayer(role1, graph.getInstance(entity2))
                .putRolePlayer(role2, graph.getInstance(entity4))
                .getId();

        graph.commit();

        // compute degrees
        Map<Long, Set<String>> degrees = ((Map) qb.parse("compute degrees;").execute());

        // assert degrees are correct
        Map<String, Long> correctDegrees = new HashMap<>();
        correctDegrees.put(entity1, 1l);
        correctDegrees.put(entity2, 3l);
        correctDegrees.put(entity3, 1l);
        correctDegrees.put(entity4, 1l);
        correctDegrees.put(id1, 2l);
        correctDegrees.put(id2, 2l);
        correctDegrees.put(id3, 2l);

        assertTrue(!degrees.isEmpty());
        degrees.entrySet().forEach(entry -> entry.getValue().forEach(
                id -> {
                    assertTrue(correctDegrees.containsKey(id));
                    assertEquals(correctDegrees.get(id), entry.getKey());
                }
        ));
    }

    @Test
    public void testDegreesAndPersist() throws Exception {
        // TODO: Fix on TinkerGraphComputer
        assumeFalse(usingTinker());

        // create 3 instances
        EntityType thing = graph.putEntityType("thing");
        Entity entity1 = graph.putEntity("1", thing);
        Entity entity2 = graph.putEntity("2", thing);
        Entity entity3 = graph.putEntity("3", thing);
        Entity entity4 = graph.putEntity("4", thing);

        RoleType relation1 = graph.putRoleType("relation1");
        RoleType relation2 = graph.putRoleType("relation2");
        thing.playsRole(relation1).playsRole(relation2);
        RelationType related = graph.putRelationType("related").hasRole(relation1).hasRole(relation2);

        // relate them
        String id1 = UUID.randomUUID().toString();
        graph.putRelation(id1, related)
                .putRolePlayer(relation1, entity1)
                .putRolePlayer(relation2, entity2);

        String id2 = UUID.randomUUID().toString();
        graph.putRelation(id2, related)
                .putRolePlayer(relation1, entity2)
                .putRolePlayer(relation2, entity3);

        String id3 = UUID.randomUUID().toString();
        graph.putRelation(id3, related)
                .putRolePlayer(relation1, entity2)
                .putRolePlayer(relation2, entity4);

        graph.commit();

        // compute degrees
        ((ComputeQuery) qb.parse("compute degreesAndPersist;")).execute();

        // assert persisted degrees are correct
//        GraknGraph graph = GraknGraphFactoryImpl.getGraph(keyspace);
        entity1 = graph.getEntity("1");
        entity2 = graph.getEntity("2");
        entity3 = graph.getEntity("3");
        entity4 = graph.getEntity("4");

        Map<Instance, Long> correctDegrees = new HashMap<>();
        correctDegrees.put(entity1, 1l);
        correctDegrees.put(entity2, 3l);
        correctDegrees.put(entity3, 1l);
        correctDegrees.put(entity4, 1l);
        correctDegrees.put(graph.getRelation(id1), 2l);
        correctDegrees.put(graph.getRelation(id2), 2l);
        correctDegrees.put(graph.getRelation(id3), 2l);

        correctDegrees.entrySet().forEach(degree -> {
            Instance instance = degree.getKey();
            Collection<Resource<?>> resources = null;
            if (instance.isEntity()) {
                resources = instance.asEntity().resources();
            } else if (instance.isRelation()) {
                resources = instance.asRelation().resources();
            }
            assertTrue(resources.iterator().next().getValue().equals(degree.getValue()));
        });

        // compute degrees again
        ((ComputeQuery) qb.parse("compute degreesAndPersist;")).execute();

        // assert persisted degrees are correct
        graph = Grakn.factory(Grakn.DEFAULT_URI, graph.getKeyspace()).getGraph();
        entity1 = graph.getEntity("1");
        entity2 = graph.getEntity("2");
        entity3 = graph.getEntity("3");
        entity4 = graph.getEntity("4");

        correctDegrees = new HashMap<>();
        correctDegrees.put(entity1, 1l);
        correctDegrees.put(entity2, 3l);
        correctDegrees.put(entity3, 1l);
        correctDegrees.put(entity4, 1l);
        correctDegrees.put(graph.getRelation(id1), 2l);
        correctDegrees.put(graph.getRelation(id2), 2l);
        correctDegrees.put(graph.getRelation(id3), 2l);

        correctDegrees.entrySet().forEach(degree -> {
            Instance instance = degree.getKey();
            Collection<Resource<?>> resources = null;
            if (instance.isEntity()) {
                resources = instance.asEntity().resources();
            } else if (instance.isRelation()) {
                resources = instance.asRelation().resources();
            }
            assertTrue(resources.iterator().next().getValue().equals(degree.getValue()));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidIdWithAnalytics() {
        ((ComputeQuery) qb.parse("compute sum of thing;")).execute();
    }

    @Test
    public void testStatisticsMethods() throws GraknValidationException {
        // TODO: Fix on TinkerGraphComputer
        assumeFalse(usingTinker());

        String resourceTypeId = "resource";

        RoleType resourceOwner = graph.putRoleType(GraqlType.HAS_RESOURCE_OWNER.getId(resourceTypeId));
        RoleType resourceValue = graph.putRoleType(GraqlType.HAS_RESOURCE_VALUE.getId(resourceTypeId));
        RelationType relationType = graph.putRelationType(GraqlType.HAS_RESOURCE.getId(resourceTypeId))
                .hasRole(resourceOwner)
                .hasRole(resourceValue);

        ResourceType<Long> resource = graph.putResourceType(resourceTypeId, ResourceType.DataType.LONG)
                .playsRole(resourceValue);
        EntityType thing = graph.putEntityType("thing").playsRole(resourceOwner);
        Entity theResourceOwner = graph.addEntity(thing);

        graph.addRelation(relationType)
                .putRolePlayer(resourceOwner,theResourceOwner)
                .putRolePlayer(resourceValue,graph.putResource(1L,resource));
        graph.addRelation(relationType)
                .putRolePlayer(resourceOwner,theResourceOwner)
                .putRolePlayer(resourceValue,graph.putResource(2L,resource));
        graph.addRelation(relationType)
                .putRolePlayer(resourceOwner,theResourceOwner)
                .putRolePlayer(resourceValue,graph.putResource(3L,resource));

        graph.commit();

        // use graql to compute various statistics
        Optional<Number> result = (Optional<Number>) ((ComputeQuery) qb.parse("compute sum of resource;")).execute();
        assertEquals(6L,(long) result.orElse(0L));
        result = (Optional<Number>) ((ComputeQuery) qb.parse("compute min of resource;")).execute();
        assertEquals(1L,(long) result.orElse(0L));
        result = (Optional<Number>) ((ComputeQuery) qb.parse("compute max of resource;")).execute();
        assertEquals(3L,(long) result.orElse(0L));
        result = (Optional<Number>) ((ComputeQuery) qb.parse("compute mean of resource;")).execute();
        assertEquals(2.0, (double) result.orElse(0L), 0.1);
        result = (Optional<Number>) ((ComputeQuery) qb.parse("compute median of resource;")).execute();
        assertEquals(2L, (long) result.orElse(0L));

    }

    @Test(expected = IllegalStateException.class)
    public void testNonResourceTypeAsSubgraphForAnalytics() throws GraknValidationException {
        EntityType thing = graph.putEntityType("thing");
        graph.commit();

        ((ComputeQuery) qb.parse("compute sum in thing;")).execute();
    }

    @Test(expected = IllegalStateException.class)
    public void testErrorWhenNoSubgrapForAnalytics() throws GraknValidationException {
        ((ComputeQuery) qb.parse("compute sum;")).execute();
        ((ComputeQuery) qb.parse("compute min;")).execute();
        ((ComputeQuery) qb.parse("compute max;")).execute();
        ((ComputeQuery) qb.parse("compute mean;")).execute();
    }

    @Test
    public void testAnalyticsDoesNotCommitByMistake() throws GraknValidationException {
        // TODO: Fix on TinkerGraphComputer
        assumeFalse(usingTinker());

        graph.putResourceType("number", ResourceType.DataType.LONG);
        graph.commit();

        Set<String> analyticsCommands = new HashSet<String>(Arrays.asList(
                "compute count;",
                "compute degrees;",
                "compute mean of number;"));

        analyticsCommands.forEach(command -> {
            // insert a node but do not commit it
            qb.parse("insert thing isa entity-type;").execute();
            // use analytics
            qb.parse(command).execute();
            // see if the node was commited
            graph.rollback();
            assertNull(graph.getEntityType("thing"));
        });
    }
}
