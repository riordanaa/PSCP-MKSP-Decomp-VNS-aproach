package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;
import es.urjc.etsii.grafo.util.TimeControl;
import es.urjc.etsii.grafo.util.collections.BitSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements a powerful and asymmetrical (2,1)-opt neighborhood.
 * This move is a form of Large Neighborhood Search (LNS) designed to make significant,
 * structural changes to the solution.
 *
 * It works by finding a move where removing TWO sets from the solution and adding ONE set
 * from outside results in a net increase in coverage after the solution is repaired back
 * to its original size 'k'.
 */
public class Swap21optNeighborhood extends Neighborhood<Swap21optNeighborhood.Swap21optMove, PSSCSolution, PSSCInstance> {

    private final int candidateListSize;

    public Swap21optNeighborhood() {
        this(50); // A default candidate list size of 50 is a good starting point
    }

    public Swap21optNeighborhood(int candidateListSize) {
        this.candidateListSize = candidateListSize;
    }

    /**
     * This method is intentionally left empty as a safety measure.
     * Its logic would be too slow and memory-intensive for a best-improvement strategy.
     * Use findFirstImprovingMove() instead for a safe, efficient exploration.
     */
    @Override
    public ExploreResult<Swap21optMove, PSSCSolution, PSSCInstance> explore(PSSCSolution solution) {
        return ExploreResult.empty();
    }

    /**
     * A memory-safe, First-Improvement exploration method. It searches for the first (2,1)-opt
     * move that yields a better coverage after repair, and returns immediately.
     *
     * @param solution The solution to explore.
     * @return An Optional containing the first improving move found, or an empty Optional if none exists.
     */
    public Optional<Swap21optMove> findFirstImprovingMove(PSSCSolution solution) {
        List<Integer> chosenSetsList = new ArrayList<>();
        for (int set : solution.getChosenSets()) {
            chosenSetsList.add(set);
        }
        if (chosenSetsList.size() < 2) return Optional.empty();

        List<Integer> candidateSetsIn = buildCandidateList(solution, this.candidateListSize);
        if (candidateSetsIn.isEmpty()) return Optional.empty();

        // Loop through all unique pairs of sets to remove
        for (int i = 0; i < chosenSetsList.size(); i++) {
            int setOut1 = chosenSetsList.get(i);
            for (int j = i + 1; j < chosenSetsList.size(); j++) {

                // Add a time check to ensure the neighborhood is responsive
                if (TimeControl.isTimeUp()) {
                    return Optional.empty();
                }

                int setOut2 = chosenSetsList.get(j);

                // For each pair, loop through promising candidates to add
                for (int setIn1 : candidateSetsIn) {
                    
                    // Hypothesize the move and its repair
                    PSSCSolution tempSolution = solution.cloneSolution();
                    tempSolution.removeSet(setOut1);
                    tempSolution.removeSet(setOut2);
                    tempSolution.addSet(setIn1);

                    // OPTIMIZATION: Use a candidate list to find the best repair set,
                    // instead of searching all N sets.
                    int bestRepairSet = findBestRepairSet(tempSolution);

                    if (bestRepairSet != -1) {
                        tempSolution.addSet(bestRepairSet);
                    } else {
                        // Cannot repair, this path is invalid, so skip it
                        continue;
                    }
                    
                    // Now, check if this complex move was actually an improvement
                    if (tempSolution.coveredPoints().size() > solution.coveredPoints().size()) {
                        // Found an improving move! Return it immediately.
                        return Optional.of(new Swap21optMove(solution, setOut1, setOut2, setIn1));
                    }
                }
            }
        }
        
        return Optional.empty(); // No improving move was found
    }

    /**
     * Finds the single best set to add to a given solution to maximize new coverage.
     * Uses a candidate list for efficiency.
     */
    private int findBestRepairSet(PSSCSolution solution) {
        // We use a smaller candidate list for the repair step as it's called more frequently
        List<Integer> repairCandidates = buildCandidateList(solution, 25); 

        int bestSet = -1;
        int maxNewCovered = -1;
        var currentlyCovered = solution.coveredPoints();

        for (int s : repairCandidates) {
            // The candidate list already filters for sets not in the solution, but this is a safe check.
            if (!solution.getChosenSets().contains(s)) {
                var contribution = solution.getInstance().getCoveredPoints(s).clone();
                contribution.andNot(currentlyCovered);
                int newCovered = contribution.size();
                if (newCovered > maxNewCovered) {
                    maxNewCovered = newCovered;
                    bestSet = s;
                }
            }
        }
        return bestSet;
    }

    /**
     * Builds a list of promising candidate sets to add to a solution.
     */
    private List<Integer> buildCandidateList(PSSCSolution solution, int size) {
        PSSCInstance instance = solution.getInstance();
        var uselessSets = instance.getUselessSets();
        var chosenSets = solution.getChosenSets();
        int[] coverCount = solution.getCoverCount();

        BitSet weakPoints = new BitSet(instance.getnPoints());
        for (int i = 0; i < coverCount.length; i++) {
            if (coverCount[i] <= 1) {
                weakPoints.add(i);
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int s = 0; s < instance.getnSets(); s++) {
            if (!chosenSets.contains(s) && !uselessSets.contains(s)) {
                BitSet contribution = instance.getCoveredPoints(s).clone();
                contribution.and(weakPoints);
                if (!contribution.isEmpty()) {
                    candidates.add(new Candidate(s, contribution.size()));
                }
            }
        }
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());

        List<Integer> topCandidates = new ArrayList<>();
        for (int i = 0; i < Math.min(size, candidates.size()); i++) {
            topCandidates.add(candidates.get(i).id());
        }
        return topCandidates;
    }

    private record Candidate(int id, int score) {}

    /**
     * Represents a (2,1)-opt move, which removes two sets and adds one.
     * The solution must be repaired to its original size 'k' after this move is executed.
     */
    public static class Swap21optMove extends PSSCBaseMove {
        private final int setOut1, setOut2, setIn1;

        public Swap21optMove(PSSCSolution solution, int setOut1, int setOut2, int setIn1) {
            super(solution);
            this.setOut1 = setOut1;
            this.setOut2 = setOut2;
            this.setIn1 = setIn1;
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(setOut1);
            solution.removeSet(setOut2);
            solution.addSet(setIn1);
            return solution;
        }
        
        /**
         * The score change for this move itself is not meaningful, as the final score
         * depends on the repair step. We return 1.0 as a placeholder to signify it's
         * an improving move, but the real check is done in the neighborhood exploration.
         */
        @Override
        public double getScoreChange() {
            return 1.0;
        }

        @Override
        public String toString() {
            return "Swap21opt{out=[" + setOut1 + "," + setOut2 + "], in=" + setIn1 + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Swap21optMove that = (Swap21optMove) o;
            // Order of setOut doesn't matter
            return (setOut1 == that.setOut1 && setOut2 == that.setOut2 || setOut1 == that.setOut2 && setOut2 == that.setOut1) && setIn1 == that.setIn1;
        }

        @Override
        public int hashCode() {
            // Order-independent hash for setOut
            int outHash = Integer.hashCode(setOut1) + Integer.hashCode(setOut2);
            return Objects.hash(outHash, setIn1);
        }
    }
}