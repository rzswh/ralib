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

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import net.automatalib.words.Word;

import org.testng.annotations.Test;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
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
import org.testng.Assert;

/**
 *
 * @author falk
 */
public class UntypedBranchingTest extends RaLibTestSuite {
    
    public UntypedBranchingTest() {
    }


    @Test
    public void testBranching() {

        RegisterAutomatonImporter loader = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/login.xml");

        RegisterAutomaton model = loader.getRegisterAutomaton();
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
        
        ParameterizedSymbol reg = new InputSymbol(
                "IRegister", new DataType[] {intType, intType});

        ParameterizedSymbol log = new InputSymbol(
                "ILogin", new DataType[] {intType, intType});    
    
        ParameterizedSymbol ok = new OutputSymbol(
                "OOK", new DataType[] {});    

        DataValue u = new DataValue(intType, 0);
        DataValue p = new DataValue(intType, 1);
        
        Word<PSymbolInstance> prefix = Word.fromSymbols(
                new PSymbolInstance(reg, new DataValue[] {u, p}),
                new PSymbolInstance(ok, new DataValue[] {}));

        Word<PSymbolInstance> suffix = Word.fromSymbols(
                new PSymbolInstance(log, new DataValue[] {u, p}),
                new PSymbolInstance(ok, new DataValue[] {}));        
        
        GeneralizedSymbolicSuffix symSuffix = new GeneralizedSymbolicSuffix(
                prefix, suffix, new Constants(), teachers);
        
        logger.log(Level.FINE, "{0}", prefix);
        logger.log(Level.FINE, "{0}", suffix);
        logger.log(Level.FINE, "{0}", symSuffix); 
        
        TreeQueryResult res = mto.treeQuery(prefix, symSuffix);        
        logger.log(Level.FINE, "SDT: {0}", res.getSdt());
       
        SymbolicDecisionTree sdt = res.getSdt();

        ParameterGenerator pgen = new ParameterGenerator();
        RegisterGenerator rgen = new RegisterGenerator();        
        
        Parameter p1 = pgen.next(intType);
        Parameter p2 = pgen.next(intType);
        Register r1 = rgen.next(intType);
        Register r2 = rgen.next(intType);
        
        VarMapping map = new VarMapping();
        map.put(r1, r2);
        map.put(r2, r1);
        
        PIV piv = new PIV();
        piv.put(p2, r1);
        piv.put(p1, r2);
        
        sdt = sdt.relabel(map);
        
        Branching bug2 = mto.getInitialBranching(prefix, log, new PIV());        
        bug2 = mto.updateBranching(prefix, log, bug2, piv, sdt);        

        // This set had only one word, there should be three    
        Assert.assertEquals(bug2.getBranches().size(), 3);
        
    }

}
