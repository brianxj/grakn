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

import io.grakn.concept.Entity;
import io.grakn.concept.EntityType;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * An instance of Entity Type which represents some data in the graph.
 */
class EntityImpl extends InstanceImpl<Entity, EntityType> implements Entity {
    EntityImpl(Vertex v, EntityType type, AbstractGraknGraph graknGraph) {
        super(v, type, graknGraph);
    }
}