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
import io.grakn.exception.ConceptException;
import io.grakn.exception.MoreThanOneEdgeException;
import io.grakn.util.ErrorMessage;
import io.grakn.util.Schema;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConceptTest {

    private AbstractGraknGraph graknGraph;
    private ConceptImpl concept;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp(){
        graknGraph = (AbstractGraknGraph) Grakn.factory(Grakn.IN_MEMORY, UUID.randomUUID().toString().replaceAll("-", "a")).getGraph();
        graknGraph.initialiseMetaConcepts();
        concept = (ConceptImpl) graknGraph.putEntityType("main_concept");
    }
    @After
    public void destroyGraphAccessManager() throws Exception {
        graknGraph.close();
    }

    @Test(expected=MoreThanOneEdgeException.class)
    public void testGetEdgeOutgoingOfType(){
        ConceptImpl<?, ?> concept = (ConceptImpl<?, ?>) graknGraph.putEntityType("Thing");
        assertNull(concept.getEdgeOutgoingOfType(Schema.EdgeLabel.SUB));

        TypeImpl type1 = (TypeImpl) graknGraph.putEntityType("Type 1");
        TypeImpl type2 = (TypeImpl) graknGraph.putEntityType("Type 2");
        TypeImpl type3 = (TypeImpl) graknGraph.putEntityType("Type 3");

        assertNotNull(type1.getEdgeOutgoingOfType(Schema.EdgeLabel.ISA));

        Vertex vertexType1 = graknGraph.getTinkerPopGraph().traversal().V(type1.getBaseIdentifier()).next();
        Vertex vertexType3 = graknGraph.getTinkerPopGraph().traversal().V(type3.getBaseIdentifier()).next();
        vertexType1.addEdge(Schema.EdgeLabel.ISA.getLabel(), vertexType3);
        type1.getEdgeOutgoingOfType(Schema.EdgeLabel.ISA);
    }

    @Test
    public void testItemIdentifier() {
        assertEquals("main_concept", concept.getId());
    }

    @Test
    public void testGetVertex(){
        assertNotNull(concept.getBaseIdentifier());
    }

    @Test
    public void testSetType() {
        concept.setType("test_type");
        Vertex conceptVertex = graknGraph.getTinkerPopGraph().traversal().V(concept.getBaseIdentifier()).next();
        assertEquals("test_type", conceptVertex.property(Schema.ConceptProperty.TYPE.name()).value());
    }

    @Test
    public void testGetType() {
        concept.setType("test_type");
        Vertex conceptVertex = graknGraph.getTinkerPopGraph().traversal().V(concept.getBaseIdentifier()).next();
        assertEquals(concept.getType(), conceptVertex.property(Schema.ConceptProperty.TYPE.name()).value());
    }

    @Test
    public void testEquality() {
        ConceptImpl c1= (ConceptImpl) graknGraph.putEntityType("Value_1");
        Concept c1_copy = graknGraph.getConcept("Value_1");
        Concept c1_copy_copy = graknGraph.putEntityType("Value_1");

        Concept c2 = graknGraph.putEntityType("Value_2");

        assertEquals(c1, c1_copy);
        assertNotEquals(c1, c2);
        assertNotEquals(c1.getBaseIdentifier(), concept.getBaseIdentifier());

        HashSet<Concept> concepts = new HashSet<>();

        concepts.add(c1);
        concepts.add(c1_copy);
        concepts.add(c1_copy_copy);
        assertEquals(1, concepts.size());

        concepts.add(c2);
        assertEquals(2, concepts.size());
        Vertex conceptVertex = graknGraph.getTinkerPopGraph().traversal().V(concept.getBaseIdentifier()).next();
        assertNotEquals(concept, conceptVertex);
    }

    @Test
    public void testGetParentIsa(){
        EntityType entityType = graknGraph.putEntityType("Entiy Type");
        Entity entity = graknGraph.addEntity(entityType);
        assertEquals(entityType, entity.type());
    }

    @Test
    public void testGetParentSub(){
        TypeImpl conceptType = (TypeImpl) graknGraph.putEntityType("conceptType");
        assertNull(conceptType.getParentSub());
        TypeImpl conceptParent = (TypeImpl) graknGraph.putEntityType("CP");
        conceptType.superType(conceptParent);
        Concept foundConcept = conceptType.getParentSub();
        assertEquals(conceptParent, foundConcept);
        assertNull(conceptParent.getParentSub());
        assertNull(conceptType.getParentIsa());
    }

    @Test(expected = RuntimeException.class)
    public void getBaseTypeTestFail() {
        RelationType concept = graknGraph.putRelationType("relType");
        graknGraph.putRoleType(concept.getId());
    }

    @Test
    public void testToString() {
        EntityType concept = graknGraph.putEntityType("a");
        Instance concept2 = graknGraph.addEntity(concept);

        assertFalse(concept2.toString().contains("ConceptType"));
        assertFalse(concept2.toString().contains("Subject Identifier"));
        assertFalse(concept2.toString().contains("Subject Locator"));
    }

    @Test
    public void testDelete() throws ConceptException{
        assertEquals(9, graknGraph.getTinkerPopGraph().traversal().V().toList().size());
        Concept c1 = graknGraph.putEntityType("1");
        assertEquals(10, graknGraph.getTinkerPopGraph().traversal().V().toList().size());
        c1.delete();
        assertEquals(9, graknGraph.getTinkerPopGraph().traversal().V().toList().size());

        Concept c2 = graknGraph.putEntityType("blab");
        assertEquals(10, graknGraph.getTinkerPopGraph().traversal().V().toList().size());
        c2.delete();
        assertEquals(9, graknGraph.getTinkerPopGraph().traversal().V().toList().size());
    }

    @Test(expected = ConceptException.class)
    public void testDeleteFail() throws ConceptException{
        EntityType c1 = graknGraph.putEntityType("C1");
        EntityType c2 = graknGraph.putEntityType("C2");
        c1.superType(c2);
        c2.delete();
    }

    @Test
    public void testGetConceptType(){
        EntityType c1 = graknGraph.putEntityType("c1");
        Entity c2 = graknGraph.addEntity(c1);
        assertEquals(c1, c2.type());
    }

    @Test
    public void testGetConceptTypeFailCycleFoundSimple(){
        TypeImpl c1 = (TypeImpl) graknGraph.putEntityType("c1");

        expectedException.expect(ConceptException.class);
        expectedException.expectMessage(allOf(
                containsString(ErrorMessage.LOOP_DETECTED.getMessage(c1.toString(), Schema.EdgeLabel.SUB.getLabel() + " " + Schema.EdgeLabel.ISA.getLabel()))
        ));

        TypeImpl c2 = (TypeImpl) graknGraph.putEntityType("c2");
        TypeImpl c3 = (TypeImpl) graknGraph.putEntityType("c3");
        Vertex c1_Vertex = graknGraph.getTinkerPopGraph().traversal().V(c1.getBaseIdentifier()).next();
        Vertex c2_Vertex = graknGraph.getTinkerPopGraph().traversal().V(c2.getBaseIdentifier()).next();
        Vertex c3_Vertex = graknGraph.getTinkerPopGraph().traversal().V(c3.getBaseIdentifier()).next();

        c1_Vertex.edges(Direction.BOTH).next().remove();
        c2_Vertex.edges(Direction.BOTH).next().remove();
        c3_Vertex.edges(Direction.BOTH).next().remove();

        c1_Vertex.addEdge(Schema.EdgeLabel.SUB.getLabel(), c2_Vertex);
        c2_Vertex.addEdge(Schema.EdgeLabel.SUB.getLabel(), c3_Vertex);
        c3_Vertex.addEdge(Schema.EdgeLabel.SUB.getLabel(), c1_Vertex);
        c1.type();
    }

    @Test
    public void testGetEdgesIncomingOfType(){
        EntityType entityType = graknGraph.putEntityType("entity type");
        InstanceImpl conceptInstance1 = (InstanceImpl) graknGraph.addEntity(entityType);
        InstanceImpl conceptInstance2 = (InstanceImpl) graknGraph.addEntity(entityType);
        InstanceImpl conceptInstance3 = (InstanceImpl) graknGraph.addEntity(entityType);
        InstanceImpl conceptInstance4 = (InstanceImpl) graknGraph.addEntity(entityType);
        InstanceImpl conceptInstance5 = (InstanceImpl) graknGraph.addEntity(entityType);
        InstanceImpl conceptInstance6 = (InstanceImpl) graknGraph.addEntity(entityType);
        Vertex conceptInstance1_Vertex = graknGraph.getTinkerPopGraph().traversal().V(conceptInstance1.getBaseIdentifier()).next();
        Vertex conceptInstance2_Vertex = graknGraph.getTinkerPopGraph().traversal().V(conceptInstance2.getBaseIdentifier()).next();
        Vertex conceptInstance3_Vertex = graknGraph.getTinkerPopGraph().traversal().V(conceptInstance3.getBaseIdentifier()).next();
        Vertex conceptInstance4_Vertex = graknGraph.getTinkerPopGraph().traversal().V(conceptInstance4.getBaseIdentifier()).next();
        Vertex conceptInstance5_Vertex = graknGraph.getTinkerPopGraph().traversal().V(conceptInstance5.getBaseIdentifier()).next();
        Vertex conceptInstance6_Vertex = graknGraph.getTinkerPopGraph().traversal().V(conceptInstance6.getBaseIdentifier()).next();

        conceptInstance2_Vertex.addEdge(Schema.EdgeLabel.SHORTCUT.getLabel(), conceptInstance1_Vertex);
        conceptInstance3_Vertex.addEdge(Schema.EdgeLabel.SHORTCUT.getLabel(), conceptInstance1_Vertex);
        conceptInstance4_Vertex.addEdge(Schema.EdgeLabel.SHORTCUT.getLabel(), conceptInstance1_Vertex);
        conceptInstance5_Vertex.addEdge(Schema.EdgeLabel.SHORTCUT.getLabel(), conceptInstance1_Vertex);
        conceptInstance6_Vertex.addEdge(Schema.EdgeLabel.SHORTCUT.getLabel(), conceptInstance1_Vertex);

        Set<EdgeImpl> edges = conceptInstance1.getEdgesOfType(Direction.IN, Schema.EdgeLabel.SHORTCUT);

        assertEquals(5, edges.size());
    }

    @Test
    public void testAsConceptType() {
        Concept concept = graknGraph.putEntityType("Test");
        assertTrue(concept.isEntityType());
        Type type = concept.asEntityType();
        assertEquals(type, concept);
    }

    @Test
    public void  testAsRoleType() {
        Concept concept = graknGraph.putRoleType("Test");
        assertTrue(concept.isRoleType());
        RoleType concept2 = concept.asRoleType();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsRelationType() {
        Concept concept = graknGraph.putRelationType("Test");
        assertTrue(concept.isRelationType());
        RelationType concept2 = concept.asRelationType();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsResourceType() {
        Concept concept = graknGraph.putResourceType("Test", ResourceType.DataType.STRING);
        assertTrue(concept.isResourceType());
        ResourceType concept2 = concept.asResourceType();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsRuleType() {
        Concept concept = graknGraph.putRuleType("Test");
        assertTrue(concept.isRuleType());
        RuleType concept2 = concept.asRuleType();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsEntity() {
        EntityType entityType = graknGraph.putEntityType("entity type");
        Concept concept = graknGraph.addEntity(entityType);
        assertTrue(concept.isEntity());
        Instance concept2 = concept.asEntity();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsRelation() {
        RelationType type = graknGraph.putRelationType("a type");
        Concept concept = graknGraph.addRelation(type);
        assertTrue(concept.isRelation());
        Relation concept2 = concept.asRelation();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsResource() {
        ResourceType type = graknGraph.putResourceType("a type", ResourceType.DataType.STRING);
        Concept concept = graknGraph.putResource("Test", type);
        assertTrue(concept.isResource());
        Resource concept2 = concept.asResource();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsRule() {
        RuleType type = graknGraph.putRuleType("a type");
        Concept concept = graknGraph.addRule("lhs", "rhs", type);
        assertTrue(concept.isRule());
        io.grakn.concept.Rule concept2 = concept.asRule();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsType() {
        Concept concept = graknGraph.getMetaType();
        assertTrue(concept.isType());
        Type concept2 = concept.asType();
        assertEquals(concept2, concept);
    }

    @Test
    public void  testAsInstance() {
        RuleType type = graknGraph.putRuleType("a type");
        Concept concept = graknGraph.addRule("lhs", "rhs", type);
        assertTrue(concept.isInstance());
        Instance concept2 = concept.asInstance();
        assertEquals(concept2, concept);
    }

    @Test
    public void incorrectConversion(){
        EntityType thingType = graknGraph.putEntityType("thing type");
        Entity thing = graknGraph.addEntity(thingType);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(allOf(
                containsString(ErrorMessage.INVALID_OBJECT_TYPE.getMessage(thing, Type.class))
        ));

        thing.asType();
    }

    @Test
    public void reservedTest(){
        expectedException.expect(ConceptException.class);
        expectedException.expectMessage(allOf(
                containsString(ErrorMessage.ID_RESERVED.getMessage("type"))
        ));
        graknGraph.putEntityType("type");
    }
}