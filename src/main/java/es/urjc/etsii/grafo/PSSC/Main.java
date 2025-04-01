package es.urjc.etsii.grafo.PSSC;

import es.urjc.etsii.grafo.PSSC.model.PSSCBaseMove;
import es.urjc.etsii.grafo.PSSC.model.PSSCSolution;
import es.urjc.etsii.grafo.solution.Objective;
import es.urjc.etsii.grafo.solver.Mork;

public class Main {

    public static final String OBJECTIVE_NAME = "Objective1";

    public static void main(String[] args) {
        // TODO Configure your objectives
        // Replace Objective1 with your objective name, possible examples are: "Distance", "Cost", etc.
        var myObjective = Objective.ofMinimizing("objectiveName", PSSCSolution::getScore, PSSCBaseMove::getScoreChange);
        Mork.start(args, myObjective);

        // For a more flexible approach, you can use the following method:
        // Mork.start(args, multiobjective:true/false, myObjective1, myObjective2, myObjective3);
        // See the docs for more information on how to configure your objectives if using alternative objective functions
        // or you are solving a multi-objective problem
    }
}
