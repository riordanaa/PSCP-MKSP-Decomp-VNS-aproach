package es.urjc.etsii.grafo.PSSC.shake;

import es.urjc.etsii.grafo.PSSC.model.*;
import es.urjc.etsii.grafo.PSSC.model.neigh.PSSCBaseMove;
import es.urjc.etsii.grafo.shake.Shake;
import es.urjc.etsii.grafo.util.random.RandomManager;

/**
 * Shake: add a random unselected set, then drop any newly redundant sets.
 */
public class ExamplePSSCShake extends Shake<PSSCSolution, PSSCInstance> {


    @Override
    public PSSCSolution shake(PSSCSolution solution, int k) {

        var rnd = RandomManager.getRandom();
        PSSCInstance ins = solution.getInstance();

        // Add k random unselected sets
        for (int i = 0; i < k; i++) {
            int tries = 0;
            int sel;
            do {
                sel = rnd.nextInt(ins.getnSets());
            } while (solution.getChosenSets().contains(sel) && ++tries < 10);

            if (!solution.getChosenSets().contains(sel)) {
                solution.addSet(sel);
            }
        }

        return solution;
    }
}
