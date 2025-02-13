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
package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Word;

/**
 *
 * @author falk and many others
 */
public class MultiTheoryTreeOracle implements TreeOracle, SDTConstructor {

    private final DataWordOracle oracle;

    private final Constants constants;

    private final Map<DataType, Theory> teachers;

    private final ConstraintSolver solver;

    private IOOracle traceOracle;
    
    private boolean prefixValidationOpt = false;
    
    private static LearnLogger log
            = LearnLogger.getLogger(MultiTheoryTreeOracle.class);

    
    public MultiTheoryTreeOracle(DataWordOracle membershipOracle, @Nullable IOOracle traceOracle,
            Map<DataType, Theory> teachers, Constants constants, 
            ConstraintSolver solver) {
        this.oracle = membershipOracle;
        this.traceOracle = traceOracle;
        this.teachers = teachers;
        this.constants = constants;
        this.solver = solver;
    }

    /**
     * Do not enable this optimization for automata which do not correspond to an IO automata
     * @param enable
     */
    public void setPrefixValidationOpt(boolean enable) {
        this.prefixValidationOpt = enable;
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix) {
        PIV pir = new PIV();
        SDT sdt;
        try {
        	sdt = treeQuery(prefix, suffix,
                new WordValuation(), pir, constants, new SuffixValuation());
        } catch(DecoratedRuntimeException e) {
        	throw e.addDecoration("prefix", prefix).addDecoration("suffix", suffix);
        }
     
//        System.out.println(prefix + " . " + suffix);
//        System.out.println(sdt);
        
        // move registers to 1 ... n
        VarMapping rename = new VarMapping();
        RegisterGenerator gen = new RegisterGenerator();
        Set<Register> regs = sdt.getRegisters();
        PIV piv = new PIV();

        for (Entry<Parameter, Register> e : pir.entrySet()) {
            if (regs.contains(e.getValue())) {
                Register oldReg = e.getValue();
                Register newReg = gen.next(oldReg.getType());
                rename.put(oldReg, newReg);
                piv.put(e.getKey(), newReg);
            }
        }

        TreeQueryResult tqr = new TreeQueryResult(piv, sdt.relabel(rename));
        log.finer("PIV: " + piv);

        return tqr;
    }

    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
            WordValuation values, PIV pir,
            Constants constants,
            SuffixValuation suffixValues) {
        
//        System.out.println("prefix = " + prefix + "   suffix = " + suffix + "    values = " + values);

        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                    suffix.getActions(), values);

            DefaultQuery<PSymbolInstance, Boolean> query
                    = new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();

//            System.out.println("Trace = " + trace.toString() + " >>> "
//                    + (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;

            // return accept / reject as a leaf
        }

        if (prefixValidationOpt) {
            // check the longest concrete prefix which ends with an output symbol
            int prefixLen = 0;
            int valLen = 0;
            for (int i = 0; i < suffix.size(); i++) {
                int localLen = suffix.getActions().getSymbol(i).getArity();
                if (valLen + localLen > values.size()) {
                    break;
                }
                valLen += localLen;
                prefixLen++;
            }
            // Only check in presence of a complete symbol.
            // If there exist extra values that do not fill in a complete suffix symbol, 
            // its longest concrete prefix has just been tested previously.
            if (valLen == values.size()) {
                Word<PSymbolInstance> concSuffixPart = DataWords.instantiate(suffix.getActions().prefix(prefixLen), values);
                boolean ifInputEnd = (prefix.length() + concSuffixPart.length()) % 2 == 1;
                if (!ifInputEnd
                        || (concSuffixPart.length() > 0 && concSuffixPart.lastSymbol().getBaseSymbol().getArity() == 0)) {
                    DefaultQuery<PSymbolInstance, Boolean> query = new DefaultQuery<>(prefix, concSuffixPart);
                    // Query whether oracle rejects this prefix earlier
                    oracle.processQueries(Collections.singleton(query));
                    if (query.getOutput() == false) {
                        // construct a REJECT SDT
                        SDT result = SDTLeaf.REJECTING;
                        for (int i = DataWords.paramLength(suffix.getActions()); i > values.size(); i--) {
                            SuffixValue sv = suffix.getDataValue(i);
                            SuffixValue currentParam = new SuffixValue(sv.getType(), i);
                            result = new SDT(Collections.singletonMap(new SDTTrueGuard(currentParam), result));
                        }
                        // log.fine("Early reject concrete prefix " + query.getInput().toString());
                        // log.finest("Early reject result SDT: " + result.toString());
                        return result;
                    }
                }
            }
        }
        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);

        Theory teach = teachers.get(sd.getType());

        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        SDT sdt = teach.treeQuery(prefix, suffix, values, pir,
                constants, suffixValues, this, solver, this.traceOracle);
        return sdt;
    }

    /**
     * This method computes the initial branching for an SDT. It re-uses
     * existing valuations where possible.
     *
     */
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree... sdts) {

        log.log(Level.FINE,
                "computing initial branching for {0} after {1}",
                new Object[]{ps, prefix});

        //TODO: check if this casting can be avoided by proper use of generics
        //TODO: the problem seems to be 
        //System.out.println("using " + sdts.length + " SDTs");
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i] = (SDTLeaf) sdts[i];
            } else {
                casted[i] = (SDT) sdts[i];
            }
        }

        MultiTheoryBranching mtb = getInitialBranching(
                prefix, ps, piv, new ParValuation(),
                new ArrayList<SDTGuard>(), casted);

        // log.log(Level.FINEST, mtb.toString());

        return mtb;
    }

    @Override
    // get the initial branching for the symbol ps after prefix given a certain tree
    public MultiTheoryBranching getInitialBranching(
            Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            List<SDTGuard> guards, SDT... sdts) {
        Node n;

        if (sdts.length == 0) {
            n = createFreshNode(1, prefix, ps, piv, pval);
            return new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, sdts);
        } else {
            n = createNode(1, prefix, ps, piv, pval, sdts);
            MultiTheoryBranching fluff = new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, sdts);
            return fluff;
        }

    }

    private Node createFreshNode(int i, Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps,
            PIV piv, ParValuation pval) {

        if (i == ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        } else {
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            DataType type = ps.getPtypes()[i - 1];
            log.log(Level.FINEST, "current type: " + type.getName());
            Parameter p = new Parameter(type, i);
            SDTGuard guard = new SDTTrueGuard(new SuffixValue(type, i));
            Theory teach = teachers.get(type);
            Node b;

            DataValue dvi = teach.instantiate(prefix, ps, piv, pval,
                    constants, guard, p, new LinkedHashSet<>());
            ParValuation otherPval = new ParValuation();
            otherPval.putAll(pval);
                otherPval.put(p, dvi);

            nextMap.put(dvi, createFreshNode(i + 1, prefix,
                    ps, piv, otherPval));

            guardMap.put(dvi, guard);
            return new Node(p, nextMap, guardMap);
        }
    }
    
    private Node createNode(int i, Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            SDT... sdts) {
        Node n = createNode(i, prefix, ps, piv, pval, null, sdts);
        return n;
    }
    
    /**
     * This method computes the initial branching for an SDT. It re-uses
     * existing valuations where possible.
     * 
     * It is essential that the new branching includes all representatives of
     * the initial guards from the old branching, otherwise rows formed from the old
     * representatives will no longer be linkable with the source component.
     *
     */
    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Branching current,
            PIV piv, SymbolicDecisionTree... sdts) {
        
        MultiTheoryBranching oldBranching = (MultiTheoryBranching) current;
        
        Node rootNode = oldBranching.getRootNode();
//        System.out.println("From trees: " + Arrays.asList(sdts));
        
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i] = (SDTLeaf) sdts[i];
            } else {
                casted[i] = (SDT) sdts[i];
            }
        }
        
        ParValuation pval = new ParValuation();
        
        Node n;

        if (casted.length == 0) {
            n = createFreshNode(1, prefix, ps, piv, pval);
            return new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, casted);
        } else {
            n = createNode(1, prefix, ps, piv, pval, rootNode,  casted);
            MultiTheoryBranching fluff = new MultiTheoryBranching(
                    prefix, ps, n, piv, pval, constants, casted);
            return fluff;
        }
    }
    
    
    private class GuardContext {
    	final Mapping<SymbolicDataValue, DataValue<?>> contextValuation; 
    	
    	GuardContext(ParValuation parValuation, Word<PSymbolInstance> prefix, PIV piv, Constants constants) {
    		contextValuation = new Mapping<SymbolicDataValue, DataValue<?>>();
    		DataValue<?> [] values = DataWords.valsOf(prefix);
    		piv.forEach((param, reg) 
    				-> contextValuation.put(reg, values[param.getId()-1]));
    		parValuation.forEach((param, dv) 
    				-> contextValuation.put(new SuffixValue(param.getType(), param.getId()), dv));
    		constants.forEach((c, dv) 
    				-> contextValuation.put(c, dv));
    	}
    	
    	public String toString() {
    		return new StringBuilder().append(contextValuation).append("\n").toString();
    	}
    }
    
    // TODO PIV is not correctly registered and should be made available
    private Node createNode(int i, Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps,
            PIV piv, ParValuation pval, @Nullable Node oldNode, 
            SDT... sdts) {

        if (i == ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        } else {
        	Set<DataValue> oldDvs = oldNode != null ? oldNode.getDvs() : Collections.emptySet();
        	
            // obtain the data type, teacher, parameter
            DataType type = ps.getPtypes()[i - 1];
            Theory teach = teachers.get(type);
            Parameter p = new Parameter(type, i);

            int numSdts = sdts.length;
            // initialize maps for next nodes
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            // setup a guard context for checking refinement/possibility to merge
            GuardContext guardContext = new GuardContext(pval, prefix, piv, constants);
            SDTGuardLogic guardLogic = teach.getGuardLogic();
            // get merged guards to the set of guards they come from
            Map<SDTGuard, Set<SDTGuard>> mergedGuards = getNewGuards(sdts, guardContext, guardLogic);
            //  get old guards to the child SDT it connects to 
            Map<SDTGuard, List<SDT>> nextSDTs = getChildren(sdts);
            
            for (Map.Entry<SDTGuard, Set<SDTGuard>> mergedGuardEntry : mergedGuards.entrySet()) {
            	SDTGuard guard = mergedGuardEntry.getKey();
            	Set<SDTGuard> oldGuards = mergedGuardEntry.getValue();
            	DataValue dvi = null;
            	try {
            		// first solve using a constraint solver
            		 dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, oldDvs);
	            	
	            	// if merging of guards is done properly, there should be no case where the guard is not instantiable.
	            	if (dvi == null || dvi.getId().equals(Double.valueOf(0.0))) {
//	            		continue;
	            		throw new DecoratedRuntimeException("Unexpected dvi " + dvi)
	            		.addDecoration("merged guard", guard).addDecoration("from guards", oldGuards).addDecoration("context", guardContext);
	            	}
	            	
            	}catch(DecoratedRuntimeException exc) {
            		throw exc.addDecoration("guard map", guardMap).addDecoration("guard to be added", guard)
            		.addDecoration("sdts", Arrays.toString(sdts)).addDecoration("prefix", prefix).addDecoration("piv", piv);
            	}
            	
            	SDT [] nextLevelSDTs = oldGuards.stream().map(g -> nextSDTs.get(g)). // stream with of sdt lists for old guards
            			flatMap(g -> g.stream()).distinct().toArray(SDT []::new); // merge and pick distinct elements
            	
            	 ParValuation otherPval = new ParValuation();
                 otherPval.putAll(pval);
                     otherPval.put(p, dvi);

                Node nextOldNode = oldNode != null? oldNode.getChildNode(dvi) : null;
                nextMap.put(dvi, createNode(i + 1, prefix, ps, piv,
                          otherPval, nextOldNode, nextLevelSDTs));
                if (guardMap.containsKey(dvi)) {
                	throw new DecoratedRuntimeException( " New guards instantiated with same dvi in branching")
                	.addDecoration("guard map", guardMap).addDecoration("guard to be added", guard).
                	addDecoration("dvi", dvi).addDecoration("param symbol", ps).addDecoration("prefix", prefix).addDecoration("PIV", piv)
                	.addDecoration("sdts", Arrays.toString(sdts));
                }
                guardMap.put(dvi, guard);
            }
            
            log.log(Level.FINEST, "guardMap: " + guardMap.toString());
            log.log(Level.FINEST, "nextMap: " + nextMap.toString());
            assert !nextMap.isEmpty();
            assert !guardMap.isEmpty();
            return new Node(p, nextMap, guardMap);
        }
    }
    
    // produces a mapping from refined sdt guards to the top level sdt guards from which they are built. Multiple top level sdt guards 
    // can be combined to form a refined guard, hence each refined guard maps to a list of top level sdt guards.
    private Map<SDTGuard, Set<SDTGuard>> getNewGuards(SDT [] sdts, GuardContext guardContext, SDTGuardLogic guardLogic) {
    	
    	Map<SDTGuard, Set<SDTGuard>> mergedGroup =   new LinkedHashMap<>();
    	MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(constants, solver);
    	for (SDT sdt : sdts) {
    		Set<SDTGuard> nextGuardGroup = sdt.getChildren().keySet();
    		mergedGroup = combineGroups(mergedGroup, nextGuardGroup, guardContext, guardLogic, mlo);
    	}
    	return mergedGroup;
    }
    
    
    // returns a mapping from the new guards built to the old ones from which they were generated
    private Map<SDTGuard, Set<SDTGuard>> combineGroups(Map<SDTGuard, Set<SDTGuard>> mergedHead , Set<SDTGuard> nextGroup,  GuardContext guardContext, SDTGuardLogic guardLogic, MultiTheorySDTLogicOracle mlo) {
    	Map<SDTGuard, Set<SDTGuard>> mergedGroup = new LinkedHashMap<>();
    	if (mergedHead.isEmpty()) {
    		nextGroup.forEach(next -> {
    			mergedGroup.put(next, Sets.newHashSet(next));	
    		});
    		return mergedGroup;
    	}
    	Set <Pair<SDTGuard, SDTGuard>> headNextPairs = new HashSet<>(); 
    	
    	for (SDTGuard head : mergedHead.keySet()) {
    		// we filter out pairs already covered
    		SDTGuard []  notCoveredPairs = nextGroup.stream().  
    				filter(next -> !headNextPairs.contains(new Pair<>(next, head))).toArray(SDTGuard []::new);
    		
    		// we then select only the next guards which are compatible with the head guards, ie both can be instantiated
    		SDTGuard [] compatibleNextGuards = Stream.of(notCoveredPairs).
    				filter(next -> canBeMerged(head, next, guardContext, mlo)).toArray(SDTGuard []::new);
    		
    		for (SDTGuard next : compatibleNextGuards) {
    			
    			SDTGuard refinedGuard = null;
    			if (head.equals(next)) 
    				refinedGuard = next;
	    			else if (refines(next, head, guardContext, mlo)) 
	    				refinedGuard = next;
	    			else 
	    				if (refines(head, next, guardContext, mlo))
	    					refinedGuard = head;
	    				else 
	    					refinedGuard =  guardLogic.conjunction(head, next);
	    					
    			// we compute the old guard set, that is the guards over which conjunction was applied to form the refined guard
    			LinkedHashSet<SDTGuard> oldGuards = Sets.newLinkedHashSet(mergedHead.get(head));
    			oldGuards.add(next);
				mergedGroup.put(refinedGuard, oldGuards);
				headNextPairs.add(new Pair<>(head, next));
    		}
    	}
    	return mergedGroup;
    }
    
    private boolean canBeMerged(SDTGuard a, SDTGuard b, GuardContext guardContext, MultiTheorySDTLogicOracle mlo) {
    	if (a.equals(b) || a instanceof SDTTrueGuard || b instanceof SDTTrueGuard) 
    		return true;
    	
    	// some quick answers, implemented for compatibility with older theories.
    	if (a instanceof EqualityGuard) 
    		if (b.equals(((EqualityGuard)a).toDeqGuard())) 
    			return false;
    	if (b instanceof EqualityGuard)
    		if (a.equals(((EqualityGuard)b).toDeqGuard()))
    			return false;
    	return mlo.canBothBeSatisfied(a.toExpr(), new PIV(), b.toExpr(), new PIV(), guardContext.contextValuation); 
    }
    
    private boolean refines(SDTGuard a, SDTGuard b,  GuardContext guardContext, MultiTheorySDTLogicOracle mlo) {
    	if (b instanceof SDTTrueGuard) 
    		return true;
    	boolean ref1 = mlo.doesRefine(a.toExpr(), new PIV(), b.toExpr(), new PIV(), guardContext.contextValuation);
    	return ref1;
    }
    

	// produces a mapping from top level SDT guards to the next level SDTs. Since the same guard can appear 
    // in multiple SDTs, the guard maps to a list of SDTs
    private Map<SDTGuard, List<SDT>> getChildren(SDT [] sdts) {
    	List<Map<SDTGuard, SDT>> sdtChildren = Stream.of(sdts).map(sdt -> sdt.getChildren()).collect(Collectors.toList());
    	Map<SDTGuard, List<SDT>> children = new LinkedHashMap<>();
    	for (Map<SDTGuard, SDT> child : sdtChildren) {
    		child.forEach((guard, nextSdt) -> {
    			children.putIfAbsent(guard, new ArrayList<>());
    			children.get(guard).add(nextSdt);
    			});
    	}
    	
    	return children;
    }
}