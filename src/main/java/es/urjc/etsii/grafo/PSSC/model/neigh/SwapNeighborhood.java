package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;
import es.urjc.etsii.grafo.util.collections.BitSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neighborhood that explores swapping one set from the solution with one set not in the solution.
 * <p>
 * The size of the solution does not change. This move is used to explore different
 * combinations of sets of the same size, potentially finding a better configuration
 * that allows for subsequent 'drop' moves. A swap is only generated if the resulting
 * solution is predicted to be feasible.
 */
public class SwapNeighborhood extends Neighborhood<SwapNeighborhood.SwapMove, PSSCSolution, PSSCInstance> {

    /**
     * Explores all valid 'swap' moves from the current solution.
     * <p>
     * A swap move involves removing a 'setOut' from the solution and adding a 'setIn'.
     * To be efficient, this method pre-calculates the change in coverage and only generates
     * moves that are guaranteed to result in a feasible solution.
     *
     * @param sol The current solution.
     * @return An ExploreResult containing a list of all possible valid SwapMoves.
     */
    @Override
    public ExploreResult<SwapMove, PSSCSolution, PSSCInstance> explore(PSSCSolution sol) {
        PSSCInstance instance = sol.getInstance();
        List<SwapMove> moves = new ArrayList<>();

        // Get data for efficient calculation
        int nSets = instance.getnSets();
        int[] coverCount = sol.getCoverCount();
        int currentCoveredPoints = sol.coveredPoints().size();
        int minRequired = sol.minCoveredRequired();

        var chosenSets = sol.getChosenSets();
        var uselessSets = instance.getUselessSets();

        // Iterate through each set to potentially remove from the solution
        for (int setOut : chosenSets) {
            BitSet pointsInSetOut = instance.getCoveredPoints(setOut);

            // Calculate how many points would become uncovered if we drop setOut
            int newlyUncovered = 0;
            for (int point : pointsInSetOut) {
                if (coverCount[point] == 1) {
                    newlyUncovered++;
                }
            }

            // Iterate through each set to potentially add to the solution
            for (int setIn = 0; setIn < nSets; setIn++) {
                // A set can be added if it's not already chosen AND it's not useless
                if (chosenSets.contains(setIn) || uselessSets.contains(setIn)) {
                    continue;
                }

                BitSet pointsInSetIn = instance.getCoveredPoints(setIn);
                
                // Calculate how many new points would be covered by adding setIn,
                // considering that setOut has been removed.
                int newlyCovered = 0;
                for (int point : pointsInSetIn) {
                    if (coverCount[point] == 0) {
                        // This point was not covered at all, so it's a new contribution.
                        newlyCovered++;
                    } else if (coverCount[point] == 1 && pointsInSetOut.contains(point)) {
                        // This point was only covered by setOut, which we are removing.
                        // Adding setIn re-covers it, so it's a new contribution relative to the post-drop state.
                        newlyCovered++;
                    }
                }

                // Predict the new total coverage after the swap
                int predictedCoverage = currentCoveredPoints - newlyUncovered + newlyCovered;

                // Only create the move if the swap maintains feasibility
                if (predictedCoverage >= minRequired) {
                    moves.add(new SwapMove(sol, setOut, setIn));
                }
            }
        }
        return ExploreResult.fromList(moves);
    }

    /**
     * Represents a move that swaps one set in the solution for another.
     */
    public static class SwapMove extends PSSCBaseMove {
        private final int setOut;
        private final int setIn;

        /**
         * Creates a new swap move.
         *
         * @param solution The solution context.
         * @param setOut   The ID of the set to remove from the solution.
         * @param setIn    The ID of the set to add to the solution.
         */
        public SwapMove(PSSCSolution solution, int setOut, int setIn) {
            super(solution);
            this.setOut = setOut;
            this.setIn = setIn;
        }

        /**
         * Executes the swap by removing one set and adding another.
         *
         * @param solution The solution to modify.
         * @return The modified solution.
         */
        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(setOut);
            solution.addSet(setIn);
            return solution;
        }

        /**
         * The score change for a SwapMove is always 0, as the total number of sets
         * in the solution does not change. This type of move explores the solution
         * space at the same objective value.
         *
         * @return A score change of 0.
         */
        @Override
        public double getScoreChange() {
            return 0;
        }

        @Override
        public String toString() {
            return "Swap{out=" + setOut + ", in=" + setIn + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SwapMove swapMove = (SwapMove) o;
            return setOut == swapMove.setOut && setIn == swapMove.setIn;
        }

        @Override
        public int hashCode() {
            return Objects.hash(setOut, setIn);
        }
    }
}