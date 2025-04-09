package es.urjc.etsii.grafo.PSSC.model.neigh;

import es.urjc.etsii.grafo.PSSC.model.PSSCBaseMove;
import es.urjc.etsii.grafo.PSSC.model.PSSCInstance;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;

import java.util.ArrayList;

public class RemoveNeighborhood extends Neighborhood<RemoveNeighborhood.RemoveMove, PSSCSolution, PSSCInstance> {

    @Override
    public ExploreResult<RemoveMove, PSSCSolution, PSSCInstance> explore(PSSCSolution solution) {
        var list = new ArrayList<RemoveMove>();
        // TODO Generate a list with all valid removals for current solution
        return ExploreResult.fromList(list);
    }

    public static class RemoveMove extends PSSCBaseMove {

        // TODO add properties if required

        public RemoveMove(PSSCSolution solution) {
            super(solution);
        }

        @Override
        protected PSSCSolution _execute(PSSCSolution solution) {
            // TODO apply removal to the given solution
            return solution;
        }

        @Override
        public double getScoreChange() {
            // how much does the score change if we apply this movement?
            // TODO
            throw new UnsupportedOperationException("getScoreChange() in PSSCListManager not implemented yet");
        }

        @Override
        public String toString() {
            // TODO Use IDE to generate this method after all properties are defined
            throw new UnsupportedOperationException("toString() in SwapMove not implemented yet");
        }

        @Override
        public boolean equals(Object o) {
            // TODO Use IDE to generate this method after all properties are defined
            throw new UnsupportedOperationException("equals() in SwapMove not implemented yet");
        }

        @Override
        public int hashCode() {
            // TODO Use IDE to generate this method after all properties are defined
            throw new UnsupportedOperationException("hashCode() in SwapMove not implemented yet");
        }
    }
}
