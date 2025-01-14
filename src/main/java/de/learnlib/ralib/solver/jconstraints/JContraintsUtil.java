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
package de.learnlib.ralib.solver.jconstraints;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.ConstantGuardExpression;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.FalseGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.guards.SumCAtomicGuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public class JContraintsUtil {
	
	/*
	 * A mapping from common Java primitive classes to JConstraints types, which appear in SMT expressions.
	 */
	private static final Map<Class<?>, Type<?>> typeMap = new LinkedHashMap<>();
	static {
		typeMap.put(Object.class, BuiltinTypes.DOUBLE);
		typeMap.put(Integer.class, BuiltinTypes.INTEGER);
		typeMap.put(Long.class, BuiltinTypes.INTEGER);
		typeMap.put(Double.class, BuiltinTypes.DOUBLE);
		typeMap.put(Float.class, BuiltinTypes.FLOAT);
	}
	
	public static <T> Type<T> getJCType(DataValue<T> dv) {
		return getJCType(dv.getType().getBase());
	}
	  
	@SuppressWarnings("unchecked")
	public static <T> Type<T> getJCType(Class<T> cls) {
		if (!typeMap.containsKey(cls))
			throw new RuntimeException("No JConstraints type defined for " + cls.getSimpleName());
		return (Type<T>) typeMap.get(cls);
	}

    public static Expression<Boolean> toExpression(
            LogicalOperator op,
            Map<SymbolicDataValue, Variable> map,
            GuardExpression... expr) {
        
        Expression<Boolean>[] elems = new Expression[expr.length];
        int i = 0;
        for (GuardExpression e : expr) {
            elems[i++] = toExpression(e, map);
        }
        switch (op) {
            case AND:
                return ExpressionUtil.and(elems);
            case OR:
                return ExpressionUtil.or(elems);
            default:
                throw new RuntimeException("Unsupported Operator: " + op);
        }
    }

    public static Expression<Boolean> toExpression(GuardExpression expr) {
        return toExpression(expr, new HashMap<>());
    }   
    
    private static Expression<Boolean> toExpression(GuardExpression expr,
            Map<SymbolicDataValue, Variable> map) {
        if (expr instanceof ConstantGuardExpression) {
        	return toExpression((ConstantGuardExpression) expr, map);
        } if (expr instanceof AtomicGuardExpression && !(expr instanceof SumCAtomicGuardExpression)) {
            return toExpression((AtomicGuardExpression) expr, map);
        } else if (expr instanceof SumCAtomicGuardExpression) {
            return toExpression((SumCAtomicGuardExpression) expr, map);
        } else if (expr instanceof TrueGuardExpression) {
            return ExpressionUtil.TRUE;
        } else if (expr instanceof FalseGuardExpression) {
            return ExpressionUtil.FALSE;
        } else if (expr instanceof Conjunction) {
            Conjunction con = (Conjunction) expr;
            return toExpression(LogicalOperator.AND, map, con.getConjuncts());
        } else if (expr instanceof Disjunction) {
            Disjunction dis = (Disjunction) expr;
            return toExpression(LogicalOperator.OR, map, dis.getDisjuncts());
        } else if (expr instanceof Negation) {
            Negation neg = (Negation) expr;
            return new gov.nasa.jpf.constraints.expressions.Negation(
                    toExpression(neg.getNegated(), map));
        }

        throw new RuntimeException("Unsupported Guard Expression: "
                + expr.getClass().getName());
    }
    
    private static Expression<Boolean> toExpression(ConstantGuardExpression expr, Map<SymbolicDataValue, Variable> map) {
    	Variable cv = getOrCreate(expr.getVariable(), map);
    	Constant cs = toConstant(expr.getConstant());
    	
    	return toAtomicExpression(cv, Relation.EQUALS, cs);
    }
    
    
    private static Expression<Boolean> toExpression(SumCAtomicGuardExpression expr,
            Map<SymbolicDataValue, Variable> map) {

        Variable lv = getOrCreate(expr.getLeft(), map);
        Variable rv = getOrCreate(expr.getRight(), map);
        Expression le;
        Expression re;
        if (expr.getLeftConst() != null)
        	le = gov.nasa.jpf.constraints.expressions.NumericCompound.create(lv, 
        		NumericOperator.PLUS, toConstant(expr.getLeftConst()));
        else {
        	le = lv;
        }
        if (expr.getRightConst() != null)
        	re = gov.nasa.jpf.constraints.expressions.NumericCompound.create(rv, 
        		NumericOperator.PLUS, toConstant(expr.getRightConst()));
        else {
        	re = rv;
        }

        Expression<Boolean> boolExpr = toAtomicExpression(le, expr.getRelation(), re);
        
        return boolExpr;    
    }

    private static Expression<Boolean> toExpression(AtomicGuardExpression expr,
            Map<SymbolicDataValue, Variable> map) {

        Variable lv = getOrCreate(expr.getLeft(), map);
        Variable rv = getOrCreate(expr.getRight(), map);

        
        Expression<Boolean> boolExpr = toAtomicExpression(lv, expr.getRelation(), rv);
        
        return boolExpr;    
    }
   
    private static Expression<Boolean> toAtomicExpression(Expression le, Relation relation, Expression re) {
        switch (relation) {
        case EQUALS:
            return new NumericBooleanExpression(le, NumericComparator.EQ, re);
        case NOT_EQUALS:
            return new NumericBooleanExpression(le, NumericComparator.NE, re);
        case LESSER:
            return new NumericBooleanExpression(le, NumericComparator.LT, re);
        case GREATER:
            return new NumericBooleanExpression(le, NumericComparator.GT, re);
        case LSREQUALS:
        	return new NumericBooleanExpression(le, NumericComparator.LE, re);
        case GREQUALS:
        	return new NumericBooleanExpression(le, NumericComparator.GE, re);

        default:
            throw new UnsupportedOperationException(
                    "Relation " + relation + " is not suported in constraint conversion");
    }
    }

    private static Variable getOrCreate(SymbolicDataValue dv,
            Map<SymbolicDataValue, Variable> map) {
        Variable ret = map.get(dv);
        if (ret == null) {
            ret = new Variable(getJCType(dv), dv.toString());
            map.put(dv, ret);
        }
        return ret;
    }
    
    public static Constant toConstant(DataValue v) {
        Type type = getJCType(v);
        return new Constant( type, type.cast(v.getId()));
    }

    public static Variable toVariable(SymbolicDataValue dv) {
        return new Variable( getJCType(dv), dv.toString());
    }
}
