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
package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.CanonizingIOCacheOracle;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.BasicSULOracle;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.tools.theories.SymbolicTraceCanonizer;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import org.testng.Assert;


/**
 *
 * @author falk
 */
public class FreshValuesTest extends RaLibTestSuite {
    
    @Test
    public void testModelswithOutput() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/keygen.xml");


        RegisterAutomaton model = loader.getRegisterAutomaton();
        logger.log(Level.FINE, "SYS: {0}", model);

        ParameterizedSymbol[] inputs = loader.getInputs().toArray(
                new ParameterizedSymbol[]{});

        Constants consts = loader.getConstants();

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        loader.getDataTypes().stream().forEach((t) -> {
            teachers.put(t, new IntegerEqualityTheory(t));
        });

        DataWordSUL sul = new SimulatorSUL(model, teachers, consts);
    
        IOOracle ioOracle = new BasicSULOracle(sul, ERROR);
        CanonizingIOCacheOracle ioCache = new CanonizingIOCacheOracle(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);
        
        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, ioCache, teachers, consts, new SimpleConstraintSolver());        
                
        teachers.values().stream().forEach((t) -> {
            ((EqualityTheory)t).setFreshValues(true);
        });
        
        DataType intType = TestUtil.getType("int", loader.getDataTypes());
  
        
        ParameterizedSymbol iput = new InputSymbol(
                "IPut", new DataType[] {intType});

        ParameterizedSymbol iget = new InputSymbol(
                "IGet", new DataType[] {intType});

         ParameterizedSymbol oput = new OutputSymbol(
                "OPut", new DataType[] {intType}); 
         
         ParameterizedSymbol oget = new OutputSymbol(
                "OGet", new DataType[] {intType});    
         
         ParameterizedSymbol onok = new OutputSymbol(
                "ONOK", new DataType[] {});   
         
         DataValue d0 = new DataValue(intType, 0);
         DataValue d1 = new DataValue(intType, 1);
         DataValue d2 = new DataValue(intType, 2);

        // IPut[0[int]] OPut[1[int]] IGet[2[int]] ONOK[] [p2>r1,p1>r2,p3>r3,] []  
        Word<PSymbolInstance> prefix1 = Word.fromSymbols(
                new PSymbolInstance(iput, d0),
                new PSymbolInstance(oput, d1),
                new PSymbolInstance(iget, d2),
                new PSymbolInstance(onok));

         DataValue d3 = new DataValue(intType, 3);
         DataValue d4 = new DataValue(intType, 4);
         DataValue d5 = new DataValue(intType, 5);
        
        // [s2, s4]((IGet[s1] ONOK[] IPut[s2] OPut[s3] IGet[s4] ONOK[] IPut[s5] ONOK[]))
        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(iget, d3),
                new PSymbolInstance(onok),
                new PSymbolInstance(iput, d0),
                new PSymbolInstance(oput, d4),
                new PSymbolInstance(iget, d0),
                new PSymbolInstance(onok),
                new PSymbolInstance(iput, d5),
                new PSymbolInstance(onok));
        
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix1, suffix, new Constants(), teachers);
        
        logger.log(Level.FINE, "Prefix: {0}", prefix1);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult tqr = mto.treeQuery(prefix1, symSuffix);       
        String tree = tqr.getSdt().toString();
                
        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());        
        logger.log(Level.FINE, "SDT: {0}", tree);
        
        
        final String expectedTree = "[r1]-+\n" +
"    []-(s1=r1)\n" +
"     |    []-TRUE: s2\n" +
"     |          []-TRUE: s3\n" +
"     |                []-TRUE: s4\n" +
"     |                      []-TRUE: s5\n" +
"     |                            [Leaf-]\n" +
"     +-(s1!=r1)\n" +
"          []-TRUE: s2\n" +
"                []-TRUE: s3\n" +
"                      []-(s4=r1)\n" +
"                       |    []-TRUE: s5\n" +
"                       |          [Leaf-]\n" +
"                       +-(s4=s3)\n" +
"                       |    []-TRUE: s5\n" +
"                       |          [Leaf-]\n" +
"                       +-ANDCOMPOUND: s4[(s4!=r1), (s4!=s3)]\n" +
"                            []-TRUE: s5\n" +
"                                  [Leaf+]\n";
        
        Assert.assertEquals(tree, expectedTree);        
    }
 
}
