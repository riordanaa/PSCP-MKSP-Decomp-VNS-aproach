package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.Move;

/**
 * Base move class for all moves in the PSSC problem.
 * It now stores a reference to the solution at the moment of the move's creation,
 * which is essential for calculating score changes without re-passing the solution.
 */
public abstract class PSSCBaseMove extends Move<PSSCSolution, PSSCInstance> {

    // ADDED: Field to store the solution context.
    private final PSSCSolution solution;

    /**
     * Move constructor
     * @param solution The solution in which this move is being generated.
     */
    public PSSCBaseMove(PSSCSolution solution) {
        super(solution);
        // Store the solution in the new field
        this.solution = solution;
    }

    /**
     * Gets the solution as it was when the move was created.
     * @return The solution context for this move.
     */
    public PSSCSolution getSolution() {
        return this.solution;
    }


    /**
     * Executes the proposed move,
     * to be implemented by each move type.
     * This method should be idempotent.
     *
     * @param solution Solution where this move will be applied to.
     * @return modified solution
     */
    @Override
    protected abstract PSSCSolution _execute(PSSCSolution solution);

    /**
     * Get the movement value, represents how much does the move changes the f.o of a solution if executed
     * @return f.o change
     */
    public abstract double getScoreChange();

    /**
     * Returns a String representation of the current movement.
     * @return human readable string
     */
    @Override
    public abstract String toString();

    /** {@inheritDoc} */
    @Override
    public abstract boolean equals(Object o);

    /** {@inheritDoc} */
    @Override
    public abstract int hashCode();
}