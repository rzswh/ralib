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
package de.learnlib.ralib.solver.jconstraints;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.solver.*;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;

/**
 *
 * @author falk
 */
public class JConstraintsConstraintSolver implements ConstraintSolver {
    
    private final gov.nasa.jpf.constraints.api.ConstraintSolver solver;
    
    public JConstraintsConstraintSolver(
            gov.nasa.jpf.constraints.api.ConstraintSolver solver) {
        this.solver = solver;
    }
    
    @Override
    public boolean isSatisfiable(GuardExpression expr) {
        Expression<Boolean> jexpr = JContraintsUtil.toExpression(expr);
        Result r = solver.isSatisfiable(jexpr);
        return r == Result.SAT;
    }
    
}
