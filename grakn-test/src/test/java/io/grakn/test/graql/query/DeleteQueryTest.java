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

package io.grakn.test.graql.query;

import io.grakn.graql.Graql;
import io.grakn.graql.QueryBuilder;
import io.grakn.graql.Var;
import io.grakn.test.AbstractMovieGraphTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.grakn.graql.Graql.var;
import static io.grakn.util.Schema.MetaSchema.ENTITY_TYPE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class DeleteQueryTest extends AbstractMovieGraphTest {

    private QueryBuilder qb;
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        // TODO: Fix delete queries in titan
        assumeFalse(usingTitan());

        qb = Graql.withGraph(graph);
    }

    @Test
    public void testDeleteMultiple() {
        qb.insert(var().id("fake-type").isa(ENTITY_TYPE.getId())).execute();
        qb.insert(var().id("fake-type-1").isa("fake-type"), var().id("fake-type-2").isa("fake-type")).execute();

        assertEquals(2, qb.match(var("x").isa("fake-type")).stream().count());

        qb.match(var("x").isa("fake-type")).delete("x").execute();

        assertFalse(qb.match(var().isa("fake-type")).ask().execute());

        qb.match(var("x").id("http://grakn.io/fake-type")).delete("x");
    }

    @Test
    public void testDeleteName() {
        qb.insert(
                var().id("123").isa("person")
                        .has("real-name", "Bob")
                        .has("real-name", "Robert")
                        .has("gender", "male")
        ).execute();

        assertTrue(qb.match(var().id("123").has("real-name", "Bob")).ask().execute());
        assertTrue(qb.match(var().id("123").has("real-name", "Robert")).ask().execute());
        assertTrue(qb.match(var().id("123").has("gender", "male")).ask().execute());

        qb.match(var("x").id("123")).delete(var("x").has("real-name")).execute();

        assertFalse(qb.match(var().id("123").has("real-name", "Bob")).ask().execute());
        assertFalse(qb.match(var().id("123").has("real-name", "Robert")).ask().execute());
        assertTrue(qb.match(var().id("123").has("gender", "male")).ask().execute());

        qb.match(var("x").id("123")).delete("x").execute();
        assertFalse(qb.match(var().id("123")).ask().execute());
    }

    @Test
    public void testDeleteSpecificEdge() {
        Var actor = var().id("has-cast").hasRole("actor");
        Var productionWithCast = var().id("has-cast").hasRole("production-with-cast");

        assertTrue(qb.match(actor).ask().execute());
        assertTrue(qb.match(productionWithCast).ask().execute());

        qb.match(var("x").id("has-cast")).delete(var("x").hasRole("actor")).execute();
        assertTrue(qb.match(var().id("has-cast")).ask().execute());
        assertFalse(qb.match(actor).ask().execute());
        assertTrue(qb.match(productionWithCast).ask().execute());

        qb.insert(actor).execute();
        assertTrue(qb.match(actor).ask().execute());
    }

    @Test
    public void testDeleteSpecificName() {
        qb.insert(
                var().id("123").isa("person")
                        .has("real-name", "Bob")
                        .has("real-name", "Robert")
                        .has("gender", "male")
        ).execute();

        assertTrue(qb.match(var().id("123").has("real-name", "Bob")).ask().execute());
        assertTrue(qb.match(var().id("123").has("real-name", "Robert")).ask().execute());
        assertTrue(qb.match(var().id("123").has("gender", "male")).ask().execute());

        qb.match(var("x").id("123")).delete(var("x").has("real-name", "Robert")).execute();

        assertTrue(qb.match(var().id("123").has("real-name", "Bob")).ask().execute());
        assertFalse(qb.match(var().id("123").has("real-name", "Robert")).ask().execute());
        assertTrue(qb.match(var().id("123").has("gender", "male")).ask().execute());

        qb.match(var("x").id("123")).delete("x").execute();
        assertFalse(qb.match(var().id("123")).ask().execute());
    }

    @Test
    public void testErrorWhenDeleteValue() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(allOf(containsString("delet"), containsString("value")));
        qb.match(var("x").isa("movie")).delete(var("x").value()).execute();
    }
}
