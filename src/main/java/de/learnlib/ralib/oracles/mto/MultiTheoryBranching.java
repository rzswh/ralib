/*
 * Copyright (C) 2015 falk.
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

package de.learnlib.ralib.oracles.mto;

import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryBranching implements Branching {

    public static class Node {
        private final Parameter parameter;
        private final Map<DataValue, Node> next = new HashMap<>();
        private final Map<DataValue, SDTGuard> guards = new HashMap<>();

        public Node(Parameter parameter) {
            this.parameter = parameter;
        }
        
        public Node(Parameter parameter, 
                Map<DataValue, Node> next, 
                Map<DataValue, SDTGuard> guards) {
            this.parameter = parameter;
            this.next.putAll(next);
            this.guards.putAll(guards);
        }
        
//        @Override
//        public String toString() {
//            return ":Node: \n " + parameter.toString() + "\n" 
//                    + "--next on: " + next.keySet().toString() + " -->\n" + next.toString() + "\n"
//                    + "--guards-->\n" + guards.toString() + "\n:End node:";
//        }
//        
            @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String start = parameter.toString();        
        sb.append(start).append("::\n");
        toString(sb, spaces(1));
        return sb.toString();
    }
    
    void toString(StringBuilder sb, String indentation) {
        sb.append(indentation);
        final int childCount = next.keySet().size();
        int count = 1;
        for (Map.Entry<DataValue, Node> e : next.entrySet()) {
            DataValue d = e.getKey();
            SDTGuard g = guards.get(d);
            //TODO: replace lists of guards by guards
            String nextIndent;
            if (count == childCount) {
                nextIndent = indentation + "      ";
            } else {
                nextIndent = indentation + " |    ";
            } 
            if (count > 1) {            
                sb.append(indentation);
            }
            sb.append("-- ").append(g.toString()).append(" (").append(d.toString()).append(") -->\n");
            e.getValue().toString(sb, nextIndent);
            
            count++;
        }
    }

    private String spaces(int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

 
    }
    
    private final Word<PSymbolInstance> prefix;
    
    private final ParameterizedSymbol action;
    
    private final Node node;

    public MultiTheoryBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol action, Node node) {
        this.prefix = prefix;
        this.action = action;
        this.node = node;
    }
    
    
    @Override
    public Map<Word<PSymbolInstance>, TransitionGuard> getBranches() {
//        Set<Word<PSymbolInstance>> words = new HashSet<>();
//        Map<Word<PSymbolInstance>, TransitionGuard> returnMap = new HashMap<>();
//        for (DataValue d : this.node.guards.keySet()) {
//            
//        }
//  
        return null;  
    }
    
    @Override
    public String toString() {
        return "---- Branching for " + action.toString() + " after " + prefix.toString() + " ----\n" + node.toString() + "\n-------------------------------------------------------------------------------------";
    }
    
}