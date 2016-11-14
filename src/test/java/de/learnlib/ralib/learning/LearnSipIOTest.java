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

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IOEquivalenceTest;
import de.learnlib.ralib.equivalence.IOHypVerifier;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class LearnSipIOTest extends RaLibTestSuite {

    @Test
    public void learnLoginExampleIO() {

        long seed = -1386796323025681754L; 
        //long seed = (new Random()).nextLong();
        logger.log(Level.FINE, "SEED={0}", seed);
        final Random random = new Random(seed);
      
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        final Constants consts = loader.getConstants();

        
        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            IntegerEqualityTheory theory = new IntegerEqualityTheory(t);
            theory.setUseSuffixOpt(true);
            teachers.put(t, theory);
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
        IOOracle ioOracle = new BasicSULOracle(sul, ERROR);
        IOCacheOracle ioCache = new IOCacheOracle(ioOracle, new SymbolicTraceCanonizer(teachers, consts));
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        ConstraintSolver solver = new SimpleConstraintSolver();
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, ioCache, teachers, consts, solver);
        MultiTheorySDTLogicOracle mlo = 
                new MultiTheorySDTLogicOracle(consts, solver);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> 
                TestUtil.createMTO(hyp, teachers, consts, solver);
        
                IOHypVerifier hypVerifier = new IOHypVerifier(teachers, consts);
                
        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, hypVerifier, actions);

            IOEquivalenceTest ioEquiv = new IOEquivalenceTest(
                    model, teachers, consts, true, actions);
        
        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle, hypVerifier);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle, hypVerifier);                        
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle, hypVerifier);
                                                
        int check = 0;
        while (true && check < 100) {
            
            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
              
            DefaultQuery<PSymbolInstance, Boolean> ce = 
                    ioEquiv.findCounterExample(hyp, null);

            if (ce == null) {
                break;
            }

            ce = loops.optimizeCE(ce.getInput(), hyp);
            ce = asrep.optimizeCE(ce.getInput(), hyp);
            ce = pref.optimizeCE(ce.getInput(), hyp);

            Assert.assertTrue(model.accepts(ce.getInput()));
            Assert.assertTrue(!hyp.accepts(ce.getInput()));
            
            rastar.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        logger.log(Level.FINE, "FINAL HYP: {0}", hyp);
        DefaultQuery<PSymbolInstance, Boolean> ce = 
            ioEquiv.findCounterExample(hyp, null);
            
        Assert.assertNull(ce);        
    }
}
