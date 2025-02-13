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
package de.learnlib.ralib.automata.guards;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author falk
 * @param <Left>
 * @param <Right>
 */
public class AtomicGuardExpression<Left extends SymbolicDataValue, Right extends SymbolicDataValue> extends GuardExpression {

    protected final Left left; 
        
    protected final Relation relation;

    protected final Right right;
    
    public AtomicGuardExpression(Left left, Relation relation, Right right) {
        this.left = left;
        this.relation = relation;
        this.right = right;
    }
    
    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {        
        
        DataValue lv = val.get(left);
        DataValue rv = val.get(right);
        
        //System.out.println(this);
        //System.out.println(val.toString());
        
        assert lv != null && rv != null;
                        
        boolean isSatisfied = isSatisfied(lv, rv, relation);
        return isSatisfied;
    }
    
    protected boolean isSatisfied(DataValue lv, DataValue rv, Relation relation) {
    	 switch (relation) {
         case EQUALS: 
             return lv.equals(rv);
         case NOT_EQUALS: 
             return !lv.equals(rv);

         case GREATER:
         case LESSER:
         case GREQUALS:
         case LSREQUALS:
             return numCompare(lv, rv, relation);
        
         case NOT_SUCC:
         case SUCC:
         case IN_WIN:
         case NOT_IN_WIN:
             return succ(lv, rv, relation);
             
         default:
             throw new UnsupportedOperationException(
                     "Relation " + relation + " is not suported in guards");
     }
    }
               
    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        SymbolicDataValue newLeft = (SymbolicDataValue) relabelling.get(left);
        if (newLeft == null) {
            newLeft = left;
        }
        SymbolicDataValue newRight = (SymbolicDataValue) relabelling.get(right);
        if (newRight == null) {
            newRight = right;
        }
        
        return new AtomicGuardExpression(newLeft, relation, newRight);
    }

    
    @Override
    public String toString() {
        return "(" + left + relation + right + ")";
    }

    public Left getLeft() {
        return left;
    }

    public Right getRight() {
        return right;
    }    

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        vals.add(left);
        vals.add(right);
    }

    public Relation getRelation() {
        return relation;
    }

    private boolean numCompare(DataValue l, DataValue r, Relation relation) {        
        if (!l.getType().equals(r.getType())) {
            return false;
        }
        Comparable lc = (Comparable) l.getId();
        int result = lc.compareTo(r.getId());        
        switch (relation) {
            case LESSER:
                return result < 0;
            case GREATER:
                return result > 0;
            case LSREQUALS:
                return result <= 0;
            case GREQUALS:
                return result >= 0;
               default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");   
        }
    }
    
    private boolean succ(DataValue lv, DataValue rv, Relation relation) {
        if (!(lv.getId() instanceof Number) || !(rv.getId() instanceof Number)) {
			return false;
    	}	else {
    		double val1 = ((Number) lv.getId()).doubleValue();
    		double val2 = ((Number) rv.getId()).doubleValue();
    		switch(relation) {
    		case IN_WIN: return val2 > val1 + 1 && val2 <= val1 + 100;
    		case NOT_IN_WIN: return val2 <= val1 + 1 || val2 > val1 + 100;
    		case SUCC: return val2 == val1+1;
    		case NOT_SUCC: return val2 != val1+1;
    		default:
    	    throw new UnsupportedOperationException(
                    "Relation " + relation + " is not suported in succ guards");
    		}
    }
        }
    

    @Override
    protected void getAtoms(Collection<AtomicGuardExpression> vals) {
        vals.add(this);
    }     
}
