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
package de.learnlib.ralib.automata;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.logging.Level;

/**
 * Register Automaton transitions have input symbols, and assignments.
 * 
 * @author falk
 */
public class Transition {
       
    protected final ParameterizedSymbol label;

    protected final TransitionGuard guard;
     
    protected final RALocation source;
    
    protected final RALocation destination;

    protected final Assignment assignment;

    private static final LearnLogger log = LearnLogger.getLogger(Transition.class);
    
    public Transition(ParameterizedSymbol label, TransitionGuard guard, 
            RALocation source, RALocation destination, Assignment assignment) {
        this.label = label;
        this.guard = guard;
        this.source = source;
        this.destination = destination;
        this.assignment = assignment;
    }
    
    public boolean isEnabled(VarValuation registers, 
            ParValuation parameters, Constants consts) {
//        log.log(Level.FINEST, "isEnabled..... registers: {0}", registers.toString());
//        log.log(Level.FINEST, " ...... parameters: {0}", parameters.toString());
//        log.log(Level.FINEST, " ..... constants {0}\n", consts.toString());
        return guard.isSatisfied(registers, parameters, consts);        
    }
    
    public VarValuation execute(VarValuation registers, ParValuation parameters, Constants consts) {    
        return this.getAssignment().compute(registers, parameters, consts);
    }
    
    /**
     * @return the label
     */
    public ParameterizedSymbol getLabel() {
        return label;
    }

    /**
     * @return the source
     */
    public RALocation getSource() {
        return source;
    }

    /**
     * @return the destination
     */
    public RALocation getDestination() {
        return destination;
    }

    /**
     * @return the assignment
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * @return the guard
     */
    public TransitionGuard getGuard() {
        return guard;
    }
    
    @Override
    public String toString() {
        return "(" + source + ", " + label + ", " + guard + ", " + assignment + ", " + destination + ")";
    }    
    
}
