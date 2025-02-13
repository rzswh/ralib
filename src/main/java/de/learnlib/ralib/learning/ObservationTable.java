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
package de.learnlib.ralib.learning;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.automatalib.words.Word;

/**
 * An observation table.
 * 
 * @author falk
 */
class ObservationTable {
    
    private final List<GeneralizedSymbolicSuffix> suffixes = new LinkedList<>();
    
    private final Map<Word<PSymbolInstance>, Component> components
            = new LinkedHashMap<>();
    
    private final Deque<GeneralizedSymbolicSuffix> newSuffixes = new LinkedList<>();
    
    private final Deque<Word<PSymbolInstance>> newPrefixes = new LinkedList<>();
    
    private final Deque<Component> newComponents = new LinkedList<>();

    private final Deque<ParameterizedSymbol> newSymbols = new LinkedList<>();

    private final TreeOracle oracle;
    
    private ParameterizedSymbol[] inputs;
    
    private final boolean ioMode;
    
    private final Constants consts;
    
    private final Map<DataType, Theory> teachers;
    
    private final ConstraintSolver solver;
    
    private static LearnLogger log = LearnLogger.getLogger(ObservationTable.class);
    
    public ObservationTable(TreeOracle oracle, boolean ioMode, 
            Constants consts, Map<DataType, Theory> teachers,
            ConstraintSolver solver,
            ParameterizedSymbol ... inputs) {
        this.oracle = oracle;
        this.inputs = inputs;
        this.ioMode = ioMode;
        this.consts = consts;
        this.teachers = teachers;
        this.solver = solver;
    }
        
    void addComponent(Component c) {
        // log.logEvent("Queueing component for obs: " + c);
        newComponents.add(c);
    }
    
    void addSuffix(GeneralizedSymbolicSuffix suffix) {
        // log.logEvent("Queueing suffix for obs: " +  suffix);
        // System.out.println("Adding suffix: " + suffix);
        newSuffixes.add(suffix);
    }
    
    void addPrefix(Word<PSymbolInstance> prefix) {
        // log.logEvent("Queueing prefix for obs: " + prefix);
        newPrefixes.add(prefix);
    }

    void addSymbol(ParameterizedSymbol symbol) {
        newSymbols.add(symbol);
    }
    
    boolean complete() {    
    	String done = "nothing";
        if (!newComponents.isEmpty()) {
            processNewComponent();
            done = "new comp";
            return false;
        }
        
        if (!newPrefixes.isEmpty()) {
        	// System.out.println("new prefix: " + newPrefixes.peek());
            processNewPrefix();
            done = "newPrefix";
            return false;
        }
        
        if (!newSuffixes.isEmpty()) {
            processNewSuffix();
            checkBranchingCompleteness();
            done = "newSuffix";
            return false;
        }

        if (!newSymbols.isEmpty()) {
            processNewSymbols();
            done = "newSymbol";
            return false;
        }
        
        if (!checkVariableConsistency()) {
            //AutomatonBuilder ab = new AutomatonBuilder(getComponents(), new Constants());            
            //Hypothesis hyp = ab.toRegisterAutomaton();        
            
            //FIXME: the default logging appender cannot log models and data structures
            //System.out.println(hyp.toString());            
        	done = "varInconsistency";
        	return false;
        }
        // System.out.println(done);
        
        return true;
    }

    private boolean checkBranchingCompleteness() {
        log.logPhase("Checking Branching Completeness");
        boolean ret = true;
        for (Component c : components.values()) {
            boolean ub = c.updateBranching(oracle);
            ret = ret && ub;
        }       
        return ret;
    }
    
    private boolean checkVariableConsistency() {
        log.logPhase("Checking Variable Consistency");
        for (Component c : components.values()) {
            if (!c.checkVariableConsistency()) {
                return false;
            }
        }
        return true;
    }

    private void processNewSuffix() {
        GeneralizedSymbolicSuffix suffix = newSuffixes.poll();
        log.logEvent("Adding suffix to obs: " + suffix);
//        System.out.println("Adding suffix to obs: " + suffix);
        suffixes.add(suffix);
        for (Component c : components.values()) {
            c.addSuffix(suffix, oracle);
        }
    }

    private void processNewPrefix() {
        Word<PSymbolInstance> prefix = newPrefixes.poll();
        log.logEvent("Adding prefix to obs: " + prefix);
        Row r = Row.computeRow(oracle, prefix, suffixes, ioMode);
        for (Component c : components.values()) {
            if (c.addRow(r)) {
                return;
            }
        }
        Component c = new Component(r, this, ioMode, consts, teachers, solver);
        addComponent(c);
    }

    private void processNewComponent() {
        Component c = newComponents.poll();
        log.logEvent("Adding component to obs: " + c);
        components.put(c.getAccessSequence(), c);
        c.start(oracle, inputs);
    }

    private void processNewSymbols() {
        ParameterizedSymbol symbol = newSymbols.poll();
        log.logEvent("Adding symbol to obs: " + symbol);
        addSuffix(new GeneralizedSymbolicSuffix(symbol, teachers));
        for (Component c: components.values()) {
            c.start(oracle, symbol);
        }
        inputs = Arrays.copyOf(inputs, inputs.length + 1);
        inputs[inputs.length - 1] = symbol;
    }

    Map<Word<PSymbolInstance>, Component> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OBS *******************************************************************\n");
        for (Component c : getComponents().values()) {            
            c.toString(sb);
        } 
        sb.append("***********************************************************************\n");
        return sb.toString();
    }
    
}
