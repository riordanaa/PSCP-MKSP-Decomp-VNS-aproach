package es.urjc.etsii.grafo.PSSC.algorithms;

import es.urjc.etsii.grafo.PSSC.constructives.PSSCGreedyConstructive;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.PSSC.model.neigh.PSSCBaseMove;
import es.urjc.etsii.grafo.PSSC.model.neigh.Swap11Neighborhood;
import es.urjc.etsii.grafo.PSSC.model.neigh.Swap22Neighborhood;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.util.CollectionUtil;
import es.urjc.etsii.grafo.util.TimeControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implements a powerful Variable Neighborhood Search for the Partial Set Covering Problem (VNS-PSCP).
 *
 * This algorithm transforms the PSCP into a series of Maximum Set k-Covering Problems (MSKCP).
 * - The Outer Loop attempts to find a feasible solution for a given solution size 'k', then decrements 'k' and repeats.
 * - The Inner Loop uses a VNS to solve the MSKCP for the current 'k', aiming to maximize coverage.
 */
public class VnsPscp extends Algorithm<PSSCSolution, PSSCInstance> {

    private static final Logger log = LoggerFactory.getLogger(VnsPscp.class);

    private final int lMax;

    /**
     * Constructor for the VNS-PSCP algorithm.
     * @param algorithmName A unique name for this algorithm instance.
     * @param lMax The maximum shake strength as a percentage of the solution size k.
     */
    public VnsPscp(String algorithmName, int lMax) {
        super(algorithmName);
        this.lMax = lMax;
    }

    @Override
    public PSSCSolution algorithm(PSSCInstance instance) {
        // 1. INITIALIZATION: Find an initial upper bound for k
        PSSCSolution initialFeasibleSolution = new PSSCGreedyConstructive().construct(this.newSolution(instance));
        if (!initialFeasibleSolution.isCovered()) {
            log.warn("Initial greedy constructive failed to find a feasible solution for instance {}.", instance.getId());
            return initialFeasibleSolution;
        }

        PSSCSolution bestSolutionEver = initialFeasibleSolution;
        int k = bestSolutionEver.getChosenSets().size();
        log.info("Initial solution found with k={}. Starting k-thinning process.", k);

        // 2. OUTER LOOP: Decrease k and solve the MSKCP for each k
        while (k >= 0 && !TimeControl.isTimeUp()) {
            log.info("Searching for a feasible solution of size k={}", k);

            PSSCSolution mskcpInitialSolution = this.newSolution(instance);
            PSSCGreedyConstructive.addNBestGreedySets(mskcpInitialSolution, k);
            mskcpInitialSolution.notifyUpdate();

            PSSCSolution bestSolutionForK = runVNS_for_fixed_k(mskcpInitialSolution);

            if (bestSolutionForK.isCovered()) {
                log.info("Success! Found a feasible solution for k={}. Coverage: {}", k, bestSolutionForK.coveredPoints().size());
                bestSolutionEver = bestSolutionForK;
                k--;
            } else {
                log.info("Failed to find a feasible solution for k={}. Max coverage found: {}. Stopping search.", k, bestSolutionForK.coveredPoints().size());
                break;
            }
        }

        log.info("VNS-PSCP finished. Best solution found with score: {}", bestSolutionEver.getScore());
        return bestSolutionEver;
    }

    /**
     * The inner VNS loop. For a fixed solution size k, it tries to maximize coverage.
     */
    private PSSCSolution runVNS_for_fixed_k(PSSCSolution initialSolution) {
        PSSCSolution currentBestForK = runLocalSearch_VND(initialSolution);
        int k = initialSolution.getChosenSets().size();
        int l_max_k = Math.max(1, (k * lMax) / 100);
        int l = 1;

        while (l <= l_max_k && !TimeControl.isTimeUp()) {
            PSSCSolution shakenSolution = shake(currentBestForK.cloneSolution(), l);
            PSSCSolution improvedSolution = runLocalSearch_VND(shakenSolution);

            if (improvedSolution.coveredPoints().size() > currentBestForK.coveredPoints().size()) {
                currentBestForK = improvedSolution;
                l = 1;
            } else {
                l++;
            }
        }
        return currentBestForK;
    }

    /**
     * A responsive Variable Neighborhood Descent (VND) local search.
     * It prioritizes fast (1,1)-swaps and only attempts the expensive (2,2)-swaps
     * when no simpler improvements can be found.
     */
    private PSSCSolution runLocalSearch_VND(PSSCSolution solution) {
        // Instantiate neighborhoods here. This is clean and thread-safe.
        var swap11 = new Swap11Neighborhood(50);
        var swap22 = new Swap22Neighborhood(25); // Use a smaller candidate list for the expensive neighborhood

        while (!TimeControl.isTimeUp()) {
            // Phase 1: Try the fast (1,1)-swap with a "best improvement" strategy.
            Optional<? extends PSSCBaseMove> best11Move = findBestMaximizingMove(swap11.explore(solution));

            if (best11Move.isPresent() && best11Move.get().getScoreChange() > 0) {
                solution = best11Move.get().execute(solution);
                continue; // Improvement found, restart the VND to prioritize 1-1 swaps again.
            }

            // Phase 2: If no 1-1 swaps worked, try the expensive (2,2)-swap with a "first improvement" strategy.
            Optional<Swap22Neighborhood.Swap22Move> first22Move = swap22.findFirstImprovingMove(solution);
            
            if (first22Move.isPresent()) {
                // The move is guaranteed to be improving because the method checks for it.
                solution = first22Move.get().execute(solution);
                continue; // Improvement found, restart the VND.
            }

            // If we reach here, neither neighborhood found an improvement. The solution is a local optimum.
            break;
        }
        return solution;
    }

    /**
     * Helper to find the best move from an ExploreResult, maximizing the score change.
     */
    private Optional<? extends PSSCBaseMove> findBestMaximizingMove(ExploreResult<? extends PSSCBaseMove, ?, ?> result) {
        return result.moves().max(Comparator.comparing(PSSCBaseMove::getScoreChange));
    }

    /**
     * Shake via Guided Destroy/Repair. Removes 'l' least useful sets and greedily adds 'l' new ones.
     */
    private PSSCSolution shake(PSSCSolution solution, int l) {
        int k = solution.getChosenSets().size();
        if (k == 0 || l == 0) return solution;

        // GUIDED DESTROY
        List<Candidate> setsToScore = new ArrayList<>();
        int[] coverCount = solution.getCoverCount();

        for (int set : solution.getChosenSets()) {
            int redundancyScore = 0;
            for (int point : solution.getInstance().getCoveredPoints(set)) {
                redundancyScore += coverCount[point];
            }
            setsToScore.add(new Candidate(set, redundancyScore));
        }
        setsToScore.sort(Comparator.comparingInt(Candidate::score).reversed());

        int nToRemove = Math.min(l, setsToScore.size());
        for (int i = 0; i < nToRemove; i++) {
            solution.removeSet(setsToScore.get(i).id());
        }

        // GREEDY REPAIR
        PSSCGreedyConstructive.addNBestGreedySets(solution, nToRemove);

        solution.notifyUpdate();
        return solution;
    }

    private record Candidate(int id, int score) {}
}