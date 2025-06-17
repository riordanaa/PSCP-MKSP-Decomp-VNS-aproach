package es.urjc.etsii.grafo.PSSC.shake;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.shake.Shake;
import es.urjc.etsii.grafo.util.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A Shake component that perturbs a solution by adding 'k' random, unselected sets.
 * This is used in the VNS to escape local optima by moving to a different region
 * of the search space (specifically, to a solution with more sets). The subsequent
 * local search (VND) will then attempt to trim down this "bloated" solution into
 * a new, hopefully better, local optimum.
 */
public class PSSCAddShake extends Shake<PSSCSolution, PSSCInstance> {

    /**
     * Shakes the solution by adding k random sets. This implementation is optimized to be
     * efficient even for large k values.
     *
     * @param solution The solution to perturb.
     * @param k        The number of random sets to add (the "shake strength").
     * @return The perturbed solution.
     */
    @Override
    public PSSCSolution shake(PSSCSolution solution, int k) {
        if (k <= 0) {
            return solution; // No shake to perform
        }

        var instance = solution.getInstance();
        var chosenSets = solution.getChosenSets();
        var uselessSets = instance.getUselessSets();

        // 1. Build a list of candidate sets that can be added.
        // A set is a candidate if it's not already in the solution and not useless.
        List<Integer> candidateSets = new ArrayList<>();
        for (int s = 0; s < instance.getnSets(); s++) {
            if (!chosenSets.contains(s) && !uselessSets.contains(s)) {
                candidateSets.add(s);
            }
        }

        // If there are no sets to add, return immediately.
        if (candidateSets.isEmpty()) {
            return solution;
        }

        // 2. Efficiently pick k unique random sets.
        // Instead of picking and removing one-by-one, we shuffle the whole list once
        // and take the first k elements. This is much more performant.
        // We use MORK's CollectionUtil to ensure reproducible randomness.
        CollectionUtil.shuffle(candidateSets);

        // 3. Add 'k' unique random sets from the shuffled candidates.
        // We take the minimum of k and the candidate list size to avoid errors
        // if k is larger than the number of available sets.
        int setsToAdd = Math.min(k, candidateSets.size());
        for (int i = 0; i < setsToAdd; i++) {
            int setToAdd = candidateSets.get(i);
            solution.addSet(setToAdd);
        }

        return solution;
    }
}