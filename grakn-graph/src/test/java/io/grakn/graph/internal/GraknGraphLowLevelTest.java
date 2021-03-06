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

package io.grakn.graph.internal;

import io.grakn.Grakn;
import io.grakn.concept.Concept;
import io.grakn.concept.Entity;
import io.grakn.concept.EntityType;
import io.grakn.concept.Instance;
import io.grakn.concept.Relation;
import io.grakn.concept.RelationType;
import io.grakn.concept.Resource;
import io.grakn.concept.ResourceType;
import io.grakn.concept.RoleType;
import io.grakn.concept.RuleType;
import io.grakn.concept.Type;
import io.grakn.exception.GraknValidationException;
import io.grakn.util.Schema;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.VerificationException;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class GraknGraphLowLevelTest {

    private AbstractGraknGraph graknGraph;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void buildGraphAccessManager(){
        graknGraph = (AbstractGraknGraph) Grakn.factory(Grakn.IN_MEMORY, UUID.randomUUID().toString().replaceAll("-", "a")).getGraph();
        graknGraph.initialiseMetaConcepts();
    }
    @After
    public void destroyGraphAccessManager()  throws Exception{
        graknGraph.close();
    }

    @Test
    public void testGetGraph(){
        assertThat(graknGraph.getTinkerPopGraph(), instanceOf(Graph.class));
    }

    @Test
    public void testPutConcept() throws Exception {
        int numVerticies = 14;
        for(int i = 0; i < numVerticies; i ++)
            graknGraph.putEntityType("c" + i);
        assertEquals(22, graknGraph.getTinkerPopGraph().traversal().V().toList().size());
    }

    //----------------------------------------------Concept Functionality-----------------------------------------------
    @Test(expected=RuntimeException.class)
    public void testTooManyNodesForId() {
        Graph graph = graknGraph.getTinkerPopGraph();
        Vertex v1 = graph.addVertex();
        v1.property(Schema.ConceptProperty.ITEM_IDENTIFIER.name(), "value");
        Vertex v2 = graph.addVertex();
        v2.property(Schema.ConceptProperty.ITEM_IDENTIFIER.name(), "value");
        graknGraph.putEntityType("value");
    }

    @Test
    public void testGetConceptByBaseIdentifier() throws Exception {
        assertNull(graknGraph.getConceptByBaseIdentifier(1000L));

        ConceptImpl c1 = (ConceptImpl) graknGraph.putEntityType("c1");
        ConceptImpl c2 = graknGraph.getConceptByBaseIdentifier(c1.getBaseIdentifier());
        assertEquals(c1, c2);
    }

    @Test
    public void testGetConcept() throws Exception {
        Concept c1 = graknGraph.putEntityType("VALUE");
        Concept c2 = graknGraph.getConcept("VALUE");
        assertEquals(c1, c2);
    }

    @Test
    public void testReadOnlyTraversal(){
        expectedException.expect(VerificationException.class);
        expectedException.expectMessage(allOf(
                containsString("not read only")
        ));

        graknGraph.getTinkerTraversal().drop().iterate();
    }

    @Test
    public void testAddCastingLong() {
        //Build It
        RelationType relationType = graknGraph.putRelationType("reltype");
        RoleTypeImpl role = (RoleTypeImpl) graknGraph.putRoleType("Role");
        EntityType thing = graknGraph.putEntityType("thing");
        InstanceImpl rolePlayer = (InstanceImpl) graknGraph.addEntity(thing);
        RelationImpl relation = (RelationImpl) graknGraph.addRelation(relationType);
        CastingImpl casting = graknGraph.putCasting(role, rolePlayer, relation);

        //Check it
        Vertex roleVertex = graknGraph.getTinkerPopGraph().traversal().V(role.getBaseIdentifier()).next();
        Vertex rolePlayerVertex = graknGraph.getTinkerPopGraph().traversal().V(rolePlayer.getBaseIdentifier()).next();
        Vertex assertionVertex = graknGraph.getTinkerPopGraph().traversal().V(relation.getBaseIdentifier()).next();

        org.apache.tinkerpop.gremlin.structure.Edge casting_role = roleVertex.edges(Direction.IN).next();
        org.apache.tinkerpop.gremlin.structure.Edge casting_rolePlayer = rolePlayerVertex.edges(Direction.IN).next();

        assertEquals(casting.getBaseIdentifier(), casting_role.outVertex().id());
        assertEquals(casting.getBaseIdentifier(), casting_rolePlayer.outVertex().id());

        assertEquals(Schema.BaseType.ROLE_TYPE.name(), roleVertex.label());
        assertEquals(Schema.BaseType.RELATION.name(), assertionVertex.label());
    }

    @Test
    public void testAddCastingLongDuplicate(){
        RelationType relationType = graknGraph.putRelationType("reltype");
        RoleTypeImpl role = (RoleTypeImpl) graknGraph.putRoleType("Role");
        EntityType thing = graknGraph.putEntityType("thing");
        InstanceImpl rolePlayer = (InstanceImpl) graknGraph.addEntity(thing);
        RelationImpl relation = (RelationImpl) graknGraph.addRelation(relationType);
        CastingImpl casting1 = graknGraph.putCasting(role, rolePlayer, relation);
        CastingImpl casting2 = graknGraph.putCasting(role, rolePlayer, relation);
        assertEquals(casting1, casting2);
    }

    public void makeArtificialCasting(RoleTypeImpl role, InstanceImpl rolePlayer, RelationImpl relation) {
        String id = "FakeCasting " + UUID.randomUUID();
        Vertex vertex = graknGraph.getTinkerPopGraph().addVertex(Schema.BaseType.CASTING.name());
        vertex.property(Schema.ConceptProperty.ITEM_IDENTIFIER.name(), id);
        vertex.property(Schema.ConceptProperty.INDEX.name(), CastingImpl.generateNewHash(role, rolePlayer));

        CastingImpl casting = (CastingImpl) graknGraph.getConcept(id);
        EdgeImpl edge = casting.addEdge(role, Schema.EdgeLabel.ISA); // Casting to Role
        edge.setProperty(Schema.EdgeProperty.ROLE_TYPE, role.getId());
        edge = casting.addEdge(rolePlayer, Schema.EdgeLabel.ROLE_PLAYER);// Casting to Roleplayer
        edge.setProperty(Schema.EdgeProperty.ROLE_TYPE, role.getId());
        relation.addEdge(casting, Schema.EdgeLabel.CASTING);// Assertion to Casting
    }

    @Test
    public void testAddCastingLongManyCastingFound() {
        //Artificially Make First Casting
        RelationType relationType = graknGraph.putRelationType("RelationType");
        RoleTypeImpl role = (RoleTypeImpl) graknGraph.putRoleType("role");
        EntityType thing = graknGraph.putEntityType("thing");
        InstanceImpl rolePlayer = (InstanceImpl) graknGraph.addEntity(thing);
        RelationImpl relation = (RelationImpl) graknGraph.addRelation(relationType);

        //First Casting
        makeArtificialCasting(role, rolePlayer, relation);

        //Second Casting Between same entities
        makeArtificialCasting(role, rolePlayer, relation);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(allOf(
                containsString("More than one casting found")
        ));

        graknGraph.putCasting(role, rolePlayer, relation);
    }

    @Test
    public void testExpandingCastingWithRolePlayer() {
        RelationType relationType = graknGraph.putRelationType("RelationType");
        EntityType type = graknGraph.putEntityType("Parent");
        RoleTypeImpl role1 = (RoleTypeImpl) graknGraph.putRoleType("Role1");
        RoleTypeImpl role2 = (RoleTypeImpl) graknGraph.putRoleType("Role2");

        InstanceImpl<?, ?> rolePlayer1 = (InstanceImpl) graknGraph.addEntity(type);
        InstanceImpl<?, ?> rolePlayer2 = (InstanceImpl) graknGraph.addEntity(type);

        RelationImpl assertion = (RelationImpl) graknGraph.addRelation(relationType).
                putRolePlayer(role1, rolePlayer1).putRolePlayer(role2, null);
        CastingImpl casting1 = graknGraph.putCasting(role1, rolePlayer1, assertion);
        CastingImpl casting2 = graknGraph.putCasting(role2, rolePlayer2, assertion);

        assertTrue(assertion.getMappingCasting().contains(casting1));
        assertTrue(assertion.getMappingCasting().contains(casting2));
        assertNotEquals(casting1, casting2);

        Concept rolePlayer2Copy = rolePlayer1.getOutgoingNeighbours(Schema.EdgeLabel.SHORTCUT).iterator().next();
        Concept rolePlayer1Copy = rolePlayer2.getOutgoingNeighbours(Schema.EdgeLabel.SHORTCUT).iterator().next();

        assertEquals(rolePlayer1, rolePlayer1Copy);
        assertEquals(rolePlayer2, rolePlayer2Copy);
    }

    @Test
    public void testGetResourcesByValue(){
        assertEquals(0, graknGraph.getResourcesByValue("Bob").size());
        ResourceType type = graknGraph.putResourceType("Parent", ResourceType.DataType.STRING);
        ResourceType type2 = graknGraph.putResourceType("Parent 2", ResourceType.DataType.STRING);

        Resource c1 = graknGraph.putResource("Bob", type);
        Resource c2 = graknGraph.putResource("Bob", type2);
        Resource c3 = graknGraph.putResource("Bob", type);

        assertEquals(2, graknGraph.getResourcesByValue("Bob").size());
        assertTrue(graknGraph.getResourcesByValue("Bob").contains(c1));
        assertTrue(graknGraph.getResourcesByValue("Bob").contains(c2));
        assertTrue(graknGraph.getResourcesByValue("Bob").contains(c3));
        assertEquals(c1, c3);
        assertNotEquals(c1, c2);
    }

    @Test
    public void testGetConceptInstance(){
        assertNull(graknGraph.getEntity("Bob"));
        EntityType type = graknGraph.putEntityType("Parent");
        Instance c2 = graknGraph.addEntity(type);
        assertEquals(c2, graknGraph.getEntity(c2.getId()));
    }

    @Test
    public void testGetRelation(){
        RelationType relationType = graknGraph.putRelationType("Hello");
        Relation c1 = graknGraph.addRelation(relationType);
        assertEquals(c1, graknGraph.getRelation(c1.getId()));
        assertNull(graknGraph.getResourceType("BOB"));
    }

    @Test
    public void testGetConceptType(){
        assertNull(graknGraph.getEntityType("Bob"));
        Type c2 = graknGraph.putEntityType("Bob");
        assertEquals(c2, graknGraph.getEntityType("Bob"));
    }

    @Test
    public void testGetRelationType(){
        assertNull(graknGraph.getRelationType("Bob"));
        RelationType c2 = graknGraph.putRelationType("Bob");
        assertEquals(c2, graknGraph.getRelationType("Bob"));
    }

    @Test
    public void testGetRoleType(){
        assertNull(graknGraph.getRoleType("Bob"));
        RoleType c2 = graknGraph.putRoleType("Bob");
        assertEquals(c2, graknGraph.getRoleType("Bob"));
    }

    @Test
    public void testGetResourceType(){
        assertNull(graknGraph.getResourceType("Bob"));
        ResourceType c2 = graknGraph.putResourceType("Bob", ResourceType.DataType.STRING);
        assertEquals(c2, graknGraph.getResourceType("Bob"));
    }

    @Test
    public void testGetRuleType(){
        assertNull(graknGraph.getRuleType("Bob"));
        RuleType c2 = graknGraph.putRuleType("Bob");
        assertEquals(c2, graknGraph.getRuleType("Bob"));
    }

    @Test
    public void testGetResource(){
        assertNull(graknGraph.getResource("Bob"));
        ResourceType type = graknGraph.putResourceType("Type", ResourceType.DataType.STRING);
        ResourceType type2 = graknGraph.putResourceType("Type 2", ResourceType.DataType.STRING);
        Resource c2 = graknGraph.putResource("1", type);
        assertEquals(c2, graknGraph.getResourcesByValue("1").iterator().next());
        assertEquals(1, graknGraph.getResourcesByValue("1").size());
        assertEquals(c2, graknGraph.getResource("1", type));
        assertNull(graknGraph.getResource("1", type2));
    }

    @Test
    public void getSuperConceptType(){
        assertEquals(graknGraph.getMetaType().getId(), Schema.MetaSchema.TYPE.getId());
    }

    @Test
    public void getSuperRelationType(){
        assertEquals(graknGraph.getMetaRelationType().getId(), Schema.MetaSchema.RELATION_TYPE.getId());
    }

    @Test
    public void getSuperRoleType(){
        assertEquals(graknGraph.getMetaRoleType().getId(), Schema.MetaSchema.ROLE_TYPE.getId());
    }

    @Test
    public void getSuperResourceType(){
        assertEquals(graknGraph.getMetaResourceType().getId(), Schema.MetaSchema.RESOURCE_TYPE.getId());
    }

    @Test
    public void testGetMetaRuleInference() {
        assertEquals(graknGraph.getMetaRuleInference().getId(), Schema.MetaSchema.INFERENCE_RULE.getId());
    }

    @Test
    public void testGetMetaRuleConstraint() {
        assertEquals(graknGraph.getMetaRuleConstraint().getId(), Schema.MetaSchema.CONSTRAINT_RULE.getId());
    }

    @Test
    public void testMetaOntologyInitialisation(){
        Type type = graknGraph.getMetaType();
        Type relationType = graknGraph.getMetaRelationType();
        Type roleType = graknGraph.getMetaRoleType();
        Type resourceType = graknGraph.getMetaResourceType();

        assertNotNull(type);
        assertNotNull(relationType);
        assertNotNull(roleType);
        assertNotNull(resourceType);

        assertEquals(type, relationType.superType());
        assertEquals(type, roleType.superType());
        assertEquals(type, resourceType.superType());
    }

    @Test
    public void checkTypeCreation(){
        Type testType = graknGraph.putEntityType("Test Concept Type");
        ResourceType testResourceType = graknGraph.putResourceType("Test Resource Type", ResourceType.DataType.STRING);
        RoleType testRoleType = graknGraph.putRoleType("Test Role Type");
        RelationType testRelationType = graknGraph.putRelationType("Test Relation Type");

        assertEquals(Schema.MetaSchema.ENTITY_TYPE.getId(), testType.type().getId());
        assertEquals(Schema.MetaSchema.RESOURCE_TYPE.getId(), testResourceType.type().getId());
        assertEquals(Schema.MetaSchema.ROLE_TYPE.getId(), testRoleType.type().getId());
        assertEquals(Schema.MetaSchema.RELATION_TYPE.getId(), testRelationType.type().getId());

    }

    @Test
    public void testGetType(){
        EntityType a = graknGraph.putEntityType("a");
        assertEquals(a, graknGraph.getType("a"));
    }

    @Test
    public void testInstance(){
        EntityType a = graknGraph.putEntityType("a");
        RelationType b = graknGraph.putRelationType("b");
        ResourceType<String> c = graknGraph.putResourceType("c", ResourceType.DataType.STRING);

        Entity instanceA = graknGraph.addEntity(a);
        Relation instanceB = graknGraph.addRelation(b);
        graknGraph.putResource("1", c);

        assertEquals(instanceA, graknGraph.getInstance(instanceA.getId()));
    }

    @Test
    public void testComplexDelete() throws GraknValidationException {
        RoleType roleType1 = graknGraph.putRoleType("roleType 1");
        RoleType roleType2 = graknGraph.putRoleType("roleType 2");
        RoleType roleType3 = graknGraph.putRoleType("roleType 3");
        RoleType roleType4 = graknGraph.putRoleType("roleType 4");
        EntityType entityType = graknGraph.putEntityType("entity type").
                playsRole(roleType1).playsRole(roleType2).
                playsRole(roleType3).playsRole(roleType4);
        RelationType relationType1 = graknGraph.putRelationType("relation type 1").hasRole(roleType1).hasRole(roleType2);
        RelationType relationType2 = graknGraph.putRelationType("relation type 2").hasRole(roleType3).hasRole(roleType4);

        Entity entity1 = graknGraph.addEntity(entityType);
        Entity entity2 = graknGraph.addEntity(entityType);
        Entity entity3 = graknGraph.addEntity(entityType);
        Entity entity4 = graknGraph.addEntity(entityType);
        Entity entity5 = graknGraph.addEntity(entityType);

        graknGraph.addRelation(relationType1).putRolePlayer(roleType1, entity1).putRolePlayer(roleType2, entity2);
        graknGraph.addRelation(relationType1).putRolePlayer(roleType1, entity1).putRolePlayer(roleType2, entity3);
        graknGraph.addRelation(relationType2).putRolePlayer(roleType3, entity1).putRolePlayer(roleType4, entity4);
        graknGraph.addRelation(relationType2).putRolePlayer(roleType3, entity1).putRolePlayer(roleType4, entity5);

        graknGraph.commit();

        entity1.delete();

        graknGraph.commit();

        assertNull(graknGraph.getConcept("1"));
    }


    @Test
    public void testGetInstancesFromMeta(){
        Type metaType = graknGraph.getMetaType();
        Type metaEntityType = graknGraph.getMetaEntityType();
        Type metaRelationType = graknGraph.getMetaRelationType();
        Type metaResourceType = graknGraph.getMetaResourceType();
        Type metaRoleType = graknGraph.getMetaRoleType();
        Type metaRuleType = graknGraph.getMetaRuleType();

        EntityType sampleEntityType = graknGraph.putEntityType("Sample Entity Type");
        RelationType sampleRelationType = graknGraph.putRelationType("Sample Relation Type");
        RoleType sampleRoleType = graknGraph.putRoleType("Sample Role Type");

        Collection<? extends Concept> instances = metaType.instances();

        assertFalse(instances.contains(metaEntityType));
        assertFalse(instances.contains(metaRelationType));
        assertFalse(instances.contains(metaResourceType));
        assertFalse(instances.contains(metaRoleType));
        assertFalse(instances.contains(metaRuleType));

        assertTrue(instances.contains(sampleEntityType));
        assertTrue(instances.contains(sampleRelationType));
        assertTrue(instances.contains(sampleRoleType));
    }
}