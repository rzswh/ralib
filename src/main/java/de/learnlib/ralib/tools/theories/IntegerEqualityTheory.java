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
package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.equality.EqualityTheory;
import de.learnlib.ralib.theory.equality.EqualityDeterminizer;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

/**
 *
 * @author falk
 */
public class IntegerEqualityTheory  extends EqualityTheory<Integer> implements TypedTheory<Integer> {


    private DataType type = null;

    public IntegerEqualityTheory() {
    }
    
    public IntegerEqualityTheory(DataType t) {
        this.type = t;
    }

    // Modify: dflasher
    // Allocate minimal possible value at the cost of allocating a small hash table
    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        boolean slots[] = new boolean[vals.size()];
        for (DataValue<Integer> d : vals) {
            // dv = Math.max(dv, d.getId());
            if (d.getId() >= 0 && d.getId() < slots.length) {
                slots[d.getId()] = true;
            }
        }
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i]) {
                return new DataValue<>(type, i);
            }
        }

        return new DataValue<>(type, slots.length);
    }
    
    public void setType(DataType type) {
        this.type = type;
    }
    
    public DataType getType() {
    	return this.type;
    }
    

    @Override
    public void setUseSuffixOpt(boolean useit) { 
        this.useNonFreeOptimization = useit;
    }

    @Override
    public void setCheckForFreshOutputs(boolean doit) {
        super.setFreshValues(doit);
    }

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {
        
        // TODO: add constants ...
        
        ArrayList<DataValue<Integer>> ret = new ArrayList<>(vals);
        ret.add(getFreshValue(vals));
        return ret;
    }

	  public Determinizer<Integer> getDeterminizer() {
	  	return new EqualityDeterminizer<Integer>(this);
	  }

	@Override
    public List<EnumSet<DataRelation>> getRelations(
            List<DataValue<Integer>> left, DataValue<Integer> right) {
        
        List<EnumSet<DataRelation>> ret = new ArrayList<>();
        left.stream().forEach((dv) -> {
            ret.add(dv.getId().equals(right.getId()) ? 
                    EnumSet.of(DataRelation.EQ) :  
                    EnumSet.of(DataRelation.DEFAULT));
        });
        
        return ret;
    }

	@Override
	public Class<Integer> getDomainType() {
		return Integer.class;
	}
}
