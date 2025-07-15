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
 * Implements a (2,2)-Swap neighborhood for the Maximum Set k-Covering Problem (MSKCP).
 * This is a large, powerful neighborhood designed to find complex improvements.
 *
 * It explores moves that swap two sets currently in the solution ('setOut1', 'setOut2')
 * with two sets not in the solution ('setIn1', 'setIn2').
 * This version is optimized for a "First Improvement" search strategy to avoid OutOfMemoryErrors
 * and includes a time check to ensure compliance with time limits.
 */
public class Swap22Neighborhood extends Neighborhood<Swap22Neighborhood.Swap22Move, PSSCSolution, PSSCInstance> {

    private final int candidateListSize;

    /**
     * Default constructor. Uses a candidate list of size 75.
     * Note: A larger candidate list is used here because we are selecting pairs.
     * Consider reducing this value if this neighborhood is too slow.
     */
    public Swap22Neighborhood() {
        this(25);
    }

    /**
     * Constructor allowing for a custom candidate list size.
     * @param candidateListSize The number of promising 'in' sets to consider.
     */
    public Swap22Neighborhood(int candidateListSize) {
        this.candidateListSize = candidateListSize;
    }

    /**
     * Standard explore method for "Best Improvement".
     * WARNING: This implementation can cause OutOfMemoryError on large instances by trying to
     * build a giant list of all possible moves. It is intentionally left non-functional.
     * Use findFirstImprovingMove for a memory-safe alternative.
     */
    @Override
    public ExploreResult<Swap22Move, PSSCSolution, PSSCInstance> explore(PSSCSolution solution) {
        // DO NOT IMPLEMENT a Best Improvement strategy here as it will consume too much memory.
        // Return an empty result to ensure safety if this method is ever called by a standard improver.
        return ExploreResult.empty();
    }

    /**
     * A memory-safe, First-Improvement exploration method.
     * It searches for the first (2,2)-swap move with a positive score change and returns immediately.
     * Includes a time check to ensure the algorithm can be interrupted if the time limit is exceeded.
     * @param solution The solution to explore.
     * @return An Optional containing the first improving move found, or an empty Optional if none exists or time runs out.
     */
    public Optional<Swap22Move> findFirstImprovingMove(PSSCSolution solution) {
        List<Integer> chosenSetsList = new ArrayList<>();
        for (int set : solution.getChosenSets()) {
            chosenSetsList.add(set);
        }
        // This neighborhood requires at least 2 sets in the solution.
        if (chosenSetsList.size() < 2) {
            return Optional.empty();
        }

        // Build a list of promising candidates to swap in.
        List<Integer> candidateSetsIn = buildCandidateList(solution);
        // We also need at least 2 candidates to form a pair.
        if (candidateSetsIn.size() < 2) {
            return Optional.empty();
        }

        // Efficiently generate unique pairs of sets to remove
        for (int i = 0; i < chosenSetsList.size(); i++) {
            int setOut1 = chosenSetsList.get(i);
            for (int j = i + 1; j < chosenSetsList.size(); j++) {
                // =========================================================================
                // CRITICAL TIME CHECK: Ensures the algorithm stops if the time limit is up.
                // This prevents the algorithm from running past its allocated time, which is
                // essential for fair experiments.
                if (TimeControl.isTimeUp()) {
                    return Optional.empty();
                }
                // =========================================================================
                
                int setOut2 = chosenSetsList.get(j);

                // Efficiently generate unique pairs of sets to add from the candidate list
                for (int k = 0; k < candidateSetsIn.size(); k++) {
                    int setIn1 = candidateSetsIn.get(k);
                    for (int l = k + 1; l < candidateSetsIn.size(); l++) {
                        int setIn2 = candidateSetsIn.get(l);

                        // Create a single move object to evaluate it
                        var move = new Swap22Move(solution, setOut1, setOut2, setIn1, setIn2);

                        // Check if it's an improving move
                        if (move.getScoreChange() > 0) {
                            return Optional.of(move); // Success! Found an improving move, return immediately.
                        }
                    }
                }
            }
        }

        return Optional.empty(); // No improving move found after checking all possibilities.
    }


    /**
     * Builds a list of the most promising candidate sets to be swapped into the solution.
     * A "promising" candidate is one that covers points that are either uncovered or critically covered (by only one set).
     * @param solution The current solution.
     * @return A list of candidate set IDs, sorted by promise.
     */
    private List<Integer> buildCandidateList(PSSCSolution solution) {
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
        for (int i = 0; i < Math.min(candidateListSize, candidates.size()); i++) {
            topCandidates.add(candidates.get(i).id());
        }
        return topCandidates;
    }

    private record Candidate(int id, int score) {}

    /**
     * Represents a move that swaps two sets in the solution for two others.
     */
    public static class Swap22Move extends PSSCBaseMove {
        private final int setOut1, setOut2, setIn1, setIn2;

        public Swap22Move(PSSCSolution solution, int setOut1, int setOut2, int setIn1, int setIn2) {
            super(solution);
            this.setOut1 = setOut1;
            this.setOut2 = setOut2;
            this.setIn1 = setIn1;
            this.setIn2 = setIn2;
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(setOut1);
            solution.removeSet(setOut2);
            solution.addSet(setIn1);
            solution.addSet(setIn2);
            return solution;
        }

        @Override
        public double getScoreChange() {
            int[] coverCount = this.getSolution().getCoverCount();
            PSSCInstance instance = this.getSolution().getInstance();

            BitSet pointsIn1 = instance.getCoveredPoints(setIn1);
            BitSet pointsIn2 = instance.getCoveredPoints(setIn2);
            BitSet pointsOut1 = instance.getCoveredPoints(setOut1);
            BitSet pointsOut2 = instance.getCoveredPoints(setOut2);

            BitSet allPointsIn = pointsIn1.clone();
            allPointsIn.or(pointsIn2);

            BitSet allPointsOut = pointsOut1.clone();
            allPointsOut.or(pointsOut2);

            int coverageGain = 0;
            // Gain is from points covered by incoming sets that were previously uncovered
            for (int point : allPointsIn) {
                if (coverCount[point] == 0) {
                    coverageGain++;
                }
            }

            int coverageLoss = 0;
            // Loss is from points that become uncovered after this move.
            // This only happens if a point is NOT covered by the incoming sets...
            for (int point : allPointsOut) {
                if (!allPointsIn.contains(point)) {
                    // ...and if the outgoing sets were the ONLY ones covering it.
                    boolean inOut1 = pointsOut1.contains(point);
                    boolean inOut2 = pointsOut2.contains(point);

                    // Case 1: Point covered only by setOut1
                    if (inOut1 && !inOut2 && coverCount[point] == 1) {
                        coverageLoss++;
                    }
                    // Case 2: Point covered only by setOut2
                    else if (!inOut1 && inOut2 && coverCount[point] == 1) {
                        coverageLoss++;
                    }
                    // Case 3: Point covered only by BOTH setOut1 and setOut2
                    else if (inOut1 && inOut2 && coverCount[point] == 2) {
                        coverageLoss++;
                    }
                }
            }
            return coverageGain - coverageLoss;
        }

        @Override
        public String toString() {
            return "Swap22{out=[" + setOut1 + "," + setOut2 + "], in=[" + setIn1 + "," + setIn2 + "]}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Swap22Move that = (Swap22Move) o;
            // Order doesn't matter for the pairs
            return (this.setOut1 == that.setOut1 && this.setOut2 == that.setOut2 || this.setOut1 == that.setOut2 && this.setOut2 == that.setOut1) &&
                   (this.setIn1 == that.setIn1 && this.setIn2 == that.setIn2 || this.setIn1 == that.setIn2 && this.setIn2 == that.setIn1);
        }

        @Override
        public int hashCode() {
            // Order-independent hash
            int outHash = Integer.hashCode(setOut1) + Integer.hashCode(setOut2);
            int inHash = Integer.hashCode(setIn1) + Integer.hashCode(setIn2);
            return Objects.hash(outHash, inHash);
        }
    }
}