package es.urjc.etsii.grafo.PSSC.experiments;

import es.urjc.etsii.grafo.PSSC.Main;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCRandomConstructive;
import es.urjc.etsii.grafo.PSSC.constructives.grasp.PSSCListManager;
import es.urjc.etsii.grafo.PSSC.model.PSSCBaseMove;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.algorithms.SimpleAlgorithm;
import es.urjc.etsii.grafo.create.grasp.GRASPListManager;
import es.urjc.etsii.grafo.create.grasp.GraspBuilder;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;

import java.util.ArrayList;
import java.util.List;

public class ConstructiveExperiment extends AbstractExperiment<PSSCSolution, PSSCInstance> {


    @Override
    public List<Algorithm<PSSCSolution, PSSCInstance>> getAlgorithms() {
        // In this experiment we will compare a random constructive with several GRASP constructive configurations
        var algorithms = new ArrayList<Algorithm<PSSCSolution, PSSCInstance>>();

        // Add random constructive to list of algorithms to test
        // SimpleAlgorithm executes the given constructive and the (optional) local search methods once.
        algorithms.add(new SimpleAlgorithm<>("Random", new PSSCRandomConstructive()));

        /* Disable GRASP constructive methods until list manager is implemented


        // Add GRASP constructive methods to experiment
        var graspListManager = new PSSCListManager();
        double[] alphaValues = {0d, 0.25d, 0.5d, 0.75d, 1d};
        // if the alpha parameter is not given --> random alpha in range [0,1] for each construction
        var graspBuilder = new GraspBuilder<PSSCBaseMove, PSSCSolution, PSSCInstance>()
                .withObjective(Main.OBJECTIVE)   // If using the default objective this line can be removed, but you can configure any objective or secondary function you want here, for example in case of a flat landscape you may want to use a custom greedy function
                .withListManager(graspListManager);

        // Create variants using greedy random strategy
        graspBuilder.withStrategyGreedyRandom();
        algorithms.add(new SimpleAlgorithm<>("GR-Random", graspBuilder.withAlphaRandom().build()));
        for (double alpha : alphaValues) {
            algorithms.add(new SimpleAlgorithm<>("GR-"+alpha, graspBuilder.withAlphaValue(alpha).build()));
        }

        // Create variants using random greedy strategy
        graspBuilder.withStrategyRandomGreedy();
        algorithms.add(new SimpleAlgorithm<>("RG-Random", graspBuilder.withAlphaRandom().build()));
        for (double alpha : alphaValues) {
            algorithms.add(new SimpleAlgorithm<>("RG-"+alpha, graspBuilder.withAlphaValue(alpha).build()));
        }

        */

        return algorithms;
    }
}
