package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neighborhood that explores removing a single set from the solution.
 * <p>
 * This move is only generated if the solution remains feasible after the removal.
 * It is a strictly improving move as it reduces the solution size by 1.
 */
public class DropNeighborhood extends Neighborhood<DropNeighborhood.DropMove, PSSCSolution, PSSCInstance> {

    /**
     * Explores all valid 'drop' moves from the current solution.
     * <p>
     * A move is considered valid if, after removing the set, the total number of
     * covered points still meets the minimum requirement. This is checked efficiently
     * using the solution's coverCount array.
     *
     * @param sol The current solution.
     * @return An ExploreResult containing a list of all possible improving DropMoves.
     */
    @Override
    public ExploreResult<DropMove, PSSCSolution, PSSCInstance> explore(PSSCSolution sol) {
        var instance = sol.getInstance();
        List<DropMove> moves = new ArrayList<>();

        // Pre-calculate coverage counts for efficiency.
        // This is the most expensive part, but we only do it once per exploration.
        int currentCoveredPoints = sol.coveredPoints().size();
        int minRequired = sol.minCoveredRequired();
        int[] coverCount = sol.getCoverCount();

        // Iterate only through the sets currently in the solution
        for (int set : sol.getChosenSets()) {
            // Calculate how many points would become uncovered if we drop this set.
            // A point becomes uncovered only if its cover count is exactly 1.
            int newlyUncoveredCount = 0;
            for (int point : instance.getCoveredPoints(set)) {
                if (coverCount[point] == 1) {
                    newlyUncoveredCount++;
                }
            }

            // Check if the solution remains feasible after the drop
            if (currentCoveredPoints - newlyUncoveredCount >= minRequired) {
                // If so, this is a valid, improving move
                moves.add(new DropMove(sol, set));
            }
        }

        return ExploreResult.fromList(moves);
    }


    /**
     * Represents a move that removes a single set from the solution.
     */
    public static class DropMove extends PSSCBaseMove {

        private final int setId;

        /**
         * Creates a new move to remove a set.
         *
         * @param solution The solution context for this move.
         * @param setId    The ID of the set to be removed.
         */
        public DropMove(PSSCSolution solution, int setId) {
            super(solution);
            this.setId = setId;
        }

        /**
         * Executes the move by removing the specified set from the solution.
         * This method modifies the solution in place.
         *
         * @param solution The solution to modify.
         * @return The modified solution.
         */
        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(this.setId);
            return solution;
        }

        /**
         * The score change for a DropMove is always -1, as we are minimizing the
         * number of sets, and this move removes one set from the solution.
         *
         * @return A score change of -1.
         */
        @Override
        public double getScoreChange() {
            return -1;
        }

        @Override
        public String toString() {
            return "Drop{" + setId + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DropMove dropMove = (DropMove) o;
            return setId == dropMove.setId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(setId);
        }
    }
}