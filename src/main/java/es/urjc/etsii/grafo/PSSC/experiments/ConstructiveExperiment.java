package es.urjc.etsii.grafo.PSSC.experiments;

// --- ADD THIS ENTIRE BLOCK ---
import es.urjc.etsii.grafo.PSSC.algorithms.PSSCVariableNeighborhoodDescent;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCRandomConstructive;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.PSSC.model.neigh.DropNeighborhood;
import es.urjc.etsii.grafo.PSSC.model.neigh.SwapNeighborhood;
import es.urjc.etsii.grafo.PSSC.shake.PSSCAddShake;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.algorithms.SimpleAlgorithm;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;
import es.urjc.etsii.grafo.improve.Improver;
import es.urjc.etsii.grafo.improve.ls.LocalSearchBestImprovement;

import java.util.ArrayList;
import java.util.List;


public class ConstructiveExperiment extends AbstractExperiment<PSSCSolution, PSSCInstance> {

    @Override
    public List<Algorithm<PSSCSolution, PSSCInstance>> getAlgorithms() {
        var algorithms = new ArrayList<Algorithm<PSSCSolution, PSSCInstance>>();
        algorithms.add(new SimpleAlgorithm<>("Random", new PSSCRandomConstructive()));

        // --- ASSEMBLE OUR VNS ALGORITHM ---
        var constructive = new PSSCGreedyConstructive();
        algorithms.add(new SimpleAlgorithm<>("Greedy", constructive));
        Improver<PSSCSolution, PSSCInstance> dropImprover = new LocalSearchBestImprovement<>(new DropNeighborhood());
        Improver<PSSCSolution, PSSCInstance> swapImprover = new LocalSearchBestImprovement<>(new SwapNeighborhood());
        var shake = new PSSCAddShake();

        // Define the maximum shake strength for our VNS
        int kMax = 20; // Example value, can be tuned

        var myVNS = new PSSCVariableNeighborhoodDescent(
                "MyVNS_with_Shake",
                constructive,
                dropImprover,
                swapImprover,
                shake,
                kMax // Pass kMax to the constructor
        );

        algorithms.add(myVNS);
        return algorithms;
    }
}