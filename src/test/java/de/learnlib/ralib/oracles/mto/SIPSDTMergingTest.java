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

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SimulatorSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import net.automatalib.words.Word;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 *
 * @author falk
 */
public class SIPSDTMergingTest extends RaLibTestSuite {
    
    @Test
    public void testModelswithOutput() {
 
        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/sip.xml");

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
        MultiTheoryTreeOracle mto = TestUtil.createMTOWithFreshValueSupport(sul, ERROR, 
                teachers, consts, new SimpleConstraintSolver(), inputs);
    
        DataType intType = TestUtil.getType("int", loader.getDataTypes());
          
        ParameterizedSymbol ipr = new InputSymbol(
                "IPRACK", new DataType[] {intType});

        ParameterizedSymbol inv = new InputSymbol(
                "IINVITE", new DataType[] {intType});

         ParameterizedSymbol inil = new InputSymbol(
                "Inil", new DataType[] {}); 
         
         ParameterizedSymbol o100 = new OutputSymbol(
                "O100", new DataType[] {intType});    

        ParameterizedSymbol o486 = new OutputSymbol(
                "O486", new DataType[] {intType});    

        ParameterizedSymbol o481 = new OutputSymbol(
                "O481", new DataType[] {intType});         
         
        DataValue d0 = new DataValue(intType, 0);
        DataValue d1 = new DataValue(intType, 1);

        
        //****** ROW:  IINVITE[0[int]] O100[0[int]] IINVITE[1[int]] O100[1[int]]
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(inv, d0),
                new PSymbolInstance(o100, d0),
                new PSymbolInstance(inv, d1),
                new PSymbolInstance(o100, d1));
        
        //**** [s1, s2, s3]((Inil[] O486[s1] IPRACK[s2] O481[s3]))
        Word<PSymbolInstance> suffix =  Word.fromSymbols(
                new PSymbolInstance(inil),
                new PSymbolInstance(o486, d0),
                new PSymbolInstance(ipr, d0),
                new PSymbolInstance(o481, d0));
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), teachers);
        
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);
        
        TreeQueryResult tqr = mto.treeQuery(prefix, symSuffix);       
        String tree = tqr.getSdt().toString();
                
        logger.log(Level.FINE, "PIV: {0}", tqr.getPiv());        
        logger.log(Level.FINE, "SDT: {0}",tree);
        
        final String expectedTree = "[r1, r2]-+\n" +
"        []-(s1=r1)\n" +
"         |    []-(s2=r2)\n" +
"         |     |    []-TRUE: s3\n" +
"         |     |          [Leaf-]\n" +
"         |     +-(s2!=r2)\n" +
"         |          []-(s3=s2)\n" +
"         |           |    [Leaf+]\n" +
"         |           +-(s3!=s2)\n" +
"         |                [Leaf-]\n" +
"         +-(s1!=r1)\n" +
"              []-TRUE: s2\n" +
"                    []-TRUE: s3\n" +
"                          [Leaf-]\n";
        
        Assert.assertEquals(tree, expectedTree);
    }
        
}
