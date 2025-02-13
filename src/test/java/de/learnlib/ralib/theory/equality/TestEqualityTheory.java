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
package de.learnlib.ralib.theory.equality;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.AUTOMATON;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.simple.SimpleConstraintSolver;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class TestEqualityTheory extends RaLibTestSuite {

    @Test
    public void testLoginExample1() {
        
        DataWordOracle oracle = new SimulatorOracle(AUTOMATON);
            
        Map<DataType, Theory> theories = new LinkedHashMap();
        theories.put(T_UID, new IntegerEqualityTheory(T_UID));
        theories.put(T_PWD, new IntegerEqualityTheory(T_PWD));
        
        MultiTheoryTreeOracle treeOracle = new MultiTheoryTreeOracle(
                oracle, null, theories, new Constants(), new SimpleConstraintSolver());
        
        final Word<PSymbolInstance> longsuffix = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        final Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(I_REGISTER, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 1)));
        
        
        // create a symbolic suffix from the concrete suffix
        // symbolic data values: s1, s2 (userType, passType)        
        final GeneralizedSymbolicSuffix symSuffix = 
                new GeneralizedSymbolicSuffix(prefix, longsuffix,
                        new Constants(), theories);
        logger.log(Level.FINE, "Prefix: {0}", prefix);
        logger.log(Level.FINE, "Suffix: {0}", symSuffix);        
        
        TreeQueryResult res = treeOracle.treeQuery(prefix, symSuffix);
        SymbolicDecisionTree sdt = res.getSdt();

        String expectedTree = "[r2, r1]-+\n" +
"        []-(s1=r2)\n" +
"         |    []-(s2=r1)\n" +
"         |     |    []-(s3=r2)\n" +
"         |     |     |    []-(s4=r1)\n" +
"         |     |     |     |    [Leaf+]\n" +
"         |     |     |     +-(s4!=r1)\n" +
"         |     |     |          [Leaf-]\n" +
"         |     |     +-(s3!=r2)\n" +
"         |     |          []-TRUE: s4\n" +
"         |     |                [Leaf-]\n" +
"         |     +-(s2!=r1)\n" +
"         |          []-TRUE: s3\n" +
"         |                []-TRUE: s4\n" +
"         |                      [Leaf-]\n" +
"         +-(s1!=r2)\n" +
"              []-TRUE: s2\n" +
"                    []-TRUE: s3\n" +
"                          []-TRUE: s4\n" +
"                                [Leaf-]\n";
        
        String tree = sdt.toString();
        Assert.assertEquals(tree, expectedTree);
        logger.log(Level.FINE, "final SDT: \n{0}", tree);
        
        Parameter p1 = new Parameter(T_UID, 1);
        Parameter p2 = new Parameter(T_PWD, 2);
        
        PIV testPiv =  new PIV();
        testPiv.put(p1, new Register(T_UID, 1));
        testPiv.put(p2, new Register(T_PWD, 2));
        
        Branching b = treeOracle.getInitialBranching(prefix, I_LOGIN, testPiv, sdt);
        
        Assert.assertEquals(b.getBranches().size(), 3);
        logger.log(Level.FINE, "initial branching: \n{0}", b.getBranches().toString());
    }
   
    @Test
    public void testLoginExample2() {
        final Word<PSymbolInstance> word = Word.fromSymbols(
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 2)),
                new PSymbolInstance(I_LOGOUT),
                new PSymbolInstance(I_LOGIN, 
                    new DataValue(T_UID, 1),
                    new DataValue(T_PWD, 3)));

        DataValue<?>[] uids = DataWords.valsOf(word, T_UID);
        DataValue<?>[] pwds = DataWords.valsOf(word, T_PWD);
        
        assert uids.length == 2;
        assert pwds.length == 2;
        
        Theory tUid = new IntegerEqualityTheory(T_UID);
        Theory tPwd = new IntegerEqualityTheory(T_PWD);
        
        List<DataValue<?>> leftUid = new ArrayList<>();
        leftUid.add(uids[0]);
        
        List<DataValue<?>> leftPwd = new ArrayList<>();
        leftPwd.add(pwds[0]);
        
        List<EnumSet<DataRelation>> retUid = tUid.getRelations(leftUid, uids[0]);
        List<EnumSet<DataRelation>> retPwd = tPwd.getRelations(leftPwd, pwds[1]);
               
        System.out.println("UID: " + Arrays.toString(retUid.toArray()));
        System.out.println("PWD: " + Arrays.toString(retPwd.toArray()));
        
        Assert.assertEquals(retUid.get(0), EnumSet.of(DataRelation.EQ));
        Assert.assertEquals(retPwd.get(0), EnumSet.of(DataRelation.DEFAULT));
    }
    
}
