package io.grakn.graql.internal.gremlin.fragment;

import io.grakn.graql.internal.gremlin.FragmentPriority;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import static io.grakn.util.Schema.EdgeLabel.HAS_ROLE;

class OutHasRoleFragment extends AbstractFragment {

    OutHasRoleFragment(String start, String end) {
        super(start, end);
    }

    @Override
    public void applyTraversal(GraphTraversal<Vertex, Vertex> traversal) {
        traversal.out(HAS_ROLE.getLabel());
    }

    @Override
    public String getName() {
        return "-[has-role]->";
    }

    @Override
    public FragmentPriority getPriority() {
        return FragmentPriority.EDGE_BOUNDED;
    }

    @Override
    public long fragmentCost(long previousCost) {
        return previousCost * NUM_ROLES_PER_RELATION;
    }
}
