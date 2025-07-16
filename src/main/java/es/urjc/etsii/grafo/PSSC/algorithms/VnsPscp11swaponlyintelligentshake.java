package es.urjc.etsii.grafo.PSSC.algorithms;

import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.PSSC.model.neigh.Swap11Neighborhood;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.util.TimeControl;
import es.urjc.etsii.grafo.util.collections.BitSet;
import es.urjc.etsii.grafo.util.random.RandomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class VnsPscp11swaponlyintelligentshake extends Algorithm<PSSCSolution, PSSCInstance> {

    private static final Logger log = LoggerFactory.getLogger(VnsPscp11swaponlyintelligentshake.class);

    private final int lMax;
    private final Swap11Neighborhood ls1;

    public VnsPscp11swaponlyintelligentshake(String algorithmName, int lMax) {
        super(algorithmName);
        this.lMax = lMax;
        this.ls1 = new Swap11Neighborhood();
    }

    @Override
    public PSSCSolution algorithm(PSSCInstance instance) {
        PSSCSolution initialFeasibleSolution = new PSSCGreedyConstructive().construct(this.newSolution(instance));
        if (!initialFeasibleSolution.isCovered()) {
            log.warn("Initial greedy constructive failed to find a feasible solution for instance {}.", instance.getId());
            return initialFeasibleSolution;
        }

        PSSCSolution bestSolutionEver = initialFeasibleSolution;
        int k = bestSolutionEver.getChosenSets().size();
        log.info("Initial solution found with k={}. Starting k-thinning process.", k);

        // OUTER LOOP
        while (k >= 0 && !TimeControl.isTimeUp()) {
            if (k < bestSolutionEver.getChosenSets().size() - 10 && k > 0) {
                log.warn("K ({}) has dropped significantly below best solution size ({}), terminating early.", k, bestSolutionEver.getChosenSets().size());
                break;
            }

            log.info("Searching for a feasible solution of size k={}", k);

            PSSCSolution mskcpInitialSolution = this.newSolution(instance);
            PSSCGreedyConstructive.addNBestGreedySets(mskcpInitialSolution, k);
            mskcpInitialSolution.notifyUpdate();

            PSSCSolution bestSolutionForK = runVNS_for_fixed_k(mskcpInitialSolution);

            if (bestSolutionForK.isCovered()) {
                log.info("Success! Found a feasible solution for k={}. Coverage: {}", k, bestSolutionForK.coveredPoints().size());
                bestSolutionEver = bestSolutionForK;
                k--;
            } else {
                log.info("Failed to find a feasible solution for k={}. Max coverage found: {}. Stopping search.", k, bestSolutionForK.coveredPoints().size());
                break;
            }
        }

        log.info("VNS-PSCP finished. Best solution found with score: {}", bestSolutionEver.getScore());
        return bestSolutionEver;
    }

    private PSSCSolution runVNS_for_fixed_k(PSSCSolution initialSolution) {
        PSSCSolution currentBestForK = runLocalSearch_VND(initialSolution);
        int k = initialSolution.getChosenSets().size();
        int l_max_k = Math.max(1, (k * lMax) / 100);
        int l = 1;

        while (l <= l_max_k && !TimeControl.isTimeUp()) {
            PSSCSolution shakenSolution = shake(currentBestForK.cloneSolution(), l);
            PSSCSolution improvedSolution = runLocalSearch_VND(shakenSolution);
            if (improvedSolution.coveredPoints().size() > currentBestForK.coveredPoints().size()) {
                currentBestForK = improvedSolution;
                l = 1;
            } else {
                l++;
            }
        }
        return currentBestForK;
    }

    /**
     * The Variable Neighborhood Descent, using only the fast (1,1)-swap with a First Improvement strategy.
     */
    private PSSCSolution runLocalSearch_VND(PSSCSolution solution) {
        boolean improvementFound;
        do {
            improvementFound = false;
            if (TimeControl.isTimeUp()) {
                break;
            }

            Optional<Swap11Neighborhood.Swap11Move> move11 = ls1.findFirstImprovingMove(solution);
            if (move11.isPresent()) {
                solution = move11.get().execute(solution);
                improvementFound = true;
            }

        } while (improvementFound);

        return solution;
    }

    /**
     * A more intelligent Shake via Guided Destroy and GRASP-style Repair.
     * @param solution The solution to perturb.
     * @param l The shake strength.
     * @return The perturbed and repaired solution.
     */
    private PSSCSolution shake(PSSCSolution solution, int l) {
        int k = solution.getChosenSets().size();
        if (k == 0) return solution;

        // --- 1. GUIDED DESTROY PHASE ---
        // Calculate the "usefulness" (contribution) of each set in the solution.
        // A set's contribution is the number of points it covers uniquely.
        List<SetContribution> contributions = new ArrayList<>();
        int[] coverCount = solution.getCoverCount();
        for (int set : solution.getChosenSets()) {
            int uniquePoints = 0;
            for (int point : solution.getInstance().getCoveredPoints(set)) {
                if (coverCount[point] == 1) {
                    uniquePoints++;
                }
            }
            contributions.add(new SetContribution(set, uniquePoints));
        }

        // Sort the sets by their contribution, from LEAST useful to most useful.
        Collections.sort(contributions);

        // Remove the 'l' least useful sets.
        int nToRemove = Math.min(l, k);
        for (int i = 0; i < nToRemove; i++) {
            solution.removeSet(contributions.get(i).setId());
        }

        // --- 2. GRASP-STYLE REPAIR PHASE ---
        // Greedily add 'nToRemove' new sets, but with some randomness.
        var random = RandomManager.getRandom();
        final int rclSize = 3; // Restricted Candidate List size. A good parameter to tune.

        for (int i = 0; i < nToRemove; i++) {
            var currentlyCovered = solution.coveredPoints();

            // Find the best candidates to add.
            List<SetContribution> candidates = new ArrayList<>();
            for (int s = 0; s < solution.getInstance().getnSets(); s++) {
                if (!solution.getChosenSets().contains(s) && !solution.getInstance().getUselessSets().contains(s)) {
                    var contribution = solution.getInstance().getCoveredPoints(s).clone();
                    contribution.andNot(currentlyCovered);
                    candidates.add(new SetContribution(s, contribution.size()));
                }
            }
            
            if (candidates.isEmpty()) {
                break; // No more sets can be added
            }

            // Sort candidates from best to worst.
            candidates.sort(Collections.reverseOrder());

            // Select one of the top 'rclSize' candidates randomly.
            int stopIndex = Math.min(rclSize, candidates.size());
            int chosenIndex = random.nextInt(stopIndex);
            
            solution.addSet(candidates.get(chosenIndex).setId());
        }

        solution.notifyUpdate();
        return solution;
    }

    /**
     * Helper record to store a set and its contribution score for sorting.
     * Implements Comparable to allow sorting from lowest to highest contribution.
     */
    private record SetContribution(int setId, int score) implements Comparable<SetContribution> {
        @Override
        public int compareTo(SetContribution other) {
            return Integer.compare(this.score, other.score);
        }
    }
}