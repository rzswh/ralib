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
package de.learnlib.ralib.example.sdts;

import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGIN;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_LOGOUT;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.I_REGISTER;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_PWD;
import static de.learnlib.ralib.example.login.LoginAutomatonExample.T_UID;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import net.automatalib.words.Word;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.example.sdts.LoginExampleSDT.SDTClass;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public class LoginExampleTreeOracle implements TreeOracle {

    public static enum State {

        INIT, REGISTER, LOGIN, ERROR
    };
    
    private final Register rUid;
    private final Register rPwd;
    
    public LoginExampleTreeOracle() {
        RegisterGenerator gen = new RegisterGenerator();
        rUid = gen.next(T_UID);
        rPwd = gen.next(T_PWD);       
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix) {

        if (prefix.length() < 1) {
            return new TreeQueryResult(new PIV(),
                    new LoginExampleSDT(SDTClass.REJECT, suffix, new LinkedHashSet<Register>()));
        }

        DataValue uid = null;
        DataValue pwd = null;

        int idx = 0;
        State state = State.INIT;
        while (idx < prefix.length()) {
            PSymbolInstance psi = prefix.getSymbol(idx);

            switch (state) {
                case INIT:
                    if (I_REGISTER.equals(psi.getBaseSymbol())) {
                        uid = psi.getParameterValues()[0];
                        pwd = psi.getParameterValues()[1];
                        state = State.REGISTER;
                    }
                    break;
                case REGISTER:
                    if (I_LOGIN.equals(psi.getBaseSymbol())) {
                        if (uid.equals(psi.getParameterValues()[0])
                                && pwd.equals(psi.getParameterValues()[1])) {
                            state = State.LOGIN;
                        }
                    } else {
                        state = State.ERROR;
                    }
                    break;
                case LOGIN:
                    if (I_LOGOUT.equals(psi.getBaseSymbol())) {
                        state = State.REGISTER;
                    } else {
                        state = State.ERROR;
                    }
                    break;
            }

            if (state == State.ERROR) {
                return new TreeQueryResult(new PIV(),
                        new LoginExampleSDT(SDTClass.REJECT, suffix, new LinkedHashSet<Register>()));
            }

            idx++;
        }

        SDTClass clazz = SDTClass.REJECT;
        switch (state) {
            case REGISTER:
                if (suffix.getActions().length() == 1) {
                    clazz = SDTClass.LOGIN;
                }
                break;
            case LOGIN:
                switch (suffix.getActions().length()) {
                    case 0:
                        clazz = SDTClass.ACCEPT;
                        break;
                    case 2:
                        clazz = SDTClass.LOGIN;
                        break;
                }
                break;
        }

        PIV piv = new PIV();
        SymbolicDataValueGenerator.ParameterGenerator pgen = 
                new SymbolicDataValueGenerator.ParameterGenerator();
        if (uid != null && clazz == SDTClass.LOGIN) {
            piv.put(pgen.next(T_UID), rUid);
            piv.put(pgen.next(T_PWD), rPwd);
        }

        return new TreeQueryResult(piv,
                new LoginExampleSDT(clazz, suffix, new LinkedHashSet<Register>()));
    }

    
    private Word<PSymbolInstance> getDefaultExtension(
            Word<PSymbolInstance> prefix, ParameterizedSymbol ps) {

        DataValue[] params = new DataValue[ps.getArity()];
        int base = DataWords.paramLength(DataWords.actsOf(prefix)) + 1;
        for (int i = 0; i < ps.getArity(); i++) {
            params[i] = new DataValue(ps.getPtypes()[i], base + i);
        }
        return prefix.append(new PSymbolInstance(ps, params));
    }

    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree... sdts) {
        
        Map<Word<PSymbolInstance>, TransitionGuard> branches = new LinkedHashMap<Word<PSymbolInstance>, TransitionGuard>();
        Word<ParameterizedSymbol> acts = DataWords.actsOf(prefix);
        DataValue[] vals = DataWords.valsOf(prefix);
        
        if (sdts.length > 0 && ps.equals(I_LOGIN) && acts.length() == 1 && 
                acts.firstSymbol().equals(I_REGISTER)) {
            
            //System.out.println("+++++ special case");
            
            SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
            SymbolicDataValue.Parameter pUid = pgen.next(T_UID);
            SymbolicDataValue.Parameter pPwd = pgen.next(T_PWD);

            // guards
        GuardExpression condition = new Conjunction(
                new AtomicGuardExpression(rUid, Relation.EQUALS, pUid),
                new AtomicGuardExpression(rPwd, Relation.EQUALS, pPwd));
        
        GuardExpression elseCond = new Disjunction(
                new AtomicGuardExpression(rUid, Relation.NOT_EQUALS, pUid),
                new AtomicGuardExpression(rPwd, Relation.NOT_EQUALS, pPwd));

            TransitionGuard ifGuard = new TransitionGuard(condition);
            TransitionGuard elseGuard = new TransitionGuard(elseCond);

            branches.put(getDefaultExtension(prefix, ps), elseGuard);
            branches.put(prefix.append(new PSymbolInstance(ps, vals)), ifGuard);
            
        } else {
            
            TransitionGuard guard = new TransitionGuard();
            branches.put(getDefaultExtension(prefix, ps), guard);
            
        }
        
        return new LoginExampleBranching(branches);
    }

    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, Branching current, 
            PIV piv, SymbolicDecisionTree... sdts) {

        return getInitialBranching(prefix, ps, piv, sdts);
    }

}
