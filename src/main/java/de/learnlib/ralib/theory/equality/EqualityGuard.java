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

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.guards.SumCAtomicGuardExpression;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;

/**
 *
 * @author falk
 */
public class EqualityGuard extends SDTIfGuard {

    public EqualityGuard(SuffixValue param, SymbolicDataExpression reg) {
        super(param, reg, Relation.EQUALS);
        if (reg == null) {
        	throw new DecoratedRuntimeException("Equality guard over null register").addDecoration("suff param", param);
        }
    }

    @Override
    public String toString() {
        return "(" + this.getParameter().toString()
                + "=" + this.getExpression().toString() + ")";

    }

    public DisequalityGuard toDeqGuard() {
        return new DisequalityGuard(parameter, registerExpr);
    }
    
    public boolean isEqualityWithSDV() {
    	return this.registerExpr instanceof SymbolicDataValue; 
    }

    @Override
    public SDTIfGuard relabel(VarMapping relabelling) {
    	  SymbolicDataValue.SuffixValue sv
          = (SymbolicDataValue.SuffixValue) relabelling.get(parameter);
		  sv = (sv == null) ? parameter : sv;
		  SymbolicDataExpression r = null;
		
		  if (registerExpr.isConstant()) {
		      return new EqualityGuard(sv, registerExpr);
		  } else {
		  	if (relabelling.containsKey(registerExpr.getSDV())) {
		  		r = registerExpr.swapSDV((SymbolicDataValue) relabelling.get(registerExpr.getSDV()));
		  	} else {
		  		r = registerExpr;
		  	}
		  }
        return new EqualityGuard(sv, r);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(parameter);
        hash = 59 * hash + Objects.hashCode(registerExpr);
        hash = 59 * hash + Objects.hashCode(relation);
        hash = 59 * hash + Objects.hashCode(getClass());

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EqualityGuard other = (EqualityGuard) obj;
        if (!Objects.equals(this.registerExpr, other.registerExpr)) {
            return false;
        }
        if (!Objects.equals(this.relation, other.relation)) {
            return false;
        }
        return Objects.equals(this.parameter, other.parameter);
    }

    @Override
    public GuardExpression toExpr() {
    	if (registerExpr instanceof  SymbolicDataValue) {
    		return new AtomicGuardExpression<>(this.registerExpr.getSDV(),
                    Relation.EQUALS, parameter);
    	} else {
    		if (registerExpr instanceof  SumCDataExpression) {
    		return new SumCAtomicGuardExpression<>(this.registerExpr.getSDV(),
    				((SumCDataExpression) this.registerExpr).getConstant(),
                    Relation.EQUALS, parameter, null);
    		} else {
    			throw new RuntimeException("Case not handle for expression "+ registerExpr);
    		}
    	}
    }
    
	public SDTGuard replace(Replacement replacing) {
		SymbolicDataExpression rExpr = replacing.containsKey(this.registerExpr) ? 
				replacing.get(this.registerExpr) : this.registerExpr;
		
		return new EqualityGuard(getParameter(), rExpr);
	}

	@Override
	public Set<SymbolicDataValue> getAllSDVsFormingGuard() {
		return Sets.newHashSet(this.registerExpr.getSDV());
	}  
}
