package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;
import es.urjc.etsii.grafo.util.collections.BitSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Neighborhood that tries to add a single set.
 * To avoid evaluating useless moves, generates moves that cover at least one currently uncovered point.
 */
public class AddNeighborhood extends Neighborhood<AddNeighborhood.AddMove, PSSCSolution, PSSCInstance> {

    @Override
    public ExploreResult<AddMove, PSSCSolution, PSSCInstance> explore(PSSCSolution sol) {


        List<AddMove> moves = new ArrayList<>();
        int nSets = sol.getInstance().getnSets();

        for (int s = 0; s < nSets; s++) {
            // todo complete
        }
        return ExploreResult.fromList(moves);
    }

    public static class AddMove extends PSSCBaseMove {

        private final int setId;

        public AddMove(PSSCSolution solution, int setId) {
            super(solution);
            this.setId = setId;
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.addSet(setId);
            return solution;
        }

        @Override public double getScoreChange() {
            // adding a set always increases the score by 1, as it adds one more set to the solution
            return +1;
        }

        @Override public String toString() { return "Add{" + setId + '}'; }

        @Override public boolean equals(Object o) {
            return (o instanceof AddMove m) && m.setId == this.setId;
        }

        @Override public int hashCode() { return Integer.hashCode(setId); }
    }
}
