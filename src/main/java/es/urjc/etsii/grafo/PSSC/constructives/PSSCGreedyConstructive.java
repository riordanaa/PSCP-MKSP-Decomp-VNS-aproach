package es.urjc.etsii.grafo.PSSC.constructives;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.random.RandomManager;
import es.urjc.etsii.grafo.util.collections.BitSet;

/**
 * Greedy constructive for the Partial Set Covering Problem (PSCP).
 * TODO
 */
public class PSSCGreedyConstructive extends Constructive<PSSCSolution, PSSCInstance> {

    @Override
    public PSSCSolution construct(PSSCSolution solution) {
        var rnd = RandomManager.getRandom();

        // TODO, construct using a greedy heuristic

        return solution;
    }
}
