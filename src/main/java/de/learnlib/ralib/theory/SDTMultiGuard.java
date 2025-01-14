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
package de.learnlib.ralib.theory;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.theory.inequality.IntervalGuard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class SDTMultiGuard extends SDTGuard {

    protected enum ConDis {

        AND, OR
    }

    protected final List<SDTGuard> guards;
    protected final Set<SDTGuard> guardSet;
    protected final ConDis condis;

    public List<SDTGuard> getGuards() {
        return guards;
    }
    
    public boolean isSingle() {
        return guards.size() == 1;
    }

    public boolean isEmpty() {
        return guards.isEmpty();
    }

    public Set<SymbolicDataValue> getAllSDVsFormingGuard() {
        Set<SymbolicDataValue> allRegs = new LinkedHashSet<SymbolicDataValue>();
        for (SDTGuard g : guards) {
            if (g instanceof SDTIfGuard) {
                allRegs.add(((SDTIfGuard)g).getRegister());
            }
            else if (g instanceof SDTMultiGuard) {
                allRegs.addAll(((SDTMultiGuard)g).getAllSDVsFormingGuard());
            } else if (g instanceof IntervalGuard) {
            	allRegs.addAll(((IntervalGuard) g).getAllSDVsFormingGuard());
            }
        }
        return allRegs;
    }

    public SDTMultiGuard(SuffixValue param, ConDis condis, SDTGuard... ifGuards) {
        super(param);
        this.condis = condis;
        guards = new ArrayList<>();
        guards.addAll(Arrays.asList(ifGuards));
        guardSet = new LinkedHashSet<>(guards);
    }

    @Override
    public abstract GuardExpression toExpr();

    @Override
    public String toString() {
        String p = condis.toString() + "COMPOUND: " + parameter.toString();
        if (guards.isEmpty()) {
            return p + "empty";
        }
        return p + guards.toString();
    }

}
