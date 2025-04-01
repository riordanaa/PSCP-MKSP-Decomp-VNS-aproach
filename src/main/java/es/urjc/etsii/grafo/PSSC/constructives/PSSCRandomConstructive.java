package es.urjc.etsii.grafo.PSSC.constructives;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.create.Constructive;

public class PSSCRandomConstructive extends Constructive<PSSCSolution, PSSCInstance> {

    @Override
    public PSSCSolution construct(PSSCSolution solution) {
        // IN --> Empty solution from solution(instance) constructor
        // OUT --> Feasible solution with an assigned score
        // TODO: Implement random constructive


        // Remember to call solution.notifyUpdate() if the solution is modified without using moves!!
        solution.notifyUpdate();
        //return solution;
        throw new UnsupportedOperationException("RandomConstructive not implemented yet");
    }
}
