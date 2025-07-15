package es.urjc.etsii.grafo.PSSC.experiments;

import es.urjc.etsii.grafo.PSSC.algorithms.PSSCVariableNeighborhoodDescent;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCGRASPConstructive;
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
     * Each algorithm in the returned list will be run against the specified instances.
     *
     * @return A list containing the configured algorithms.
     */
    @Override
    public List<Algorithm<PSSCSolution, PSSCInstance>> getAlgorithms() {

        var algorithms = new ArrayList<Algorithm<PSSCSolution, PSSCInstance>>();

        // ALGORITHM 1: A fast, greedy constructive algorithm to serve as a baseline.
        algorithms.add(new SimpleAlgorithm<>("Greedy", new PSSCGreedyConstructive()));

        // ALGORITHM 2: The GRASP constructive run by itself.
        // This allows us to see the quality of the VNS starting points before the main loop begins.
        // We use an RCL size of 3, which is a common choice.
        algorithms.add(new SimpleAlgorithm<>("GRASP", new PSSCGRASPConstructive(3)));

        // --- ASSEMBLE OUR VNS ALGORITHM (ALGORITHM 3) ---

        // 1. Define the constructive heuristic for the VNS to start with.
        //    We use the same GRASP constructive.
        var graspConstructive = new PSSCGRASPConstructive(3);

        // 2. Define the improvers for our manual VND loop.
        var dropImprover = new LocalSearchBestImprovement<>(new DropNeighborhood());
        
        // Instantiate the optimized SwapAndDrop neighborhood with a candidate list size of 50.
        // This is a parameter that can be tuned.
        var swapAndDropImprover = new LocalSearchBestImprovement<>(new SwapAndDropNeighborhood(50));

        // 3. Instantiate our "Destroy and Repair" Shake component.
        var shake = new ShakeDestroyAndRepair();

        // 4. Define the maximum shake strength. This is another key parameter for tuning.
        int kMax = 20;

        // 5. Create an instance of our custom VNS algorithm.
        var myVNS = new PSSCVariableNeighborhoodDescent(
                "MyVNS_GRASP", // A descriptive name for the results table
                graspConstructive,
                dropImprover,
                swapAndDropImprover,
                shake,
                kMax
        );

        // 6. Add the fully assembled VNS to the experiment list.
        algorithms.add(myVNS);

        return algorithms;
    }
}