package es.urjc.etsii.grafo.PSSC.experiments;

import es.urjc.etsii.grafo.PSSC.constructives.PSSCRandomConstructive;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.algorithms.SimpleAlgorithm;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;

import java.util.ArrayList;
import java.util.List;

/**
 * Experiment that benchmarks different algorithms among them
 */
public class ConstructiveExperiment
        extends AbstractExperiment<PSSCSolution, PSSCInstance> {

    @Override
    public List<Algorithm<PSSCSolution, PSSCInstance>> getAlgorithms() {

        var algorithms = new ArrayList<Algorithm<PSSCSolution, PSSCInstance>>();

        algorithms.add(new SimpleAlgorithm<>("Random", new PSSCRandomConstructive()));

        // any neighborhood can be used with the local searches
        // example:
        // Improver<PSSCSolution, PSSCInstance> firstImprovementLS = new LocalSearchFirstImprovement<>(new SwapNeighborhood());
        // or using best improvement:
        // Improver<PSSCSolution, PSSCInstance> bestImprovementLS = new LocalSearchBestImprovement<>(new SwapNeighborhood());

        // VNS algorithms can be created using the VNSBuilder class
        // TODO add more algorithms to the experiment
        return algorithms;
    }
}
