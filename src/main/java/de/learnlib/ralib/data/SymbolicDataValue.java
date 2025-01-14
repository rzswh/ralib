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
package de.learnlib.ralib.data;

import java.util.Objects;

/**
 * Symbolic Data Values (Parameters, registers, etc.).
 *
 * @author falk
 */
public abstract class SymbolicDataValue extends DataValue<Integer> implements SymbolicDataExpression{

	/**
	 * Constructs a new symbolic data expression where the register is swapped
	 * by a new one. 
	 */
	public SymbolicDataValue getSDV() {
		return this;
	}
	
    public static final class Parameter extends SymbolicDataValue {

        public Parameter(DataType dataType, int id) {
            super(dataType, id);
        }

        public boolean equals(Parameter other) {
            return (this.getType().equals(other.getType()) && this.getId().equals(other.getId()));

        }
    };

    public static final class Register extends SymbolicDataValue {

        public Register(DataType dataType, int id) {
            super(dataType, id);
        }
        
        public boolean equals(Parameter other) {
            return (this.getType().equals(other.getType()) && this.getId().equals(other.getId()));

        }
    };

    public static final class Constant extends SymbolicDataValue {

        public Constant(DataType dataType, int id) {
            super(dataType, id);
        }
    };
    
    public static final class SumConstant extends SymbolicDataValue {
    	public SumConstant(DataType dataType, int id) {
            super(dataType, id);
        }
    }
    
    public static final class TempConstant extends SymbolicDataValue{

    	public TempConstant(DataType dataType, int id) {
            super(dataType, id);
        }
    };

    public static final class SuffixValue extends SymbolicDataValue {

        public SuffixValue(DataType dataType, int id) {
            super(dataType, id);
        }
    };

    private SymbolicDataValue(DataType dataType, int id) {
        super(dataType, id);
    }

    public String toStringWithType() {
        return this.toString() + ":" + this.type.getName();
    }

    @Override
    public String toString() {
        String s = "";
        if (this.isParameter()) {
            s += "p";
        } else if (this.isRegister()) {
            s += "r";
        } else if (this.isSuffixValue()) {
            s += "s";
        } else if (this.isConstant()) {
            s += "c";
        } else if (this instanceof TempConstant) {
        	s += "tempc";
        }  if (this instanceof SumConstant) {
        	s += "sumc";
        }
        return s + this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SymbolicDataValue other = (SymbolicDataValue) obj;
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.getClass());
        return hash;
    }

    public boolean isRegister() {
        return this.getClass().equals(Register.class);
    }

    public boolean isParameter() {
        return this.getClass().equals(Parameter.class);
    }

    public boolean isConstant() {
        return this.getClass().equals(Constant.class);
    }

    public boolean isSuffixValue() {
        return this.getClass().equals(SuffixValue.class);
    }
    
    public DataValue<?> instantiateExprForValuation(Mapping<SymbolicDataValue, DataValue<?>> valuation) {
    	return valuation.get(this);
    }

}
