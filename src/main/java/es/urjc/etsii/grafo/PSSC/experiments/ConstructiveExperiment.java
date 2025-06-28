package es.urjc.etsii.grafo.PSSC.experiments;

import es.urjc.etsii.grafo.PSSC.algorithms.PSSCVariableNeighborhoodDescent;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.PSSC.model.neigh.DropNeighborhood;
import es.urjc.etsii.grafo.PSSC.model.neigh.SwapAndDropNeighborhood;
import es.urjc.etsii.grafo.PSSC.shake.ShakeDestroyAndRepair;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.algorithms.SimpleAlgorithm;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;
import es.urjc.etsii.grafo.improve.Improver;
import es.urjc.etsii.grafo.improve.ls.LocalSearchBestImprovement;

import java.util.ArrayList;
import java.util.List;

/**
 * Experiment that benchmarks different algorithms among them.
 * This file assembles the components into runnable algorithms.
 */
public class ConstructiveExperiment extends AbstractExperiment<PSSCSolution, PSSCInstance> {

    /**
     * Defines the list of algorithms to be executed in this experiment.
     *
     * @return A list containing the configured algorithms.
     */
    @Override
    public List<Algorithm<PSSCSolution, PSSCInstance>> getAlgorithms() {

        var algorithms = new ArrayList<Algorithm<PSSCSolution, PSSCInstance>>();

        // ALGORITHM 1: A fast, greedy constructive algorithm to serve as a baseline.
        algorithms.add(new SimpleAlgorithm<>("Greedy", new PSSCGreedyConstructive()));

        // --- ASSEMBLE OUR VNS ALGORITHM (ALGORITHM 2) ---

        // 1. Define the constructive heuristic for the VNS to start with.
        var constructive = new PSSCGreedyConstructive();

        // 2. Define the improvers for our manual VND loop.
        var dropImprover = new LocalSearchBestImprovement<>(new DropNeighborhood());
        
        // Instantiate the new, OPTIMIZED SwapAndDrop neighborhood.
        // 50 is a good starting candidate list size to test.
        var swapAndDropImprover = new LocalSearchBestImprovement<>(new SwapAndDropNeighborhood(50));

        // 3. Instantiate our "Destroy and Repair" Shake component.
        var shake = new ShakeDestroyAndRepair();

        // 4. Define the maximum shake strength.
        int kMax = 20;

        // 5. Create an instance of our custom VNS algorithm.
        var myVNS = new PSSCVariableNeighborhoodDescent(
                "MyVNS_OptimizedSwapDrop", // New name to reflect the change
                constructive,
                dropImprover,
                swapAndDropImprover, // Pass the new, optimized improver
                shake,
                kMax
        );

        // 6. Add the fully assembled VNS to the experiment list.
        algorithms.add(myVNS);

        return algorithms;
    }
}