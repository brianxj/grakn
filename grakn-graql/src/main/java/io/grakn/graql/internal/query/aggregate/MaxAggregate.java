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

package io.grakn.graql.internal.query.aggregate;

import io.grakn.concept.Concept;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;

/**
 * Aggregate that finds maximum of a match query.
 */
class MaxAggregate extends AbstractAggregate<Map<String, Concept>, Optional<?>> {

    private final String varName;

    MaxAggregate(String varName) {
        this.varName = varName;
    }

    @Override
    public Optional<?> apply(Stream<? extends Map<String, Concept>> stream) {
        return stream.map(result -> (Comparable) result.get(varName).asResource().getValue()).max(naturalOrder());
    }

    @Override
    public String toString() {
        return "max $" + varName;
    }
}
