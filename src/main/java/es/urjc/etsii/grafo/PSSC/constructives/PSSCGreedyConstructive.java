package es.urjc.etsii.grafo.PSSC.constructives;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.collections.BitSet;
// --> FIX 1: Correct the import to use 'CollectionUtil' (singular).
import es.urjc.etsii.grafo.util.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * An efficient two-phase greedy constructive for the Partial Set Covering Problem (PSCP).
 */
public class PSSCGreedyConstructive extends Constructive<PSSCSolution, PSSCInstance> {

    @Override
    public PSSCSolution construct(PSSCSolution solution) {
        greedyConstruction(solution);
        redundancyElimination(solution);
        solution.notifyUpdate();
        return solution;
    }

    private void greedyConstruction(PSSCSolution solution) {
        var instance = solution.getInstance();
        BitSet currentlyCovered = solution.coveredPoints();

        while (currentlyCovered.size() < solution.minCoveredRequired()) {
            int bestSet = -1;
            int maxNewCovered = -1;

            for (int s = 0; s < instance.getnSets(); s++) {
                if (solution.getChosenSets().contains(s) || instance.getUselessSets().contains(s)) {
                    continue;
                }

                BitSet contribution = instance.getCoveredPoints(s).clone();
                contribution.andNot(currentlyCovered);
                int newCoveredCount = contribution.size();

                if (newCoveredCount > maxNewCovered) {
                    maxNewCovered = newCoveredCount;
                    bestSet = s;
                }
            }

            if (bestSet == -1 || maxNewCovered == 0) {
                break;
            }

            solution.addSet(bestSet);
            currentlyCovered.or(instance.getCoveredPoints(bestSet));
        }
    }

    private void redundancyElimination(PSSCSolution solution) {
        var instance = solution.getInstance();
        int[] coverCount = solution.getCoverCount();

        List<Integer> setsToCheck = new ArrayList<>();
        for(int set : solution.getChosenSets()){
            setsToCheck.add(set);
        }

        // --> FIX 2: Correct the method call to use 'CollectionUtil' (singular).
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