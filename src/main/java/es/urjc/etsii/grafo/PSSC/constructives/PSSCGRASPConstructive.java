package es.urjc.etsii.grafo.PSSC.constructives;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.CollectionUtil;
import es.urjc.etsii.grafo.util.collections.BitSet;
import es.urjc.etsii.grafo.util.random.RandomManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An efficient GRASP (Greedy Randomized Adaptive Search Procedure) constructive for the PSCP.
 * This constructive introduces strategic randomness to generate diverse, high-quality initial solutions.
 *
 * The process is two-phase:
 * 1. GRASP Construction: At each step, build a Restricted Candidate List (RCL) of the best
 *    available sets and randomly select one to add to the solution. Repeat until feasible.
 * 2. Redundancy Elimination: A fast post-processing step to remove any unnecessary sets from the
 *    solution, making it lean.
 */
public class PSSCGRASPConstructive extends Constructive<PSSCSolution, PSSCInstance> {

    /**
     * The size of the Restricted Candidate List (RCL). A value of 1 makes this a pure greedy constructive.
     * A value of 3-5 is a good starting point for tuning.
     */
    private final int rclSize;

    /**
     * Constructor for the GRASP constructive.
     * @param rclSize The number of top candidates to consider at each step.
     */
    public PSSCGRASPConstructive(int rclSize) {
        this.rclSize = rclSize;
    }

    /**
     * Default constructor uses a common RCL size of 3.
     */
    public PSSCGRASPConstructive() {
        this(3);
    }


    @Override
    public PSSCSolution construct(PSSCSolution solution) {
        graspConstruction(solution);
        redundancyElimination(solution);
        solution.notifyUpdate();
        return solution;
    }

    private void graspConstruction(PSSCSolution solution) {
        var instance = solution.getInstance();
        BitSet currentlyCovered = solution.coveredPoints();

        while (currentlyCovered.size() < solution.minCoveredRequired()) {
            // --- GRASP LOGIC STARTS HERE ---
            
            // 1. Build a list of all possible candidates and their scores
            List<Candidate> candidates = new ArrayList<>();
            for (int s = 0; s < instance.getnSets(); s++) {
                if (solution.getChosenSets().contains(s) || instance.getUselessSets().contains(s)) {
                    continue;
                }

                BitSet contribution = instance.getCoveredPoints(s).clone();
                contribution.andNot(currentlyCovered);
                int newCoveredCount = contribution.size();
                if(newCoveredCount > 0){
                    candidates.add(new Candidate(s, newCoveredCount));
                }
            }

            if (candidates.isEmpty()) {
                // No more sets can be added to improve coverage.
                break;
            }

            // 2. Sort the candidates by score in descending order (best first)
            candidates.sort(Comparator.comparingInt(Candidate::score).reversed());

            // 3. Build the Restricted Candidate List (RCL)
            int currentRCLSize = Math.min(this.rclSize, candidates.size());
            List<Candidate> rcl = candidates.subList(0, currentRCLSize);

            // 4. Pick a random candidate from the RCL
            var rnd = RandomManager.getRandom();
            Candidate chosenCandidate = rcl.get(rnd.nextInt(rcl.size()));
            int bestSet = chosenCandidate.id();
            
            // --- GRASP LOGIC ENDS HERE ---

            // Add the chosen set and update coverage
            solution.addSet(bestSet);
            currentlyCovered.or(instance.getCoveredPoints(bestSet));
        }
    }

    /**
     * Removes redundant sets from the solution. This is a fast and effective local search.
     * This logic is identical to the one in PSSCGreedyConstructive.
     */
    private void redundancyElimination(PSSCSolution solution) {
        var instance = solution.getInstance();
        int[] coverCount = solution.getCoverCount();

        // Shuffle sets to avoid bias in removal order
        List<Integer> setsToCheck = new ArrayList<>();
        for(int set : solution.getChosenSets()){
            setsToCheck.add(set);
        }
        CollectionUtil.shuffle(setsToCheck);

        for (int set : setsToCheck) {
            boolean isRedundant = true;
            for (int point : instance.getCoveredPoints(set)) {
                if (coverCount[point] <= 1) {
                    isRedundant = false;
                    break;
                }
            }

            if (isRedundant) {
                solution.removeSet(set);
            }
        }
    }

    /**
     * A simple record to hold a candidate set and its score during construction.
     */
    private record Candidate(int id, int score) {}
}