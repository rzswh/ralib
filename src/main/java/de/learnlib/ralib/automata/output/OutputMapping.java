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
package de.learnlib.ralib.automata.output;

import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * An output mapping encodes the guard of an output transition in a 
 * more straight-forward form in the case of guards with equalities. 
 * 
 * - Fresh parameters have to be unequal to values stored in registers.
 * - A mapping encodes equalities.
 * 
 * @author falk
 */
public class OutputMapping  {
     
    private final Collection<Parameter> fresh;
    
    private final VarMapping<Parameter, ? extends SymbolicDataExpression> piv;
    
    public OutputMapping(Collection<Parameter> fresh, 
            VarMapping<Parameter, ? extends SymbolicDataExpression> piv) {
        this.fresh = fresh;
        this.piv = piv;
    }
    
    public OutputMapping() {
        this(new ArrayList<Parameter>(), new VarMapping<Parameter, SymbolicDataValue>());
    }

    public OutputMapping(Parameter fresh) {
        this(Collections.singleton(fresh), new VarMapping<Parameter, SymbolicDataValue>());
    }
    
    public OutputMapping(Parameter key, Register value) {
        this(new ArrayList<Parameter>(), new VarMapping<Parameter, SymbolicDataValue>(key, value));
    }

    public OutputMapping(VarMapping<Parameter, SymbolicDataValue> outputs) {
        this(new ArrayList<Parameter>(), outputs);
    }

    public Collection<Parameter> getFreshParameters() {
        return fresh;
    }
    
    public VarMapping<Parameter, ? extends  SymbolicDataExpression> getOutput() {
        return piv;
    }

    @Override
    public String toString() {
        return "F:" + Arrays.toString(fresh.toArray()) + ", M:" + piv.toString();
    }

}
