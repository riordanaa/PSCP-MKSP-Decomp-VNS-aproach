package es.urjc.etsii.grafo.PSSC.algorithms;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.improve.Improver;
import es.urjc.etsii.grafo.shake.Shake;
import es.urjc.etsii.grafo.util.TimeControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a Variable Neighborhood Search (VNS) algorithm for the Partial Set Covering Problem.
 * This algorithm combines a powerful local search (VND) with a perturbation (shake) mechanism
 * to escape local optima.
 *
 * The search strategy is as follows:
 * 1. Build an initial solution and find its local optimum using VND.
 * 2. Loop until termination:
 *    a. Perturbation (Shake): Apply a 'Shake' of strength 'k' to the current best solution.
 *    b. Intensification (VND): Run the full VND on the perturbed solution to find its local optimum.
 *    c. Neighborhood Change: If this new local optimum is better than the best-so-far, update the best
 *       and reset shake strength 'k' to 1. Otherwise, increment 'k'.
 *
 * The best solution found throughout the entire process is always maintained.
 */
public class PSSCVariableNeighborhoodDescent extends Algorithm<PSSCSolution, PSSCInstance> {

    private static final Logger log = LoggerFactory.getLogger(PSSCVariableNeighborhoodDescent.class);

    private final Constructive<PSSCSolution, PSSCInstance> constructive;
    private final Improver<PSSCSolution, PSSCInstance> dropImprover;
    private final Improver<PSSCSolution, PSSCInstance> swapImprover;
    private final Shake<PSSCSolution, PSSCInstance> shake;
    private final int kMax;

    /**
     * Constructor for the VNS algorithm.
     *
     * @param algorithmName A unique name for this algorithm instance.
     * @param constructive  The constructive heuristic to generate an initial solution.
     * @param dropImprover  An improver configured with the DropNeighborhood.
     * @param swapImprover  An improver configured with the SwapNeighborhood.
     * @param shake         The Shake component used for perturbation.
     * @param kMax          The maximum shake strength before stopping.
     */
    public PSSCVariableNeighborhoodDescent(
            String algorithmName,
            Constructive<PSSCSolution, PSSCInstance> constructive,
            Improver<PSSCSolution, PSSCInstance> dropImprover,
            Improver<PSSCSolution, PSSCInstance> swapImprover,
            Shake<PSSCSolution, PSSCInstance> shake,
            int kMax) {
        super(algorithmName);
        this.constructive = constructive;
        this.dropImprover = dropImprover;
        this.swapImprover = swapImprover;
        this.shake = shake;
        this.kMax = kMax;
    }

    /**
     * The core logic of the VNS algorithm.
     *
     * @param instance The problem instance to solve.
     * @return The best solution found.
     */
    @Override
    public PSSCSolution algorithm(PSSCInstance instance) {
        // 1. Generate an initial solution
        PSSCSolution currentSolution = this.newSolution(instance);
        currentSolution = constructive.construct(currentSolution);
        log.debug("Initial solution constructed with score: {}", currentSolution.getScore());

        // 2. Improve it to find the first local optimum and set it as the best
        currentSolution = localSearch(currentSolution);
        PSSCSolution bestSolution = currentSolution.cloneSolution();
        log.info("Initial best solution found with score: {}", bestSolution.getScore());

        // 3. Main VNS loop
        int k = 1;
        while (k <= kMax && !TimeControl.isTimeUp()) {
            // PERTURBATION: Shake from the BEST solution found so far
            log.debug("Shaking from score {} with k={}", bestSolution.getScore(), k);
            PSSCSolution shakenSolution = bestSolution.cloneSolution(); // Always shake from the best
            shakenSolution = this.shake.shake(shakenSolution, k);
            log.debug("Shaken solution has score: {}", shakenSolution.getScore());

            // INTENSIFICATION: Apply local search to the shaken solution
            PSSCSolution improvedFromShake = localSearch(shakenSolution);
            log.debug("After local search, score is: {}", improvedFromShake.getScore());

            // NEIGHBORHOOD CHANGE: Decide whether to accept the new solution
            if (improvedFromShake.getScore() < bestSolution.getScore()) {
                log.info("New best solution found with score: {}. Resetting k=1.", improvedFromShake.getScore());
                bestSolution = improvedFromShake.cloneSolution(); // Update best solution
                k = 1; // Improvement found, reset k to the first neighborhood
            } else {
                k++; // No improvement, advance to the next neighborhood (increase shake strength)
            }
        }

        log.info("VNS finished. Best solution found with score: {}", bestSolution.getScore());
        return bestSolution;
    }

    /**
     * Executes a Variable Neighborhood Descent (VND) to find a local optimum.
     * It iteratively applies Drop and Swap neighborhoods until no further improvement is possible.
     *
     * @param solution The solution to improve.
     * @return A locally optimal solution.
     */
    private PSSCSolution localSearch(PSSCSolution solution) {
        boolean improved;
        do {
            double scoreBefore = solution.getScore();

            // Try to improve with the first neighborhood (Drop)
            solution = dropImprover.improve(solution);

            // If Drop improved, the score changed. Restart the VND to try Drop again.
            if (solution.getScore() < scoreBefore) {
                improved = true;
                continue; // Go to the next iteration of the do-while loop
            }

            // If Drop did not improve, try the second neighborhood (Swap)
            solution = swapImprover.improve(solution);

            // Check if either Drop or Swap made an improvement in this full pass
            improved = solution.getScore() < scoreBefore;

        } while (improved && !TimeControl.isTimeUp()); // Loop as long as we find improvements

        return solution;
    }


    @Override
    public String toString() {
        return "PSSC_VNS{" +
                "constructive=" + constructive +
                ", drop=" + dropImprover +
                ", swap=" + swapImprover +
                ", shake=" + shake +
                ", kMax=" + kMax +
                '}';
    }
}