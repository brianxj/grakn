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

import io.grakn.concept.Concept;
import io.grakn.concept.Entity;
import io.grakn.concept.EntityType;
import io.grakn.concept.Instance;
import io.grakn.concept.Relation;
import io.grakn.concept.RelationType;
import io.grakn.concept.Resource;
import io.grakn.concept.ResourceType;
import io.grakn.concept.RoleType;
import io.grakn.concept.Rule;
import io.grakn.concept.RuleType;
import io.grakn.concept.Type;
import io.grakn.exception.ConceptException;
import io.grakn.exception.ConceptNotUniqueException;
import io.grakn.exception.InvalidConceptTypeException;
import io.grakn.exception.InvalidConceptValueException;
import io.grakn.exception.MoreThanOneEdgeException;
import io.grakn.util.ErrorMessage;
import io.grakn.util.Schema;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A concept which can represent anything in the graph
 * @param <T> The leaf interface of the object concept. For example an EntityType, Entity, RelationType etc . . .
 * @param <V> The type of the concept.
 */
abstract class ConceptImpl<T extends Concept, V extends Type> implements Concept {
    @SuppressWarnings("unchecked")
    T getThis(){
        return (T) this;
    }

    private final AbstractGraknGraph graknGraph;
    private Vertex vertex;

    ConceptImpl(Vertex v, V type, AbstractGraknGraph graknGraph){
        this.vertex = v;
        this.graknGraph = graknGraph;
        type(type);
        graknGraph.getConceptLog().putConcept(this);
    }

    /**
     * Generates and saves a readable entity id
     * @param type the type of this concept
     */
    protected void generateInstanceId(V type){
        if(getId() == null){
            String id = getBaseType() + "-" + type.getId() + "-" + UUID.randomUUID().toString();
            setImmutableProperty(Schema.ConceptProperty.ITEM_IDENTIFIER, id);
        }
    }

    /**
     *
     * @param key The key of the property to mutate
     * @param value The value to commit into the property
     * @return The concept itself casted to the correct interface itself
     */
    private T setProperty(String key, Object value){
        if(value == null)
            vertex.property(key).remove();
        else
            vertex.property(key, value);
        return getThis();
    }

    /**
     * Deletes the concept.
     * @throws ConceptException Throws an exception if the node has any edges attached to it.
     */
    @Override
    public void delete() throws ConceptException {
        ConceptImpl properType = getGraknGraph().getElementFactory().buildUnknownConcept(vertex);
        properType.innerDelete(); //This will execute the proper deletion method.
    }

    /**
     * Helper method to call the appropriate deletion based on the type of the concept.
     */
    //TODO: Check if this is actually the right way of doing things. This is quite odd.
    void innerDelete(){
        deleteNode();
    }

    /**
     *
     * @param key The key of the unique property to mutate
     * @param id The new value of the unique property
     * @return The concept itself casted to the correct interface itself
     */
    T setUniqueProperty(Schema.ConceptProperty key, String id){
        if(graknGraph.isBatchLoadingEnabled() || updateAllowed(key, id))
            return setProperty(key, id);
        else
            throw new ConceptNotUniqueException(this, key, id);
    }

    /**
     *
     * @param key The key of the unique property to mutate
     * @param value The value to check
     * @return True if the concept can be updated. I.e. the value is unique for the property.
     */
    private boolean updateAllowed(Schema.ConceptProperty key, String value) {
        ConceptImpl fetchedConcept = graknGraph.getConcept(key, value);
        return fetchedConcept == null || this.equals(fetchedConcept);
    }

    /**
     * Deletes the node and adds it neighbours for validation
     */
    void deleteNode(){
        // tracking
        vertex.edges(Direction.BOTH).
                forEachRemaining(
                        e -> {
                            graknGraph.getConceptLog().putConcept(getGraknGraph().getElementFactory().buildUnknownConcept(e.inVertex()));
                            graknGraph.getConceptLog().putConcept(getGraknGraph().getElementFactory().buildUnknownConcept(e.outVertex()));}
                );
        graknGraph.getConceptLog().removeConcept(this);
        // delete node
        vertex.remove();
        vertex = null;
    }

    /**
     *
     * @return The type of the concept casted to the correct interface
     */
    @SuppressWarnings("unchecked")
    @Override
    public V type() {
        HashSet<Concept> visitedConcepts = new HashSet<>();
        ConceptImpl currentConcept = this;
        visitedConcepts.add(currentConcept);
        Type type = null;
        boolean notFound = true;

        while(notFound && currentConcept != null){
            ConceptImpl concept = currentConcept.getParentIsa();
            if(concept != null){
                notFound = false;
                type = concept.asType();
            } else {
                currentConcept = currentConcept.getParentSub();
                if(visitedConcepts.contains(currentConcept)){
                    throw new ConceptException(ErrorMessage.LOOP_DETECTED.getMessage(toString(), Schema.EdgeLabel.SUB.getLabel() + " " + Schema.EdgeLabel.ISA.getLabel()));
                }
                visitedConcepts.add(currentConcept);

            }
        }

        return (V) type;
    }

    /**
     * Helper method to cast a concept to it's correct type
     * @param type The type to cast to
     * @param <E> The type of the interface we are casting to.
     * @return The concept itself casted to the defined interface
     * @throws InvalidConceptTypeException when casting a concept incorrectly
     */
    private <E> E castConcept(Class<E> type){
        try {
            return type.cast(this);
        } catch(ClassCastException e){
            throw new InvalidConceptTypeException(ErrorMessage.INVALID_OBJECT_TYPE.getMessage(this, type));
        }
    }

    /**
     *
     * @return A Type if the concept is a Type
     */
    @Override
    public Type asType() {
        return castConcept(Type.class);
    }

    /**
     *
     * @return An Instance if the concept is an Instance
     */
    @Override
    public Instance asInstance() {
        return castConcept(Instance.class);
    }

    /**
     *
     * @return A Entity Type if the concept is a Entity Type
     */
    @Override
    public EntityType asEntityType() {
        return castConcept(EntityType.class);
    }

    /**
     *
     * @return A Role Type if the concept is a Role Type
     */
    @Override
    public RoleType asRoleType() {
        return castConcept(RoleType.class);
    }

    /**
     *
     * @return A Relation Type if the concept is a Relation Type
     */
    @Override
    public RelationType asRelationType() {
        return castConcept(RelationType.class);
    }

    /**
     *
     * @return A Resource Type if the concept is a Resource Type
     */
    @SuppressWarnings("unchecked")
    @Override
    public <D> ResourceType<D> asResourceType() {
        return castConcept(ResourceType.class);
    }

    /**
     *
     * @return A Rule Type if the concept is a Rule Type
     */
    @Override
    public RuleType asRuleType() {
        return castConcept(RuleType.class);
    }

    /**
     *
     * @return An Entity if the concept is an Instance
     */
    @Override
    public Entity asEntity() {
        return castConcept(Entity.class);
    }

    /**
     *
     * @return A Relation if the concept is a Relation
     */
    @Override
    public Relation asRelation() {
        return castConcept(Relation.class);
    }

    /**
     *
     * @return A Resource if the concept is a Resource
     */
    @SuppressWarnings("unchecked")
    @Override
    public <D> Resource<D> asResource() {
        return castConcept(Resource.class);
    }

    /**
     *
     * @return A Rule if the concept is a Rule
     */@Override
    public Rule asRule() {
        return castConcept(Rule.class);
    }

    /**
     *
     * @return A casting if the concept is a casting
     */
    public CastingImpl asCasting(){
        return (CastingImpl) this;
    }

    /**
     *
     * @return true if the concept is a Type
     */
    @Override
    public boolean isType() {
        return this instanceof Type;
    }

    /**
     *
     * @return true if the concept is an Instance
     */
    @Override
    public boolean isInstance() {
        return this instanceof Instance;
    }

    /**
     *
     * @return true if the concept is a Entity Type
     */
    @Override
    public boolean isEntityType() {
        return this instanceof EntityType;
    }

    /**
     *
     * @return true if the concept is a Role Type
     */
    @Override
    public boolean isRoleType() {
        return this instanceof RoleType;
    }

    /**
     *
     * @return true if the concept is a Relation Type
     */
    @Override
    public boolean isRelationType() {
        return this instanceof RelationType;
    }

    /**
     *
     * @return true if the concept is a Resource Type
     */
    @Override
    public boolean isResourceType() {
        return this instanceof ResourceType;
    }

    /**
     *
     * @return true if the concept is a Rule Type
     */
    @Override
    public boolean isRuleType() {
        return this instanceof RuleType;
    }

    /**
     *
     * @return true if the concept is a Entity
     */
    @Override
    public boolean isEntity() {
        return this instanceof Entity;
    }

    /**
     *
     * @return true if the concept is a Relation
     */
    @Override
    public boolean isRelation() {
        return this instanceof Relation;
    }

    /**
     *
     * @return true if the concept is a Resource
     */
    @Override
    public boolean isResource() {
        return this instanceof Resource;
    }

    /**
     *
     * @return true if the concept is a Rule
     */
    @Override
    public boolean isRule() {
        return this instanceof Rule;
    }

    /**
     *
     * @return true if the concept is a casting
     */
    public boolean isCasting(){
        return this instanceof CastingImpl;
    }

    /**
     *
     * @param type The type of this concept
     * @return The concept itself casted to the correct interface
     */
    private T type(V type) {
        if(type != null){
            TypeImpl currentIsa = getParentIsa();
            if(currentIsa == null){
                setType(String.valueOf(type.getId()));
                putEdge(type, Schema.EdgeLabel.ISA);
            } else if(!currentIsa.equals(type)){
                throw new InvalidConceptTypeException(ErrorMessage.IMMUTABLE_TYPE.getMessage(this, type, currentIsa));
            }

        }

        return getThis();
    }

    /**
     *
     * @return The result of following one outgoing isa edge to a Type.
     */
    public TypeImpl getParentIsa(){
        Concept isaParent = getOutgoingNeighbour(Schema.EdgeLabel.ISA);
        if(isaParent != null){
            return (TypeImpl) isaParent;
        } else {
            return null;
        }
    }

    /**
     *
     * @return The result of following one outgoing sub edge to a Type.
     */
    public TypeImpl getParentSub(){
        Concept subParent = getOutgoingNeighbour(Schema.EdgeLabel.SUB);
        if(subParent != null){
            return (TypeImpl) subParent;
        } else {
            return null;
        }
    }

    /**
     *
     * @param edgeLabel The edge label to traverse
     * @return The neighbouring concept found by traversing one outgoing edge of a specific type
     */
    protected Concept getOutgoingNeighbour(Schema.EdgeLabel edgeLabel){
        Set<ConceptImpl> concepts = getOutgoingNeighbours(edgeLabel);
        if(concepts.size() == 1){
            return concepts.iterator().next();
        } else if(concepts.isEmpty()){
            return null;
        } else {
            throw new MoreThanOneEdgeException(this, edgeLabel);
        }
    }

    /**
     *
     * @param edgeType The edge label to traverse
     * @return The neighbouring concepts found by traversing outgoing edges of a specific type
     */
    protected Set<ConceptImpl> getOutgoingNeighbours(Schema.EdgeLabel edgeType){
        Set<ConceptImpl> outgoingNeighbours = new HashSet<>();

        getEdgesOfType(Direction.OUT, edgeType).forEach(edge -> {
            ConceptImpl found = edge.getTarget();
            if(found != null){
                outgoingNeighbours.add(found);
            }
        });
        return outgoingNeighbours;
    }

    /**
     *
     * @param edgeLabel The edge label to traverse
     * @return The neighbouring concept found by traversing one incoming edge of a specific type
     */
    Concept getIncomingNeighbour(Schema.EdgeLabel edgeLabel){
        Set<ConceptImpl> concepts = getIncomingNeighbours(edgeLabel);
        if(concepts.size() == 1){
            return concepts.iterator().next();
        } else if(concepts.isEmpty()){
            return null;
        } else {
            throw new MoreThanOneEdgeException(this, edgeLabel);
        }
    }
    /**
     *
     * @param edgeType The edge label to traverse
     * @return The neighbouring concepts found by traversing incoming edges of a specific type
     */
    protected Set<ConceptImpl> getIncomingNeighbours(Schema.EdgeLabel edgeType){
        Set<ConceptImpl> incomingNeighbours = new HashSet<>();
        getEdgesOfType(Direction.IN, edgeType).forEach(edge -> {
            ConceptImpl found = edge.getSource();
            if(found != null){
                incomingNeighbours.add(found);
            }
        });
        return incomingNeighbours;
    }

    /**
     *
     * @param key The key of the non-unique property to mutate
     * @param value The value to commit into the property
     * @return The concept itself casted to the correct interface
     */
    T setProperty(Schema.ConceptProperty key, Object value){
        return setProperty(key.name(), value);
    }

    /**
     *
     * @param key The key of the non-unique property to retrieve
     * @return The value stored in the property
     */
    @SuppressWarnings("unchecked")
    public <X extends Object> X getProperty(Schema.ConceptProperty key){
        VertexProperty property = vertex.property(key.name());
        if(property != null && property.isPresent())
            return (X) property.value();
        return null;
    }
    public Boolean getPropertyBoolean(Schema.ConceptProperty key){
        Boolean value = getProperty(key);
        if(value == null)
            return false;
        return value;
    }

    /**
     *
     * @return The tinkerpop vertex
     */
    Vertex getVertex() {
        return vertex;
    }

    //------------ Setters ------------
    /**
     *
     * @param type The type of this concept
     * @return The concept itself casted to the correct interface
     */
    public T setType(String type){
        return setProperty(Schema.ConceptProperty.TYPE, type);
    }

    //------------ Getters ------------
    /**
     *
     * @return The unique base identifier of this concept.
     */
    public Object getBaseIdentifier() {
        return vertex.id();
    }

    /**
     *
     * @return The base ttpe of this concept which helps us identify the concept
     */
    public String getBaseType(){
        return vertex.label();
    }

    /**
     *
     * @return A string representing the concept's unique id.
     */
    @Override
    public String getId(){
        return getProperty(Schema.ConceptProperty.ITEM_IDENTIFIER);
    }

    /**
     *
     * @return The id of the type of this concept. This is a shortcut used to prevent traversals.
     */
    public String getType(){
        return getProperty(Schema.ConceptProperty.TYPE);
    }

    /**
     *
     * @param direction The direction of the edges to retrieve
     * @param type The type of the edges to retrieve
     * @return A collection of edges from this concept in a particular direction of a specific type
     */
    protected Set<EdgeImpl> getEdgesOfType(Direction direction, Schema.EdgeLabel type){
        Set<EdgeImpl> edges = new HashSet<>();
        vertex.edges(direction, type.getLabel()).
                forEachRemaining(e -> edges.add(new EdgeImpl(e, getGraknGraph())));
        return edges;
    }

    /**
     *
     * @param type The type of the edge to retrieve
     * @return An edge from this concept in a particular direction of a specific type
     * @throws MoreThanOneEdgeException when more than one edge of s specific type
     */
    public EdgeImpl getEdgeOutgoingOfType(Schema.EdgeLabel type) {
        Set<EdgeImpl> edges = getEdgesOfType(Direction.OUT, type);
        if(edges.size() == 1)
            return edges.iterator().next();
        else if(edges.size() > 1)
            throw new MoreThanOneEdgeException(this, type);
        else
            return null;
    }

    /**
     *
     * @return The grakn graph this concept is bound to.
     */
    protected AbstractGraknGraph getGraknGraph() {return graknGraph;}

    //--------- Create Links -------//
    /**
     *
     * @param to the target concept
     * @param type the type of the edge to create
     */
    void putEdge(Concept to, Schema.EdgeLabel type){
        ConceptImpl toConcept = (ConceptImpl) to;
        GraphTraversal<Vertex, Edge> traversal = graknGraph.getTinkerPopGraph().traversal().V(getBaseIdentifier()).outE(type.getLabel()).as("edge").otherV().hasId(toConcept.getBaseIdentifier()).select("edge");
        if(!traversal.hasNext())
            addEdge(toConcept, type);
    }

    /**
     *
     * @param toConcept the target concept
     * @param type the type of the edge to create
     * @return The edge created
     */
    public EdgeImpl addEdge(ConceptImpl toConcept, Schema.EdgeLabel type) {
        graknGraph.getConceptLog().putConcept(this);
        graknGraph.getConceptLog().putConcept(toConcept);

        return getGraknGraph().getElementFactory().buildEdge(toConcept.addEdgeFrom(this.vertex, type.getLabel()), graknGraph);
    }

    /**
     *
     * @param direction The direction of the edges to retrieve
     * @param type The type of the edges to retrieve
     */
    void deleteEdges(Direction direction, Schema.EdgeLabel type){
        // track changes
        vertex.edges(direction, type.getLabel()).
                forEachRemaining(
                        e -> {
                            graknGraph.getConceptLog().putConcept(
                                    getGraknGraph().getElementFactory().buildUnknownConcept(e.inVertex()));
                            graknGraph.getConceptLog().putConcept(
                                    getGraknGraph().getElementFactory().buildUnknownConcept(e.outVertex()));
                        }
                );

        // deletion
        vertex.edges(direction, type.getLabel()).forEachRemaining(Element::remove);
    }

    /**
     * Deletes an edge of a specific type going to a specific concept
     * @param type The type of the edge
     * @param toConcept The target concept
     */
    void deleteEdgeTo(Schema.EdgeLabel type, Concept toConcept){
        GraphTraversal<Vertex, Edge> traversal = graknGraph.getTinkerPopGraph().traversal().V(getBaseIdentifier()).
                outE(type.getLabel()).as("edge").otherV().hasId(((ConceptImpl) toConcept).getBaseIdentifier()).select("edge");
        if(traversal.hasNext())
            traversal.next().remove();
    }

    private org.apache.tinkerpop.gremlin.structure.Edge addEdgeFrom(Vertex fromVertex, String type) {
        return fromVertex.addEdge(type, vertex);
    }


    //------------ Base Equality ------------
    /**
     *
     * @return The hash code of the underlying vertex
     */
    public int hashCode() {
        return vertex.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ConceptImpl && ((ConceptImpl) object).getVertex().equals(vertex);
    }

    @Override
    public String toString(){
        String message = "[" +  this.hashCode() + "] "+
                "- Base Type [" + getBaseType() + "] ";
        if(getId() != null)
            message = message + "- Item Identifier [" + getId() + "] ";

        return message;
    }

    //---------- Null Vertex Handler ---------
    /**
     * Checks if the underlaying vertex has not been removed and if it is not a ghost
     * @return true if the underlying vertex has not been removed.
     */
    boolean isAlive() {
        if(vertex == null)
            return false;

        try {
            return vertex.property(Schema.BaseType.TYPE.name()).isPresent();
        } catch (IllegalStateException e){
            return false;
        }
    }

    void setImmutableProperty(Schema.ConceptProperty conceptProperty, Object value){
        if(value == null){
            throw new InvalidConceptValueException(ErrorMessage.NULL_VALUE.getMessage(conceptProperty.name()));
        }

        if(getProperty(conceptProperty) != null){
            Object foundValue = getProperty(conceptProperty);
            if(!foundValue.equals(value)){
                throw new InvalidConceptValueException(ErrorMessage.IMMUTABLE_VALUE.getMessage(foundValue, this, value, conceptProperty.name()));
            }
        } else {
            setProperty(conceptProperty, value);
        }
    }
    
    @Override
    public int compareTo(Concept o) {
        return this.getId().compareTo(o.getId());
    }
}
