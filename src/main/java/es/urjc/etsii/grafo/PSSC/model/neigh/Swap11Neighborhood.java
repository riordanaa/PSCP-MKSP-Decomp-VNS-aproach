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
import java.util.Optional;

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

    /**
     * Standard explore method that generates all possible valid moves.
     * Used for a "Best Improvement" strategy.
     */
    @Override
    public ExploreResult<Swap11Move, PSSCSolution, PSSCInstance> explore(PSSCSolution solution) {
        List<Swap11Move> moves = new ArrayList<>();
        var chosenSets = solution.getChosenSets();
        List<Integer> candidateSetsIn = buildCandidateList(solution);

        for (int setOut : chosenSets) {
            for (int setIn : candidateSetsIn) {
                if (setOut == setIn || chosenSets.contains(setIn)) {
                    continue;
                }
                moves.add(new Swap11Move(solution, setOut, setIn));
            }
        }
        return ExploreResult.fromList(moves);
    }

    /**
     * NEW METHOD: A memory-safe, "First Improvement" exploration method.
     * Searches for the first (1,1)-swap move with a positive score change and returns immediately.
     * @param solution The solution to explore.
     * @return An Optional containing the first improving move found, or an empty Optional if none exists.
     */
    public Optional<Swap11Move> findFirstImprovingMove(PSSCSolution solution) {
        var chosenSets = solution.getChosenSets();
        List<Integer> candidateSetsIn = buildCandidateList(solution);

        // For each set in the current solution...
        for (int setOut : chosenSets) {
            // ...try swapping it with each of the best candidates.
            for (int setIn : candidateSetsIn) {
                if (setOut == setIn || chosenSets.contains(setIn)) {
                    continue;
                }

                // Create a move object to evaluate its score change
                var move = new Swap11Move(solution, setOut, setIn);

                // Check if it's an improving move
                if (move.getScoreChange() > 0) {
                    return Optional.of(move); // Success! An improving move was found, return immediately.
                }
            }
        }
        
        return Optional.empty(); // No improving move was found after checking all combinations.
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
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            solution.removeSet(setOut);
            solution.addSet(setIn);
            return solution;
        }

        @Override
        public double getScoreChange() {
            int[] coverCount = this.getSolution().getCoverCount();
            PSSCInstance instance = this.getSolution().getInstance();
            BitSet pointsIn = instance.getCoveredPoints(setIn);
            BitSet pointsOut = instance.getCoveredPoints(setOut);

            int coverageGain = 0;
            for (int point : pointsIn) {
                if (coverCount[point] == 0) {
                    coverageGain++;
                }
            }

            int coverageLoss = 0;
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