package es.urjc.etsii.grafo.PSSC.algorithms;

import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.model.*;
import es.urjc.etsii.grafo.PSSC.model.neigh.*;
import es.urjc.etsii.grafo.PSSC.shake.ExamplePSSCShake;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.improve.Improver;
import es.urjc.etsii.grafo.shake.Shake;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;

import java.util.List;
import java.util.Optional;

/**
 * Example of a custom algorithm that executes a constructive followed by
 * a sequence of improve -> shake --> improve
 * This algorithm can be used instead of one provided by the framework, such as VNS.
 */
public class ExampleAlgorithm extends Algorithm<PSSCSolution, PSSCInstance> {

    final Constructive<PSSCSolution, PSSCInstance> constructive;
    final Improver<PSSCSolution, PSSCInstance> improver;
    final Shake<PSSCSolution, PSSCInstance> shake;

    public ExampleAlgorithm(String name,
                            Constructive<PSSCSolution, PSSCInstance> constructive,
                            Improver<PSSCSolution, PSSCInstance> improver,
                            Shake<PSSCSolution, PSSCInstance> shake) {
        super(name);
        this.constructive = constructive;
        this.improver = improver;
        this.shake = shake;
    }

    @Override
    public PSSCSolution algorithm(PSSCInstance instance) {

        PSSCSolution solution = new PSSCSolution(instance);
        solution = constructive.construct(solution);
        solution = improver.improve(solution);

        // copy in case the solution gets worse after shaking-improving
        PSSCSolution bestSolutionFound = solution.cloneSolution();
        for (int i = 0; i < 10; i++) {
            // Shake the solution
            solution = shake.shake(solution, 1);

            // Improve the shaken solution
            solution = improver.improve(solution);

            // If the shaken-improved solution is better than the best found, update it
            if (solution.getScore() < bestSolutionFound.getScore()) {
                bestSolutionFound = solution.cloneSolution();
            } else {
                // If the shaken-improved is bad, retry from the best found solution
                // and drop the current solution
                solution = bestSolutionFound.cloneSolution();
            }
        }

        return bestSolutionFound;
    }
}
