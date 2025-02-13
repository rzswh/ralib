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
package de.learnlib.ralib.theory;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;


/**
 *
 * @author falk
 * @param <T>
 */
public interface Theory<T> {
      

    /**
     * Returns a fresh data value.
     * 
     * @param vals
     * @return a fresh data value of type T 
     */
    public DataValue<T> getFreshValue(List<DataValue<T>> vals);
    
    /** 
     * Implements a tree query for this theory. 
     * <p>
     * The method chooses possible guards and values for the next suffix parameter to be insantiated.
     * For each value it recursively calls {@code treeQuery}, for follow-up suffix parameters.
     * The resulting SDTs are merged under their respective guards into a single SDT. 
     * 
     * @param prefix prefix word. 
     * @param suffix suffix word.
     * @param values found values for complete word (pos -> dv)
     * @param piv memorable data values of the prefix (dv <-> itr) 
     * @param constants 
     * @param suffixValues map of already instantiated suffix 
     * data values (sv -> dv)
     * @param oracle the tree oracle in control of this query
     * @param solver TODO
     * @return a symbolic decision tree and updated piv 
     */    
    public SDT treeQuery(
            Word<PSymbolInstance> prefix,             
            GeneralizedSymbolicSuffix suffix,
            WordValuation values, 
            PIV piv,
            Constants constants,
            SuffixValuation suffixValues,
            SDTConstructor oracle,
            ConstraintSolver solver,
            IOOracle traceOracle);
 
    /**
     * returns all next data values to be tested (for vals).
     * 
     * @param vals
     * @return 
     */
    public Collection<DataValue<T>> getAllNextValues(List<DataValue<T>> vals);
    
    /**
     * returns an ordered list of potential values
     * 
     * @param vals
     * @return
     */
    public default List<DataValue<T>> getPotential(List<DataValue<T>> vals) {
    	return Collections.emptyList();
    }
    
    /**
     * Instantiates a determinizer for the given theory which is used for handling fresh values.
     * 
     * @return the determinizer instance 
     * @throws exception if fresh values are not supported
     */
    public default Determinizer<T> getDeterminizer() {
    	throw new NotImplementedException("Determinizer not implemented for theory");
    }
    
    
    /**
     * Instantiates the guard by providing a DataValue satisfying the guard, or null, if no such value exists.
     * <p>
     * For theories with fresh value support the DataValue subtype should reflect the guard constraints it satisfies.
     * For example, {@link IntervalDataValue} reflects inequality, {@link FreshValue} reflects new value.
	 * This information is required by the determinizer and symbolic trace canonizer. 
	 * 
     * 
     * @param prefix
     * @param ps
     * @param piv
     * @param pval
     * @param constants
     * @param guard
     * @param param
     * @param oldDvs
     * @return 
     */
    public @Nullable DataValue instantiate(Word<PSymbolInstance> prefix, 
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            Constants constants,
            SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs);
    
    /**
     * return set of relations that hold between left and right
     * 
     * @param left
     * @param right
     * @return 
     */
    public List<EnumSet<DataRelation>> getRelations(List<DataValue<T>> left, DataValue<T> right);
    
    /**
     * return set of all recognized relations
     */
    public EnumSet<DataRelation> recognizedRelations();
    
    /**
     * Returns disjunction and conjunction logic for sdt guards.
     */
    public SDTGuardLogic getGuardLogic();
}
