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

package io.grakn.graql.internal.pattern.property;

import io.grakn.concept.Concept;
import io.grakn.graql.admin.UniqueVarProperty;
import io.grakn.graql.internal.gremlin.fragment.Fragment;
import io.grakn.graql.internal.gremlin.fragment.Fragments;
import io.grakn.graql.internal.query.InsertQueryExecutor;

public class IsAbstractProperty extends AbstractVarProperty implements UniqueVarProperty, SingleFragmentProperty {

    @Override
    public void buildString(StringBuilder builder) {
        builder.append("is-abstract");
    }

    @Override
    public Fragment getFragment(String start) {
        return Fragments.isAbstract(start);
    }

    @Override
    public void insert(InsertQueryExecutor insertQueryExecutor, Concept concept) throws IllegalStateException {
        concept.asType().setAbstract(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return !(o == null || getClass() != o.getClass());

    }

    @Override
    public int hashCode() {
        return 31;
    }
}
