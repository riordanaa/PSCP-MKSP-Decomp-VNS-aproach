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

/**
 * Implements a (1,1)-Swap neighborhood for the Maximum Set k-Covering Problem (MSKCP).
 * The goal of this neighborhood is to maximize the total number of covered points for a solution of a fixed size.
 *
 * It explores moves that swap one set currently in the solution ('setOut') with one set
 * not in the solution ('setIn'). An improving move is one that results in a net increase in coverage.
 */
public class Swap11Neighborhood extends Neighborhood<Swap11Neighborhood.Swap11Move, PSSCSolution, PSSCInstance> {

    private final int candidateListSize;

    /**
     * Default constructor. Uses a candidate list of size 50.
     */
    public Swap11Neighborhood() {
        this(50);
    }

    /**
     * Constructor allowing for a custom candidate list size.
     * @param candidateListSize The number of promising 'in' sets to consider for each 'out' set.
     */
    public Swap11Neighborhood(int candidateListSize) {
        this.candidateListSize = candidateListSize;
    }

    @Override
    public ExploreResult<Swap11Move, PSSCSolution, PSSCInstance> explore(PSSCSolution solution) {
        List<Swap11Move> moves = new ArrayList<>();
        var chosenSets = solution.getChosenSets();

        // Build a list of promising candidates to swap in.
        List<Integer> candidateSetsIn = buildCandidateList(solution);

        // For each set in the current solution...
        for (int setOut : chosenSets) {
            // Add a time check to ensure the algorithm stops if the time limit is up.
            if (TimeControl.isTimeUp()) {
                break; // Stop exploring and return any moves found so far.
            }
            // ...try swapping it with each of the best candidates.
            for (int setIn : candidateSetsIn) {
                // A set cannot be swapped with itself or another set already in the solution.
                if (setOut == setIn || chosenSets.contains(setIn)) {
                    continue;
                }
                // Create a move. The calculation of the score change is handled within the move class.
                moves.add(new Swap11Move(solution, setOut, setIn));
            }
        }
        return ExploreResult.fromList(moves);
    }

    /**
     * Builds a list of the most promising candidate sets to be swapped into the solution.
     * A "promising" candidate is one that covers points that are either uncovered or critically covered.
     * @param solution The current solution.
     * @return A list of candidate set IDs.
     */
    private List<Integer> buildCandidateList(PSSCSolution solution) {
        PSSCInstance instance = solution.getInstance();
        var uselessSets = instance.getUselessSets();
        var chosenSets = solution.getChosenSets();
        int[] coverCount = solution.getCoverCount();

        // Identify points that are "weakly" covered (covered by 1 or 0 sets).
        // These are the points where an incoming set can make the most difference.
        BitSet weakPoints = new BitSet(instance.getnPoints());
        for (int i = 0; i < coverCount.length; i++) {
            if (coverCount[i] <= 1) {
                weakPoints.add(i);
            }
        }

        // Score all potential incoming sets based on how many weak points they cover.
        List<Candidate> candidates = new ArrayList<>();
        for (int s = 0; s < instance.getnSets(); s++) {
            if (!chosenSets.contains(s) && !uselessSets.contains(s)) {
                BitSet contribution = instance.getCoveredPoints(s).clone();
                contribution.and(weakPoints); // Intersect with weak points
                if (!contribution.isEmpty()) {
                    candidates.add(new Candidate(s, contribution.size()));
                }
            }
        }

        // Sort candidates by their score in descending order.
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());

        // Return the IDs of the top N candidates.
        List<Integer> topCandidates = new ArrayList<>();
        for (int i = 0; i < Math.min(candidateListSize, candidates.size()); i++) {
            topCandidates.add(candidates.get(i).id());
        }
        return topCandidates;
    }

    // A small record to hold a candidate set and its score.
    private record Candidate(int id, int score) {}

    /**
     * Represents a move that swaps one set in the solution for another.
     * Calculates the change in total coverage.
     */
    public static class Swap11Move extends PSSCBaseMove {
        private final int setOut;
        private final int setIn;

        public Swap11Move(PSSCSolution solution, int setOut, int setIn) {
            super(solution);
            this.setOut = setOut;
            this.setIn = setIn;
            // The score is calculated lazily (when getScoreChange() is called)
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(setOut);
            solution.addSet(setIn);
            return solution;
        }

        /**
         * Calculates the change in total number of covered points resulting from this swap.
         * A positive value indicates an improvement for the MSKCP objective.
         * @return The net change in coverage.
         */
        @Override
        public double getScoreChange() {
            int[] coverCount = this.getSolution().getCoverCount();
            PSSCInstance instance = this.getSolution().getInstance();
            BitSet pointsIn = instance.getCoveredPoints(setIn);
            BitSet pointsOut = instance.getCoveredPoints(setOut);

            int coverageGain = 0;
            // Gain is from points covered by 'setIn' that were previously uncovered (coverCount == 0).
            for (int point : pointsIn) {
                if (coverCount[point] == 0) {
                    coverageGain++;
                }
            }

            int coverageLoss = 0;
            // Loss is from points that were ONLY covered by 'setOut' (coverCount == 1)
            // AND are NOT also covered by the incoming 'setIn'.
            for (int point : pointsOut) {
                if (coverCount[point] == 1 && !pointsIn.contains(point)) {
                    coverageLoss++;
                }
            }
            return coverageGain - coverageLoss;
        }

        @Override
        public String toString() {
            return "Swap11{out=" + setOut + ", in=" + setIn + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Swap11Move swap11Move = (Swap11Move) o;
            return setOut == swap11Move.setOut && setIn == swap11Move.setIn;
        }

        @Override
        public int hashCode() {
            return Objects.hash(setOut, setIn);
        }
    }
}