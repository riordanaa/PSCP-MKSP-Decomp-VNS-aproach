package es.urjc.etsii.grafo.PSSC.constructives;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.CollectionUtil;
import es.urjc.etsii.grafo.util.collections.BitSet;

import java.util.ArrayList;
import java.util.List;

/**
 * An efficient greedy constructive for the PSCP.
 * This class now also provides public static helper methods for greedy logic
 * that can be used by other algorithms like VNS-PSCP.
 */
public class PSSCGreedyConstructive extends Constructive<PSSCSolution, PSSCInstance> {

    /**
     * This is the method called when PSSCGreedyConstructive is used as a MORK component.
     * It builds a feasible solution and then improves it by removing redundant sets.
     */
    @Override
    public PSSCSolution construct(PSSCSolution solution) {
        // Phase 1: Greedily build until feasible
        buildUntilFeasible(solution);
        // Phase 2: Clean up any redundant sets
        redundancyElimination(solution);
        
        // Notify MORK that the solution has been created and is ready
        solution.notifyUpdate();
        return solution;
    }

    /**
     * Helper method to greedily add sets until the solution is feasible.
     * @param solution The solution to modify.
     */
    public static void buildUntilFeasible(PSSCSolution solution) {
        while (!solution.isCovered()) {
            boolean improved = addNBestGreedySets(solution, 1);
            if (!improved) {
                // If we cannot add any more sets but are still not feasible, break to avoid infinite loop.
                break;
            }
        }
    }

    /**
     * Public static helper to greedily add a specific number of the best sets to a solution.
     * This is the core reusable logic.
     * @param solution The solution to add sets to.
     * @param n The number of sets to add.
     * @return true if at least one set was successfully added, false otherwise.
     */
    public static boolean addNBestGreedySets(PSSCSolution solution, int n) {
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            int bestSetToAdd = -1;
            int maxNewCovered = -1;
            BitSet currentlyCovered = solution.coveredPoints();

            for (int s = 0; s < solution.getInstance().getnSets(); s++) {
                if (!solution.getChosenSets().contains(s) && !solution.getInstance().getUselessSets().contains(s)) {
                    BitSet contribution = solution.getInstance().getCoveredPoints(s).clone();
                    contribution.andNot(currentlyCovered);
                    int newCovered = contribution.size();
                    if (newCovered > maxNewCovered) {
                        maxNewCovered = newCovered;
                        bestSetToAdd = s;
                    }
                }
            }

            if (bestSetToAdd != -1) {
                solution.addSet(bestSetToAdd);
                changed = true;
            } else {
                // No more sets can be added
                break;
            }
        }
        return changed;
    }


    /**
     * Removes redundant sets from a feasible solution.
     * @param solution The solution to clean up.
     */
    private void redundancyElimination(PSSCSolution solution) {
        var instance = solution.getInstance();
        int[] coverCount = solution.getCoverCount();

        List<Integer> setsToCheck = new ArrayList<>();
        for (int set : solution.getChosenSets()) {
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
}