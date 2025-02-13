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
package de.learnlib.ralib.oracles;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.learning.SymbolicDecisionTree;

/**
 * Container for result of tree queries.
 * 
 * @author falk
 */
public class TreeQueryResult {
    
    private final PIV piv;
    
    private final SymbolicDecisionTree sdt;

    public TreeQueryResult(
            PIV piv, 
            SymbolicDecisionTree sdt) {
        
        this.piv = piv;
        this.sdt = sdt;
    }

    /**
     * @return the piv
     */
    public PIV getPiv() {
        return piv;
    }

    /**
     * @return the sdt
     */
    public SymbolicDecisionTree getSdt() {
        //System.out.println("getSdt() " + sdt);
        return sdt;
    }

    public String toString() {
    	return " PIV: " + piv + " \n SDT: " + this.sdt;
    }
}
