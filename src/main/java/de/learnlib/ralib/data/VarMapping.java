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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * maps symbolic data values to symbolic data values.
 * 
 * 
 * @author falk
 * @param <K>
 * @param <V>
 */
public class VarMapping<K extends SymbolicDataValue, V extends SymbolicDataExpression> extends LinkedHashMap<K,V>
implements Iterable<Map.Entry<K, V>>
//extends Mapping<K, V> 
{
    
    public VarMapping(SymbolicDataValue ... kvpairs) {
        for (int i=0; i<kvpairs.length; i+= 2) {
            K key = (K) kvpairs[i];
            V val = (V) kvpairs[i+1];
            put(key, val);
        }
        
    }
    
    public Iterator<Map.Entry<K, V>> iterator() {
        return this.entrySet().iterator();
    }
    
    /**
     * Returns an inversed mapping.
     */
    public VarMapping inverse() {
    	VarMapping mapping = new VarMapping();
    	for (Map.Entry entry : this) {
    		mapping.put(entry.getValue(), entry.getKey());
    	}
    	return mapping;
    }

    public String toString(String map) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Map.Entry<K,V> e : entrySet()) {
            sb.append(e.getKey()).append(map).append(e.getValue()).append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
