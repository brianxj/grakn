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

package io.grakn.graql.internal.shell;

import io.grakn.graql.GraqlShell;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class GraqlSignalHandler implements SignalHandler {

    private final GraqlShell shell;

    public GraqlSignalHandler(GraqlShell shell) {
        this.shell = shell;
    }

    @Override
    public void handle(Signal signal) {
        if (signal.getName().equals("INT")) {
            try {
                shell.interrupt();
            } catch (Throwable e) {
                System.err.println(e.getMessage());
                System.exit(0);
            }
        }
    }
}
