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
package de.learnlib.ralib.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.util.RAToDot;
import de.learnlib.ralib.automata.xml.RegisterAutomatonExporter;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.HypVerifier;
import de.learnlib.ralib.equivalence.IOCounterExampleLoopRemover;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.RaStar;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.CanonizingIOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import de.learnlib.statistics.SimpleProfiler;

/**
 *
 * @author falk
 */
public class IOSimulator extends AbstractToolWithRandomWalk {
            
    private static final ConfigurationOption.StringOption OPTION_TARGET = 
            new ConfigurationOption.StringOption("target", 
                    "XML file with target sul", null, false);    
            
    private static final ConfigurationOption.BooleanOption OPTION_USE_EQTEST =
            new ConfigurationOption.BooleanOption("use.eqtest", 
                    "Use an eq test for finding counterexamples", Boolean.FALSE, true);
                
    private static final ConfigurationOption[] OPTIONS = getOptions(IOSimulator.class, EquivalenceOracleFactory.class);
    
    private RegisterAutomaton model;

    private DataWordSUL sulLearn;
    
    private DataWordSUL sulTest;
        
    private IOEquivalenceOracle randomWalk = null;
    
    private IOEquivalenceTest eqTest;
    
    private RaStar rastar;

    private IOCounterExampleLoopRemover ceOptLoops;
    
    private IOCounterExamplePrefixReplacer ceOptAsrep;                      
    
    private IOCounterExamplePrefixFinder ceOptPref;
    
    private boolean useEqTest;
 
    private long resets = 0;
    private long inputs = 0;
    
    private Constants consts;
    
    private Map<DataType, Theory> teachers;

    @Override
    public String description() {
        return "uses an IORA model as SUL";
    }

    @Override
    public void setup(Configuration config) throws ConfigurationException {
        super.setup(config);
        
        config.list(System.out);
        
        // target
        String filename = OPTION_TARGET.parse(config);
        FileInputStream fsi;        
        try {
            fsi = new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        RegisterAutomatonImporter loader = new RegisterAutomatonImporter(fsi);
        this.model = loader.getRegisterAutomaton();
        
        ParameterizedSymbol[] inputSymbols = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        consts = loader.getConstants();
        
        Map<String, DataType> typeMap = loader.getDataTypes()
        		.stream().collect(Collectors.toMap(type -> type.getName(), type -> type));
        
        
        // create teachers
        this.teachers = super.buildTypeTheoryMapAndConfigureTheories(teacherClasses, config, typeMap, inputSymbols, consts);

        // oracles
        this.sulLearn = new SimulatorSUL(model, teachers, consts);
        if (this.timeoutMillis > 0L) {
           this.sulLearn = new TimeOutSUL(this.sulLearn, this.timeoutMillis);
        }
        this.sulTest  = new SimulatorSUL(model, teachers, consts);
        if (this.timeoutMillis > 0L) {
           this.sulTest = new TimeOutSUL(this.sulTest, this.timeoutMillis);
        }
        
        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});
        
       IOOracle back = new BasicSULOracle(sulLearn, ERROR);
       CanonizingIOCacheOracle ioCache = new CanonizingIOCacheOracle(back);
       IOFilter ioOracle = new IOFilter(ioCache, inputSymbols);
                
       
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(ioOracle, ioCache, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, solver);

        final long timeout = this.timeoutMillis;
        TreeOracleFactory hypFactory = new TreeOracleFactory() {
            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                DataWordOracle hypOracle = new SimulatorOracle(hyp);
                if (timeout > 0L) {
                    hypOracle = new TimeOutOracle(hypOracle, timeout);
                }
                return new MultiTheoryTreeOracle(hypOracle, ioCache, teachers, consts, solver);
            }
        };
        
        this.rastar = new RaStar(mto, hypFactory, mlo, consts, true, 
                teachers, solver, actions);
        this.eqTest = new IOEquivalenceTest(model, teachers, consts, true, actions);

        this.useEqTest = OPTION_USE_EQTEST.parse(config);
      
        if (findCounterexamples) {
        	IOOracle testOracle = new BasicSULOracle(sulTest, SpecialSymbols.ERROR);
        	this.randomWalk = EquivalenceOracleFactory.buildEquivalenceOracle(config,
        			testOracle, this.teachers, consts, random, inputSymbols);            
        }
        
        HypVerifier hypVerifier = HypVerifier.getVerifier(true, teachers, consts);
        
        this.ceOptLoops = new IOCounterExampleLoopRemover(back, hypVerifier);
        this.ceOptAsrep = new IOCounterExamplePrefixReplacer(back, hypVerifier);                        
        this.ceOptPref = new IOCounterExamplePrefixFinder(back, hypVerifier);
    }
    
    @Override
    public void run() {

        System.out.println("=============================== START ===============================");
        
        final String __RUN__ = "overall execution time";
        final String __LEARN__ = "learning";
        final String __SEARCH__ = "ce searching";
        final String __EQ__ = "eq tests";
                
        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");
        
        SimpleProfiler.start(__RUN__);
        SimpleProfiler.start(__LEARN__);
        
        boolean eqTestfoundCE = false;
        ArrayList<Integer> ceLengths = new ArrayList<>();
        ArrayList<Integer> ceLengthsShortened = new ArrayList<>();
        Hypothesis hyp = null;
        
        int rounds = 0;
        while (true && (maxRounds < 0 || rounds < maxRounds)) {
                        
            rounds++;
            rastar.learn();            
            hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

            SimpleProfiler.stop(__LEARN__);
            SimpleProfiler.start(__EQ__);
            DefaultQuery<PSymbolInstance, Boolean> ce  = null; 
            DefaultQuery<PSymbolInstance, Boolean> origCe  = null; 
            
            if (useEqTest) {
                ce = this.eqTest.findCounterExample(hyp, null);

                if (ce != null) {
                    eqTestfoundCE = true;
                    System.out.println("EQ-TEST found counterexample: " + ce);
                } else {
                    eqTestfoundCE = false;
                    System.out.println("EQ-TEST did not find counterexample!");                
                }
            }
            
            SimpleProfiler.stop(__EQ__);
            SimpleProfiler.start(__SEARCH__);
            
            if (findCounterexamples) {
                ce = null;
            }
            
            boolean nullCe = false;            
            for (int i=0; i<3; i++) {
            
                DefaultQuery<PSymbolInstance, Boolean> ce2 = null;
                
                if (findCounterexamples) {
                    ce2 = this.randomWalk.findCounterExample(hyp, null);
                } else {
                    ce2 = ce;
                }

                SimpleProfiler.stop(__SEARCH__);
                System.out.println("CE: " + ce2);
                if (ce2 == null) {
                    nullCe = true;
                    break;
                }

                resets = sulTest.getResets();
                inputs = sulTest.getInputs();


                if (useCeOptimizers) {
                    ce2 = ceOptLoops.optimizeCE(ce2.getInput(), hyp);
                    System.out.println("Shorter CE: " + ce2);
                    ce2 = ceOptAsrep.optimizeCE(ce2.getInput(), hyp);
                    System.out.println("New Prefix CE: " + ce2);
                    ce2 = ceOptPref.optimizeCE(ce2.getInput(), hyp);
                    System.out.println("Prefix of CE is CE: " + ce2);
                }
                   
                ce = (ce == null || ce.getInput().length() > ce2.getInput().length()) ?
                        ce2 : ce;
            }
   
            if (nullCe) {
                break;
            }
            
            SimpleProfiler.start(__LEARN__);
            //ceLengths.add(ce.getInput().length());
            
            //ceLengthsShortened.add(ce.getInput().length());
            
            assert model.accepts(ce.getInput());
            assert !hyp.accepts(ce.getInput());
            
            rastar.addCounterexample(ce);
        }
        
        System.out.println("=============================== STOP ===============================");
        System.out.println(SimpleProfiler.getResults());
        
        for (Entry<DataType, Theory> e : teachers.entrySet()) {
            System.out.println("Theory: " + e.getKey() + " -> " + e.getValue().getClass().getName());
        }
        
        if (useEqTest) {
            System.out.println("Last EQ Test found a counterexample: " + eqTestfoundCE);
        }
        
        System.out.println("ce lengths (oirginal): " + 
                Arrays.toString(ceLengths.toArray()));
        
        if (useCeOptimizers) {
            System.out.println("ce lengths (shortend): " + 
                    Arrays.toString(ceLengthsShortened.toArray()));
        }
                
        // model
        if (hyp != null) {            
            System.out.println("Locations: " + hyp.getStates().size());
            System.out.println("Transitions: " + hyp.getTransitions().size());
        
            // input locations + transitions            
            System.out.println("Input Locations: " + hyp.getInputStates().size());
            System.out.println("Input Transitions: " + hyp.getInputTransitions().size());
            
            if (this.exportModel) {
                System.out.println("exporting model to model.xml");
                try {
                    FileOutputStream fso = new FileOutputStream("model.xml");
                    RegisterAutomatonExporter.write(hyp, consts, fso);
                } catch (FileNotFoundException ex) {
                    System.out.println("... export failed");
                }
                System.out.println("exporting model to model.dot");
				RAToDot dotExport = new RAToDot(hyp, true);
				String dot = dotExport.toString();

				try (BufferedWriter wr = new BufferedWriter(new FileWriter(new File("model.dot")))) {
					wr.write(dot, 0, dot.length());
				} catch (IOException ex) {
					System.out.println("... export failed");
				}
            }
        }
        
        // tests during learning
        // resets + inputs
        System.out.println("Resets Learning: " + sulLearn.getResets());
        System.out.println("Inputs Learning: " + sulLearn.getInputs());
        
        // tests during search
        // resets + inputs
        System.out.println("Resets Testing: " + resets);
        System.out.println("Inputs Testing: " + inputs);
        
        // + sums
        System.out.println("Resets: " + (resets + sulLearn.getResets()));
        System.out.println("Inputs: " + (inputs + sulLearn.getInputs()));
        
    }

    
    
    @Override
    public String help() {
        StringBuilder sb = new StringBuilder();
        for (ConfigurationOption o : OPTIONS) {
            sb.append(o.toString()).append("\n");
        }
        return sb.toString();
    }
    
    
}
