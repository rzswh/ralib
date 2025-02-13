/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.learnlib.ralib.solver;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;

/**
 *
 * @author falk
 */
public class ConstraintSolverFactory {
 
    public final static String ID_SIMPLE = "simple";

    public final static String ID_Z3 = "z3";

    public ConstraintSolverFactory(){
        // Do nothing
    }
    
    
    public static ConstraintSolver createSolver(final String id) {
        switch (id) {
            case ID_SIMPLE:
                return createSimpleConstraintSolver();
            case ID_Z3:
                return (ConstraintSolver) createZ3ConstraintSolver();
            default:
                throw new RuntimeException("Unsupported constraint solver: " + id);
        }
    }
    
    public static SimpleConstraintSolver createSimpleConstraintSolver() {
        return new SimpleConstraintSolver();
    }
    
    public static ConstraintSolver createZ3ConstraintSolver() {
        gov.nasa.jpf.constraints.api.ConstraintSolver innerSolver = gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory.createSolver("z3");
        
        return new JConstraintsConstraintSolver(innerSolver) ;
    }
    
}
