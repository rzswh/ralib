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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionSequenceTransformer;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class Hypothesis extends MutableRegisterAutomaton 
implements AccessSequenceTransformer<PSymbolInstance>, TransitionSequenceTransformer<PSymbolInstance> {

    private final Map<RALocation, Word<PSymbolInstance>> accessSequences = new LinkedHashMap<>();

    private final Map<Transition, Word<PSymbolInstance>> transitionSequences = new LinkedHashMap<>();
    
    public Hypothesis(Constants consts) {
        super(consts);
    }
    
    public void setAccessSequence(RALocation loc, Word<PSymbolInstance> as) {
        accessSequences.put(loc, as);
    }

    public void setTransitionSequence(Transition t, Word<PSymbolInstance> as) {
        transitionSequences.put(t, as);
    }
    
    public Word<PSymbolInstance> getAccessSequence(RALocation location) {
    	return accessSequences.get(location);
    }
    
    @Override
    public Word<PSymbolInstance> transformAccessSequence(Word<PSymbolInstance> word) {
        RALocation loc = getLocation(word);
        return accessSequences.get(loc);
    }

    @Override
    public boolean isAccessSequence(Word<PSymbolInstance> word) {
        return accessSequences.containsValue(word);
    }

    @Override
    public Word<PSymbolInstance> transformTransitionSequence(Word<PSymbolInstance> word) {
        List<Transition> tseq = getTransitions(word);
        if (tseq == null) {
        	return null;
        }
        //System.out.println("TSEQ: " + tseq);
        Transition last = tseq.get(tseq.size() -1);
        return transitionSequences.get(last);        
    }
    
}
