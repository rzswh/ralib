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
package de.learnlib.ralib.words;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import net.automatalib.words.Word;

/**
 * static helper methods for data words.
 * 
 * @author falk
 */
public final class DataWords {

    /**
     * returns sequence of data values of a specific type in a data word.
     * 
     * @param <T>
     * @param word
     * @param t
     * @return 
     */
    public static <T> DataValue<T>[] valsOf(Word<PSymbolInstance> word, DataType t) {
        List<DataValue<T>> vals = new ArrayList<>();
        for (PSymbolInstance psi : word) {
            for (DataValue d : psi.getParameterValues()) {
                if (d.getType().equals(t)) {
                    vals.add(d);
                }
            }
        }
        return vals.toArray(new DataValue[] {});
    }
    
    /**
     * returns sequence of all data values in a data word.
     * 
     * @param word
     * @return 
     */
    public static DataValue[] valsOf(Word<PSymbolInstance> word) {
        DataValue[] vals = new DataValue[DataWords.paramLength(actsOf(word))];
        int i = 0;
        for (PSymbolInstance psi : word) {
            for (DataValue p : psi.getParameterValues()) {
                vals[i++] = p;
            }
        }
        return vals;
    }
    
    /**
     * returns sequence of all data types in a data word.
     * 
     * @param word
     * @return 
     */
    public static DataType[] typesOf(Word<ParameterizedSymbol> word) {
        DataType[] types = new DataType[DataWords.paramLength(word)];
        int i = 0;
        for (ParameterizedSymbol ps : word) {
            for (DataType t : ps.getPtypes()) {
                types[i++] = t;
            }
        }
        return types;
    }
    
    /**
     * returns an insertion-ordered set of unique data values of some type in a data word from first to last.
     * 
     * @param <T>
     * @param word
     * @param t
     * @return 
     */
    public static <T> LinkedHashSet<DataValue<T>> valSet(Word<PSymbolInstance> word, DataType t) {
    	LinkedHashSet<DataValue<T>> vals = new LinkedHashSet<>();
        for (PSymbolInstance psi : word) {
            for (DataValue d : psi.getParameterValues()) {
                if (d.getType().equals(t)) {
                    vals.add(d);
                }
            }
        }
        return vals;
    }
    
    /**
     * 
     * @param <T>
     * @param in
     * @return 
     */
    @SafeVarargs
    public static <T> LinkedHashSet<DataValue<T>> joinValsToSet(Collection<DataValue<T>> ... in) {
    	LinkedHashSet<DataValue<T>> vals = new LinkedHashSet<>();    
        for (Collection<DataValue<T>> s : in) {
            vals.addAll(s);
        }
        return vals;
    }
    
    
    /**
     * 
     * @param <T>
     * @param in
     * @return 
     */
    @SafeVarargs
	public static <T> List<DataValue<T>> joinValsToList(Collection<DataValue<T>> ... in) {
    	return Stream.of(in).
    			flatMap(coll -> coll.stream()).
    			collect(Collectors.toList());
    }
    
    /**
     * returns an insertion-ordered set of all unique data values in a data word from first to last.
     * 
     * @param word
     * @return 
     */
    public static LinkedHashSet<DataValue> valSet(Word<PSymbolInstance> word) {
    	LinkedHashSet<DataValue> valset = new LinkedHashSet<>();
        for (PSymbolInstance psi : word) {
            valset.addAll(Arrays.asList(psi.getParameterValues()));
        }
        return valset;
    }    
     
    /**
     * returns sequence of actions in a data word.
     * @param word
     * @return 
     */
    public static Word<ParameterizedSymbol> actsOf(
            Word<PSymbolInstance> word) {
        ParameterizedSymbol[] symbols = new ParameterizedSymbol[word.length()];        
        int idx = 0;
        for (PSymbolInstance psi : word) {
            symbols[idx++] = psi.getBaseSymbol();
        }        
        return Word.fromSymbols(symbols);
    }
    
    /**
     * instantiates a data word from a sequence of actions and
     * a valuation.
     * 
     * @param actions
     * @param dataValues
     * @return 
     */
    public static Word<PSymbolInstance> instantiate(
            Word<ParameterizedSymbol> actions, 
            Map<Integer, ? extends DataValue> dataValues) {
       
        PSymbolInstance[] symbols = new PSymbolInstance[actions.length()];
        int idx = 0;
        int pid = 1;
        for (ParameterizedSymbol ps : actions) {
            DataValue[] pvalues = new DataValue[ps.getArity()];
            for (int i = 0; i < ps.getArity(); i++) {
                pvalues[i] = dataValues.get(pid++);
                //log.log(Level.FINEST,pvalues[i].toString());
            }
            symbols[idx++] = new PSymbolInstance(ps, pvalues);
        }
        for (PSymbolInstance p : symbols) {
            //log.log(Level.FINEST,p.toString());
        }
        return Word.fromSymbols(symbols);
    }

    /**
     * returns the number of data values in a sequence of actions.
     * 
     * @param word
     * @return 
     */
    public static int paramLength(Word<ParameterizedSymbol> word) {
        int length = 0;
        for (ParameterizedSymbol psi : word) {
            length += psi.getArity();
        }
        return length;
    }
    
        
    private DataWords() {        
    }    
}
