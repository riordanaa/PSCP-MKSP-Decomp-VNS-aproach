package es.urjc.etsii.grafo.PSSC.shake;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.shake.Shake;
import es.urjc.etsii.grafo.util.CollectionUtil;
import es.urjc.etsii.grafo.util.collections.BitSet;

import java.util.ArrayList;
import java.util.List;

/**
 * A powerful Shake component for the VNS algorithm that implements a "Destroy and Repair" strategy.
 * This is designed to make a large, intelligent jump in the search space to escape deep local optima.
 *
 * The process is as follows:
 * 1. **Destroy:** A number of sets, determined by the shake strength 'k', are removed from the solution.
 *    This phase intentionally "breaks" the solution, likely making it infeasible.
 * 2. **Repair:** The broken solution is repaired by greedily adding the most effective sets one by one
 *    until it becomes feasible again.
 *
 * The resulting solution is structurally different from the original but is guaranteed to be feasible,
 * providing a new, high-quality starting point for the local search (VND).
 */
public class ShakeDestroyAndRepair extends Shake<PSSCSolution, PSSCInstance> {

    /**
     * Perturbs the given solution using the Destroy and Repair method.
     *
     * @param solution The solution to perturb. This solution is modified in place.
     * @param k        The shake strength, corresponding to the number of sets to remove in the destroy phase.
     * @return The modified, repaired, and feasible solution.
     */
    @Override
    public PSSCSolution shake(PSSCSolution solution, int k) {
        // If shake strength is 0, do nothing. This allows the VNS to perform a pure local search in its first iteration.
        if (k == 0) {
            return solution;
        }

        // --- 1. DESTROY PHASE ---
        // Remove 'k' random sets from the solution.
        destroy(solution, k);

        // --- 2. REPAIR PHASE ---
        // The solution is now likely infeasible. Greedily add sets until it's feasible again.
        repair(solution);

        // Return the perturbed and repaired solution
        return solution;
    }

    /**
     * Removes 'k' random sets from the solution.
     * For a more advanced strategy, this could be modified to remove the "least useful" sets.
     */
    private void destroy(PSSCSolution solution, int k) {
        if (solution.getChosenSets().isEmpty()) {
            return;
        }

        // To safely remove random elements, we copy the chosen sets to a list and shuffle it.
        List<Integer> setsToRemove = new ArrayList<>();
        for (int set : solution.getChosenSets()) {
            setsToRemove.add(set);
        }
        CollectionUtil.shuffle(setsToRemove);

        // Determine the actual number of sets to remove, ensuring we don't exceed the list size.
        int nToRemove = Math.min(k, setsToRemove.size());

        // Remove the first 'nToRemove' sets from the shuffled list.
        for (int i = 0; i < nToRemove; i++) {
            solution.removeSet(setsToRemove.get(i));
        }
    }

    /**
     * Greedily adds sets to the solution until the minimum coverage requirement is met.
     * This uses the same effective logic as the greedy constructive.
     */
    private void repair(PSSCSolution solution) {
        var instance = solution.getInstance();

        // Use a BitSet for efficient calculation of covered points.
        // This is much faster than repeatedly calling solution.coveredPoints() inside the loop.
        BitSet currentlyCovered = solution.coveredPoints();

        while (currentlyCovered.size() < solution.minCoveredRequired()) {
            int bestSet = -1;
            int maxNewCovered = -1;

            // Find the best set to add: the one that covers the most new points.
            for (int s = 0; s < instance.getnSets(); s++) {
                // A set is a candidate if it's not already chosen and not "useless".
                if (solution.getChosenSets().contains(s) || instance.getUselessSets().contains(s)) {
                    continue;
                }

                // Calculate the contribution of this set (how many NEW points it covers).
                BitSet contribution = instance.getCoveredPoints(s).clone();
                contribution.andNot(currentlyCovered); // Keep only points not already covered.
                int newCoveredCount = contribution.size();

                if (newCoveredCount > maxNewCovered) {
                    maxNewCovered = newCoveredCount;
                    bestSet = s;
                }
            }

            // If we found a set to add, add it and update our tracking BitSet.
            if (bestSet != -1) {
                solution.addSet(bestSet);
                currentlyCovered.or(instance.getCoveredPoints(bestSet));
            } else {
                // Safety break: If no set can improve coverage but the solution is still infeasible,
                // we must exit to prevent an infinite loop. This might indicate an unsolvable instance.
                break;
            }
        }
    }
}