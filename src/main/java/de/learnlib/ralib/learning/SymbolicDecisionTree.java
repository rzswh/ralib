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
package de.learnlib.ralib.learning;


import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.solver.ConstraintSolver;

/**
 * This interface describes the methods that are needed in a symbolic decision
 * tree during learning.
 * 
 * @author falk
 */
public interface SymbolicDecisionTree {
    
    /**
     * checks if the tree is equivalent to {@code other} tree under a renaming from {@code other} to {@code this}  
     * 
     * @param other
     * @param renaming
     * @param solver constraint solver to use to check for equivalence
     * @return 
     */
    public boolean isEquivalent(SymbolicDecisionTree other, VarMapping renaming, ConstraintSolver solver);
    
    /**
     * apply relabeling to tree and return a renamed tree.
     * 
     * @param relabeling
     * @return 
     */
    public SymbolicDecisionTree relabel(VarMapping relabeling);
    
    public SymbolicDecisionTree replace(Replacement relabeling);
    
    /**
     * 
     * @return 
     */
    //public Set<SymbolicDataValue.Register> getRegisters();
    
    /**
     * true if all paths in this tree are accepting
     * 
     * @return 
     */
    public boolean isAccepting();
}
