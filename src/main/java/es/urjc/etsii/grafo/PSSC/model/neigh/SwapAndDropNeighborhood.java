package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;
import es.urjc.etsii.grafo.util.collections.BitSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A powerful composite neighborhood that explores a (1,1)-Swap followed immediately by a Drop.
 * This move is designed to find strictly improving paths that simple Drop or Swap moves cannot.
 * This version includes critical performance optimizations to make it viable for large instances.
 *
 * A SwapAndDrop move consists of:
 * 1. Swapping one set 'setOut' (from the solution) with 'setIn' (not in the solution).
 * 2. After the swap, checking if any other set 'setDrop' in the solution has become redundant.
 *
 * This entire sequence constitutes a single move with a score change of -1.
 */
public class SwapAndDropNeighborhood extends Neighborhood<SwapAndDropNeighborhood.SwapAndDropMove, PSSCSolution, PSSCInstance> {

    private final int candidateListSize;

    /**
     * Constructor for the optimized SwapAndDropNeighborhood.
     *
     * @param candidateListSize The number of promising 'in' candidates to consider for each 'out' set.
     *                          A smaller value is faster but less thorough. 50 is a good starting point.
     */
    public SwapAndDropNeighborhood(int candidateListSize) {
        this.candidateListSize = candidateListSize;
    }

    /**
     * Default constructor uses a candidate list size of 50.
     */
    public SwapAndDropNeighborhood() {
        this(50);
    }

    @Override
    public ExploreResult<SwapAndDropMove, PSSCSolution, PSSCInstance> explore(PSSCSolution solution) {
        PSSCInstance instance = solution.getInstance();
        List<SwapAndDropMove> moves = new ArrayList<>();
        var chosenSets = solution.getChosenSets();

        if (chosenSets.size() < 2) {
            // Cannot perform a swap and drop if there are fewer than 2 sets.
            return ExploreResult.fromList(moves);
        }

        // OPTIMIZATION 1: Build a candidate list of promising sets to swap IN.
        List<Integer> candidateSetsIn = buildCandidateList(solution);

        // OPTIMIZATION 2: Pre-calculate coverage data for efficiency.
        int[] coverCount = solution.getCoverCount();
        int minRequired = solution.minCoveredRequired();
        int currentCoveredPoints = 0;
        for (int count : coverCount) {
            if (count > 0) currentCoveredPoints++;
        }

        for (int setOut : chosenSets) {
            BitSet pointsInSetOut = instance.getCoveredPoints(setOut);

            for (int setIn : candidateSetsIn) {
                // A candidate should not already be in the chosen set.
                if (chosenSets.contains(setIn)) continue;

                // OPTIMIZATION 3: Efficiently predict coverage after the swap.
                int netCoverageChangeFromSwap = 0;
                BitSet pointsInSetIn = instance.getCoveredPoints(setIn);

                for (int point : pointsInSetIn) {
                    if (coverCount[point] == 0) netCoverageChangeFromSwap++;
                }
                for (int point : pointsInSetOut) {
                    if (coverCount[point] == 1 && !pointsInSetIn.contains(point)) {
                        netCoverageChangeFromSwap--;
                    }
                }

                int coverageAfterSwap = currentCoveredPoints + netCoverageChangeFromSwap;
                // If the swap itself makes the solution infeasible, no drop can fix it. Skip.
                if (coverageAfterSwap < minRequired) {
                    continue;
                }

                // Now, efficiently check for a possible drop.
                for (int setDrop : chosenSets) {
                    if (setDrop == setOut) continue; // Cannot drop the set we are swapping out in the same move.

                    int newlyUncoveredByDrop = 0;
                    for (int point : instance.getCoveredPoints(setDrop)) {
                        // Simulate the cover count for this point after the swap.
                        int countAfterSwap = coverCount[point];
                        if (pointsInSetOut.contains(point)) countAfterSwap--;
                        if (pointsInSetIn.contains(point)) countAfterSwap++;

                        // If the point would be uniquely covered after the swap, then dropping setDrop would uncover it.
                        if (countAfterSwap == 1) {
                            newlyUncoveredByDrop++;
                        }
                    }

                    if (coverageAfterSwap - newlyUncoveredByDrop >= minRequired) {
                        moves.add(new SwapAndDropMove(solution, setOut, setIn, setDrop));
                        // For a "first improvement" strategy, we could 'return ExploreResult.fromList(moves);' here.
                        // For "best improvement", we continue building the full list.
                    }
                }
            }
        }
        return ExploreResult.fromList(moves);
    }

    /**
     * Builds a list of promising candidate sets to consider for 'setIn'.
     * A good candidate is one that covers points that are currently critically covered.
     */
    private List<Integer> buildCandidateList(PSSCSolution solution) {
        PSSCInstance instance = solution.getInstance();
        var uselessSets = instance.getUselessSets();
        var chosenSets = solution.getChosenSets();
        int[] coverCount = solution.getCoverCount();

        // Find all critically covered points (covered by only one set).
        BitSet criticalPoints = new BitSet(instance.getnPoints());
        for (int i = 0; i < coverCount.length; i++) {
            if (coverCount[i] == 1) {
                criticalPoints.add(i);
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        if (!criticalPoints.isEmpty()) {
            for (int s = 0; s < instance.getnSets(); s++) {
                if (!chosenSets.contains(s) && !uselessSets.contains(s)) {
                    BitSet contribution = instance.getCoveredPoints(s).clone();
                    contribution.and(criticalPoints); // Intersect with critical points.
                    if (!contribution.isEmpty()) {
                        candidates.add(new Candidate(s, contribution.size()));
                    }
                }
            }
        }

        // Sort candidates by their score (how many critical points they cover), descending.
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());

        // Return the IDs of the top N candidates.
        List<Integer> topCandidates = new ArrayList<>();
        for (int i = 0; i < Math.min(this.candidateListSize, candidates.size()); i++) {
            topCandidates.add(candidates.get(i).id());
        }
        return topCandidates;
    }

    // A private record for sorting candidates.
    private record Candidate(int id, int score) {}

    /**
     * Represents a single composite move: Swap one set, then Drop another.
     */
    public static class SwapAndDropMove extends PSSCBaseMove {
        private final int setOut;
        private final int setIn;
        private final int setDrop;

        public SwapAndDropMove(PSSCSolution solution, int setOut, int setIn, int setDrop) {
            super(solution);
            this.setOut = setOut;
            this.setIn = setIn;
            this.setDrop = setDrop;
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(setOut);
            solution.addSet(setIn);
            solution.removeSet(setDrop);
            return solution;
        }

        @Override
        public double getScoreChange() {
            return -1;
        }

        @Override
        public String toString() {
            return "SwapAndDrop{out=" + setOut + ", in=" + setIn + ", drop=" + setDrop + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SwapAndDropMove that = (SwapAndDropMove) o;
            return setOut == that.setOut && setIn == that.setIn && setDrop == that.setDrop;
        }

        @Override
        public int hashCode() {
            return Objects.hash(setOut, setIn, setDrop);
        }
    }
}