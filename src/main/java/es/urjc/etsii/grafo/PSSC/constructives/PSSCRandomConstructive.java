package es.urjc.etsii.grafo.PSSC.constructives;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.random.RandomManager;

public class PSSCRandomConstructive extends Constructive<PSSCSolution, PSSCInstance> {

    @Override
    public PSSCSolution construct(PSSCSolution solution) {
        var rnd = RandomManager.getRandom();
        int nSets = solution.getInstance().getnSets();
        while (!solution.isCovered()) {
            int set = rnd.nextInt(nSets);
            solution.addSet(set);
        }
        solution.notifyUpdate();
        return solution;
    }
}
