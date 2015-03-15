/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.learning;

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoader;
import de.learnlib.ralib.automata.xml.RegisterAutomatonLoaderTest;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixFinder;
import de.learnlib.ralib.equivalence.IOCounterExamplePrefixReplacer;
import de.learnlib.ralib.equivalence.IOCounterexampleLoopRemover;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class LearnLoginIOTest {

    public LearnLoginIOTest() {
    }

    @Test
    public void learnLoginExampleIO() {

        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINEST);
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.FINEST);
     //       h.setFilter(new CategoryFilter(EnumSet.of(
//                   Category.EVENT, Category.PHASE, Category.MODEL, Category.SYSTEM)));
     //               Category.EVENT, Category.PHASE, Category.MODEL)));
        }

        final ParameterizedSymbol ERROR
                = new OutputSymbol("_io_err", new DataType[]{});

        RegisterAutomatonLoader loader = new RegisterAutomatonLoader(
                RegisterAutomatonLoaderTest.class.getResourceAsStream(
                        "/de/learnlib/ralib/automata/xml/login.xml"));

        RegisterAutomaton model = loader.getRegisterAutomaton();
        System.out.println("SYS:------------------------------------------------");
        System.out.println(model);
        System.out.println("----------------------------------------------------");

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        ParameterizedSymbol[] actions = loader.getActions().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new HashMap<DataType, Theory>();
        for (final DataType t : loader.getDataTypes()) {
            teachers.put(t, new EqualityTheory() {
                @Override
                public DataValue getFreshValue(List vals) {
                    //System.out.println("GENERATING FRESH: " + vals.size());
                    return new DataValue(t, vals.size());
                }
            });
        }

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts, inputs);

        SimulatorOracle oracle = new SimulatorOracle(model);
        
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        //IOCache ioCache = new IOCache(ioOracle);
        //IOFilter ioFilter = new IOFilter(oracle, inputs);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(oracle, teachers);
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle();

        TreeOracleFactory hypFactory = new TreeOracleFactory() {

            @Override
            public TreeOracle createTreeOracle(RegisterAutomaton hyp) {
                return new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers);
            }
        };

        RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, true, actions);

        long seed = 1423423204823000L; //-3921391701929787366L;// (new Random()).nextLong();
        System.out.println("SEED=" + seed);
              
        IORandomWalk iowalk = new IORandomWalk(new Random(seed),
                sul,
                false, // do not draw symbols uniformly 
                0.05, // reset probability 
                0.5, // prob. of choosing a fresh data value
                1000, // 1000 runs 
                10, // max depth
                consts,
                false, // reset runs 
                teachers,
                inputs);
        
        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);                        
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);
                        
                        
        int check = 0;
        while (true && check < 100) {

            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
            System.out.println("HYP:------------------------------------------------");
            System.out.println(hyp);
            System.out.println("----------------------------------------------------");

            DefaultQuery<PSymbolInstance, Boolean> ce = 
                    iowalk.findCounterExample(hyp, null);
           
            System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

            ce = loops.optimizeCE(ce.getInput(), hyp);
            System.out.println("Shorter CE: " + ce);
//            ce = asrep.replacePrefix(ce, hyp);
//            System.out.println("New Prefix CE: " + ce);
//            ce = pref.findPrefix(ce, hyp);
//            System.out.println("Prefix of CE is CE: " + ce);
            
            assert model.accepts(ce.getInput());
            assert !hyp.accepts(ce.getInput());
            
            rastar.addCounterexample(ce);

        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        System.out.println("LAST:------------------------------------------------");
        System.out.println(hyp);
        System.out.println("----------------------------------------------------");

        System.out.println(ioOracle.getQueryCount());
        System.out.println(seed);
    }
}
