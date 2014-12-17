/*
 * Copyright (C) 2014 falk.
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

package de.learnlib.ralib.example.login;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.DataExpression;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public final class LoginAutomatonExample {
    
    public static final DataType T_UID = new DataType("T_uid", Integer.class) {};
    public static final DataType T_PWD = new DataType("T_pwd", Integer.class) {};

    public static final ParameterizedSymbol I_REGISTER = 
            new ParameterizedSymbol("register", new DataType[] {T_UID, T_PWD}); 
    
    public static final ParameterizedSymbol I_LOGIN = 
            new ParameterizedSymbol("login", new DataType[] {T_UID, T_PWD});
    
    public static final ParameterizedSymbol I_LOGOUT = 
            new ParameterizedSymbol("logout", new DataType[] {});
    
    public static final RegisterAutomaton AUTOMATON = buildAutomaton();
    
    private LoginAutomatonExample() {      
    }
    
    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();        
        
        // locations
        RALocation l0 = ra.addInitialState();
        RALocation l1 = ra.addState();
        RALocation l2 = ra.addState();
        
        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register rUid = rgen.next(T_UID);
        Register rPwd = rgen.next(T_PWD);        
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter pUid = pgen.next(T_UID);
        Parameter pPwd = pgen.next(T_PWD);
        
        // guards
        Variable x1 = new Variable(BuiltinTypes.SINT32, "x1");
        Variable x2 = new Variable(BuiltinTypes.SINT32, "x2");
        Variable p1 = new Variable(BuiltinTypes.SINT32, "p1");
        Variable p2 = new Variable(BuiltinTypes.SINT32, "p2");
        Expression<Boolean> expression = new PropositionalCompound(
                new NumericBooleanExpression(x1, NumericComparator.EQ, p1), 
                LogicalOperator.AND, 
                new NumericBooleanExpression(x2, NumericComparator.EQ, p2)); 
        
        Map<SymbolicDataValue, Variable> mapping = new HashMap<>();
        mapping.put(rUid, x1);
        mapping.put(rPwd, x2);
        mapping.put(pUid, p1);
        mapping.put(pPwd, p2);
                
        DataExpression<Boolean> condition = 
                new DataExpression<>(expression, mapping);
        
        IfGuard   okGuard    = new IfGuard(condition);
        ElseGuard errorGuard = new ElseGuard(Collections.singleton(okGuard));
        ElseGuard trueGuard  = new ElseGuard(Collections.EMPTY_SET);        
        
        // assignments
        VarMapping<Register, SymbolicDataValue> copyMapping = new VarMapping<>();
        copyMapping.put(rUid, rUid);
        copyMapping.put(rPwd, rPwd);
        
        VarMapping<Register, SymbolicDataValue> storeMapping = new VarMapping<>();
        storeMapping.put(rUid, pUid);
        storeMapping.put(rPwd, pPwd);
        
        Assignment copyAssign  = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);
                
        // initial location
        ra.addTransition(l0, I_REGISTER, new InputTransition(trueGuard, I_REGISTER, l0, l1, storeAssign));        
        
        // reg. location
        ra.addTransition(l1, I_LOGIN, new InputTransition(okGuard, I_LOGIN, l1, l2, copyAssign));        
        ra.addTransition(l1, I_LOGIN, new InputTransition(errorGuard, I_LOGIN, l1, l1, copyAssign));        
        
        // login location
        ra.addTransition(l1, I_LOGOUT, new InputTransition(trueGuard, I_LOGOUT, l2, l1, copyAssign));        
        
        return ra;
    }
    
}