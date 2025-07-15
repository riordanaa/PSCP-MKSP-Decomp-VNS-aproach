package es.urjc.etsii.grafo.PSSC.experiments;

import es.urjc.etsii.grafo.PSSC.algorithms.VnsPscp;
import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.algorithms.SimpleAlgorithm;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;

import java.util.ArrayList;
import java.util.List;

/**
 * Experiment that benchmarks different algorithms among them.
 * This file assembles the components into runnable algorithms.
 *
 * This configuration is set to run and compare:
 * 1. A fast Greedy constructive heuristic.
 * 2. The advanced VNS-PSCP algorithm.
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


        // --- ASSEMBLE OUR NEW VNS-PSCP ALGORITHM (ALGORITHM 2) ---
        // This VNS solves a series of MSKCPs (maximizing coverage).

        // Maximum shake strength as a percentage of the current solution size k.
        // A value of 20 means the shake can remove and repair up to 20% of the sets.
        int lMaxPercentage = 20;

        var vnsPscp = new VnsPscp(
                "VNS-PSCP", // Algorithm name for the results table
                lMaxPercentage
        );
        algorithms.add(vnsPscp);


        return algorithms;
    }
}