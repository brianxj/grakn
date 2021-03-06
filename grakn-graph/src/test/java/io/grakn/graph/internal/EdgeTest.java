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
import io.grakn.concept.Entity;
import io.grakn.concept.EntityType;
import io.grakn.util.Schema;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class EdgeTest {

    private AbstractGraknGraph graknGraph;
    private EntityType entityType;
    private Entity entity;
    private EdgeImpl edge;

    @Before
    public void setUp(){
        graknGraph = (AbstractGraknGraph) Grakn.factory(Grakn.IN_MEMORY, UUID.randomUUID().toString().replaceAll("-", "a")).getGraph();
        entityType = graknGraph.putEntityType("My Entity Type");
        entity = graknGraph.addEntity(entityType);
        Edge tinkerEdge = (Edge) graknGraph.getTinkerTraversal().has(Schema.ConceptProperty.ITEM_IDENTIFIER.name(), entity.getId()).outE().next();
        edge = new EdgeImpl(tinkerEdge, graknGraph);
    }

    @After
    public void destroyGraphAccessManager() throws Exception {
        graknGraph.close();
    }

    @Test
    public void testEquals(){
        Entity entity2 = graknGraph.addEntity(entityType);
        Edge tinkerEdge = (Edge) graknGraph.getTinkerTraversal().has(Schema.ConceptProperty.ITEM_IDENTIFIER.name(), entity2.getId()).outE().next();
        EdgeImpl edge2 = new EdgeImpl(tinkerEdge, graknGraph);

        assertEquals(edge, edge);
        assertNotEquals(edge, edge2);
    }

    @Test
    public void testGetSource() throws Exception {
        assertEquals(entity, edge.getSource());
    }

    @Test
    public void testGetTarget() throws Exception {
        assertEquals(entityType, edge.getTarget());
    }

    @Test
    public void testGetType() throws Exception {
        assertEquals(Schema.EdgeLabel.ISA, edge.getType());
    }

    @Test
    public void testProperty() throws Exception {
        edge.setProperty(Schema.EdgeProperty.ROLE_TYPE, "role");
        assertEquals("role", edge.getProperty(Schema.EdgeProperty.ROLE_TYPE));
        assertNull(edge.getProperty(Schema.EdgeProperty.FROM_TYPE));
    }

}