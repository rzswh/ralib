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
package de.learnlib.ralib.automata.xml;


import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.guards.SumCAtomicGuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SumConstant;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 *
 * @author fh
 */
public class ExpressionParser {
    
    
    private final String expLine;
    private final Map<String, SymbolicDataValue> pMap;
      
    private GuardExpression predicate;
	private SumConstants consts;
    
    public ExpressionParser(String exp, Map<String, SymbolicDataValue> pMap, SumConstants consts) {
        expLine = exp.trim();
        this.pMap = pMap;
        this.consts = consts;
        
        buildExpression();
    }
    
    private void buildExpression() 
    {
        this.predicate = buildDisjunction(expLine);
    }
    
    private GuardExpression buildDisjunction(String dis) {
        StringTokenizer tok = new StringTokenizer(dis, "||");
        if (tok.countTokens() < 2) {
            return buildConjunction(dis);
        }
        List<GuardExpression> disjuncts = new ArrayList<>();        
        while (tok.hasMoreTokens()) {
            disjuncts.add(buildConjunction(tok.nextToken().trim()));
        }
        return new Disjunction(disjuncts.toArray(new GuardExpression[] {}));
    }

    private GuardExpression buildConjunction(String con) {
        StringTokenizer tok = new StringTokenizer(con, "&&");
        if (tok.countTokens() < 2) {
            return buildPredicate(con);
        }
        List<GuardExpression> conjuncts = new ArrayList<>();        
        while (tok.hasMoreTokens()) {
            conjuncts.add(buildPredicate(tok.nextToken().trim()));
        }
        return new Conjunction(conjuncts.toArray(new GuardExpression[] {}));            
    }

    private GuardExpression buildPredicate(String pred) 
    {
        
        pred = pred.replace("!=", "!!");
        if (pred.trim().length() < 1) {
            return new TrueGuardExpression();
        }
        
        Relation relation = null;
        String[] related = null;
        
        if (pred.contains("==")) {
            related = pred.split("==");
            relation = Relation.EQUALS;
        } 
        else if (pred.contains("!!")) {
            related = pred.split("!!");
            relation = Relation.NOT_EQUALS;           
        }
        else if (pred.contains("<")) {
            related = pred.split("<");
            relation = Relation.LESSER;           
        }
        else if (pred.contains(">")) {
            related = pred.split(">");
            relation = Relation.GREATER;           
        }        
        else if (pred.contains("[next]")) {
            related = pred.split("\\[next\\]");
            relation = Relation.SUCC;
        }
        else if (pred.contains("[!next]")) {
            related = pred.split("\\[!next\\]");
            relation = Relation.NOT_SUCC;  
        }
        else if (pred.contains("[in_win]")) {
            related = pred.split("\\[in_win\\]");
            relation = Relation.IN_WIN;
        }
        else if (pred.contains("[!in_win]")) {
            related = pred.split("\\[!in_win\\]");
            relation = Relation.NOT_IN_WIN;  
        }
        
        if (relation == null) {
            throw new IllegalStateException(
                    "this should not happen!!! " + pred + " in " + expLine);
        }
        
        SymbolicDataExpression left = resolve(related[0].trim());
        SymbolicDataExpression right = resolve(related[1].trim());
        if (left instanceof SymbolicDataValue && right instanceof SymbolicDataValue) {
        	return new AtomicGuardExpression((SymbolicDataValue) left, relation, (SymbolicDataValue) right);          
        }
        if (left instanceof SumCDataExpression || right instanceof SumCDataExpression) {
        	SymbolicDataValue leftVar = left.getSDV();
        	DataValue leftConst = (left instanceof SumCDataExpression) ? ((SumCDataExpression) left).getConstant() : null;
        	SymbolicDataValue rightVar = right.getSDV();
        	DataValue rightConst = (right instanceof SumCDataExpression) ? ((SumCDataExpression) right).getConstant() : null;
        	return new SumCAtomicGuardExpression<SymbolicDataValue, SymbolicDataValue>(leftVar, leftConst, relation, rightVar, rightConst);
        }
        throw new UnsupportedOperationException("Unable to resolve expression " + pred);
    }
    
    private SymbolicDataExpression resolve(String expr) {
    	if (expr.contains("+")) {
    		String[] operands = expr.split("\\+");
    		if (operands.length > 2) {
    			throw new UnsupportedOperationException("Currently only supporting sums with two elements");
    		}
    		SymbolicDataExpression op1 = pMap.get(operands[0].trim());
    		SumConstant c = (SumConstant) pMap.get(operands[1].trim());
    		return new SumCDataExpression (op1, this.consts.get(c));
    	} else {
    		return pMap.get(expr);
    	}
    }
    
    /**
     * @return the predicate
     */
    public GuardExpression getPredicate() {
        return predicate;
    }
    
}
