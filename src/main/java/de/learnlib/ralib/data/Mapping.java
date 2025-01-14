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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author falk
 * @param <K>
 * @param <V>
 */
public class Mapping<K, V extends DataValue<?>> extends LinkedHashMap<K, V>
        implements Iterable<Map.Entry<K, V>> {
	
	public Mapping(){}
	public Mapping(Mapping ...mappings){
		for (Mapping mapping : mappings) {
			this.putAll(mapping);
		}
	}

    /**
     * returns the contained values of some type.
     * 
     * @param <T>
     * @param type the type
     * @return 
     */
    public <T> Collection<DataValue<T>> values(DataType type) {
        List<DataValue<T>> list = new ArrayList<>();
        for (DataValue<?> v : values()) {
            if (v.type.equals(type)) {
                list.add((DataValue<T>) v);
            }
        }
        return list;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return this.entrySet().iterator();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Mapping<?, ?> other = (Mapping<?, ?>) obj;
        return other.entrySet().equals(entrySet());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash * this.entrySet().hashCode();
    }

//    @Override
//    public V get(Object key) {
//        V v = super.get(key);
//        if (v == null) {
//            throw new IllegalStateException();
//        }
//        return v;
//    }
    
    
    public String toString(String map) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Map.Entry<K,V> e : entrySet()) {
            sb.append(e.getKey()).append(map).append(e.getValue()).append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    
    public Set<K> getAllKeys(V value) {
        Set<K> retKeySet = new LinkedHashSet();
        for (K key : this.keySet()) {
            //log.log(Level.FINEST,"key = " + K);
            //log.log(Level.FINEST,"value = " + this.get(key).toString());
            if (this.get(key).equals(value)){
                //log.log(Level.FINEST,this.get(key).toString() + " equals " + value.toString());
                retKeySet.add(key);
            }
        }   
        return retKeySet;
    }
    
    
    @Override
    public String toString() {
        return toString(">");
    }
        
    
}
