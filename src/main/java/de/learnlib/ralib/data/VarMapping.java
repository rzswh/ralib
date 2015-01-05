/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.data;

/**
 * maps symbolic data values to symbolic data values.
 * 
 * 
 * @author falk
 * @param <K>
 * @param <V>
 */
public class VarMapping<K extends SymbolicDataValue, V extends SymbolicDataValue> 
extends Mapping<K, V> {
    
    public VarMapping(SymbolicDataValue ... kvpairs) {
        for (int i=0; i<kvpairs.length; i+= 2) {
            K key = (K) kvpairs[i];
            V val = (V) kvpairs[i+1];
            put(key, val);
        }
        
    }
    
   
}
