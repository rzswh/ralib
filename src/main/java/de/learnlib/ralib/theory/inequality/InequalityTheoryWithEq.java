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
package de.learnlib.ralib.theory.inequality;

import static de.learnlib.ralib.solver.jconstraints.JContraintsUtil.toVariable;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTConstructor;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.oracles.mto.SDTQuery;
import de.learnlib.ralib.oracles.mto.SemanticEquivalenceChecker;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.jconstraints.JConstraintsGuardInstantiator;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.IfElseGuardMerger;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTEquivalenceChecker;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTMultiGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.BranchingLogic.BranchingContext;
import de.learnlib.ralib.theory.inequality.BranchingLogic.BranchingStrategy;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.words.Word;

/**
 * Abstract class for inequality theories with fresh values.
 * For output parameters only equality and disequality are supported.
 *
 * @param<T>
 */
public abstract class InequalityTheoryWithEq<T extends Comparable<T>> implements TypedTheory<T> {

	protected static final LearnLogger log = LearnLogger.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Builds a guard instantiator.
	 */
	protected static <P extends Comparable<P>> JConstraintsGuardInstantiator<P> getInstantiator(DataType type,
			String solverName,
			Class<P> domainType) {
		gov.nasa.jpf.constraints.api.ConstraintSolver solver = gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory.createSolver(solverName);
		return new JConstraintsGuardInstantiator<P>(type, solver, domainType);
	}

	private boolean freshValues;
	protected DataType type;
	private final InequalityGuardMerger fullMerger;
	private final IfElseGuardMerger ifElseMerger;
	private boolean suffixOptimization;
	private final String jSolverName; 
	private JConstraintsGuardInstantiator<T> guardInstantiator;

	public InequalityTheoryWithEq(InequalityGuardMerger fullMerger) {
		this.freshValues = false;
		this.suffixOptimization = false;
		this.fullMerger = fullMerger;
		this.jSolverName = "z3";
		this.ifElseMerger = new IfElseGuardMerger(getGuardLogic());
	}

	@Override
	public void setCheckForFreshOutputs(boolean doit) {
		freshValues = doit;
	}

	/**
	 * Sets the type as well as the inequality guard merger and instantiator.
	 */
	public void setType(DataType dataType) {
		this.type = dataType;
		this.guardInstantiator = getInstantiator(dataType, jSolverName, getDomainType());
	}

	public DataType getType() {
		return type;
	}

	private Map<SDTGuard, SDT> mergeAllGuards(final Map<SDTGuard, SDT> tempGuards,
			Map<SDTGuard, DataValue<T>> instantiations, SDTEquivalenceChecker sdtChecker,
			Mapping<SymbolicDataValue, DataValue<?>> valuation) {
		if (tempGuards.size() == 1) { // for true guard do nothing
			return tempGuards;
		}

		final List<SDTGuard> sortedGuards = tempGuards.keySet().stream().sorted(new Comparator<SDTGuard>() {
			public int compare(SDTGuard o1, SDTGuard o2) {
				DataValue<T> dv1 = instantiations.get(o1);
				DataValue<T> dv2 = instantiations.get(o2);
				int ret = ((java.lang.Comparable) dv1.getId()).compareTo((java.lang.Comparable) dv2.getId());
				// the generated guards can never have the same dv
				// instantiation. In case they do, it signals collision and
				// needs to be addressed.
				if (ret == 0) {
					throw new DecoratedRuntimeException("Different guards are instantiated with equal Dv")
							.addDecoration("guard1:", o1).addDecoration("dv1", dv1).addDecoration("guard2:", o2)
							.addDecoration("dv2", dv2);
				}
				return ret;
			}
		}).collect(Collectors.toList());

		// System.out.println("TEMP: " + tempGuards);
		Map<SDTGuard, SDT> merged = fullMerger.merge(sortedGuards, tempGuards, sdtChecker, valuation);
		// System.out.println("RES: " + merged);

		return merged;
	}

	protected Map<SDTGuard, SDT> mergeEquDiseqGuards(final Map<SDTGuard, SDT> equGuards, SDTGuard elseGuard,
			SDT elseSDT, SDTEquivalenceChecker sdtChecker) {
		Map<SDTGuard, SDT> merged = ifElseMerger.merge(equGuards, elseGuard, elseSDT, sdtChecker);

		return merged;
	}

	// given a set of registers and a set of guards, keep only the registers
	// that are mentioned in any guard
	//
	protected PIV keepMem(Map<SDTGuard, SDT> guardMap) {
		PIV ret = new PIV();

		for (Map.Entry<SDTGuard, SDT> e : guardMap.entrySet()) {
			SDTGuard mg = e.getKey();
			if (mg instanceof SDTIfGuard) {
				log.log(Level.FINEST, mg.toString());
				SymbolicDataValue r = ((SDTIfGuard) mg).getRegister();
				Parameter p = new Parameter(r.getType(), r.getId());
				if (r instanceof Register) {
					ret.put(p, (Register) r);
				}
			} else if (mg instanceof IntervalGuard) {
				IntervalGuard iGuard = (IntervalGuard) mg;
				if (!iGuard.isBiggerGuard()) {
					SymbolicDataValue r = iGuard.getRightSDV();
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}

				}
				if (!iGuard.isSmallerGuard()) {
					SymbolicDataValue r = iGuard.getLeftSDV();
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}
				}
			} else if (mg instanceof SDTMultiGuard) {
				Set<SymbolicDataValue> rSet = ((SDTMultiGuard) mg).getAllSDVsFormingGuard();
				for (SymbolicDataValue r : rSet) {
					Parameter p = new Parameter(r.getType(), r.getId());
					if (r instanceof Register) {
						ret.put(p, (Register) r);
					}
				}
			} else if (!(mg instanceof SDTTrueGuard)) {
				throw new IllegalStateException("wrong kind of guard " + mg + " type: " + mg.getClass());
			}
		}
		return ret;
	}

	public SDT treeQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix, WordValuation values, PIV piv,
			Constants constants, SuffixValuation suffixValues, SDTConstructor oracle, ConstraintSolver solver, IOOracle traceOracle) {

		int pId = values.size() + 1;
		SuffixValue sv = suffix.getDataValue(pId);
		DataType type = sv.getType();

		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
		DataValue<T>[] typedPrefixValues = DataWords.valsOf(prefix, type);

		WordValuation typedPrefixValuation = new WordValuation();
		for (int i = 0; i < typedPrefixValues.length; i++) {
			typedPrefixValuation.put(i + 1, typedPrefixValues[i]);
		}

		SuffixValue currentParam = new SuffixValue(type, pId);

		Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		Collection<DataValue<T>> potSet = DataWords.<T>joinValsToSet( constants.<T>values(type),
				DataWords.<T>valSet(prefix, type), values.<T>values(type));

		List<DataValue<T>> potList = new ArrayList<>(potSet);
		List<DataValue<T>> potential = getPotential(potList);
		// WE ASSUME THE POTENTIAL IS SORTED
		int potSize = potential.size();
		Map<SDTGuard, DataValue<T>> guardDvs = new LinkedHashMap<>();

		ParameterizedSymbol ps = SymbolicSuffix.computeSymbol(suffix, pId);
		// if suffix optimization is enabled, we compute an optimized branching
		// context,
		// otherwise we exhaustively check all branches
		BranchingContext<T> context;
		if (suffixOptimization) {
			BranchingLogic<T> logic = new BranchingLogic<T>(this);
			context = logic.computeBranchingContext(pId, potential, prefix, constants, suffixValues, suffix);

		} else
			context = new BranchingContext<>(BranchingStrategy.FULL, potential);

		BranchingStrategy branching = context.getStrategy();

		if (ps instanceof OutputSymbol && ps.getArity() > 0) {

			int idx = SymbolicSuffix.computeLocalIndex(suffix, pId);
			Word<PSymbolInstance> query = SymbolicSuffix.buildQuery(prefix, suffix, values);
			Word<PSymbolInstance> trace = traceOracle.trace(query);
			PSymbolInstance out = trace.lastSymbol();

			// we compare not only the output generated to the suffix, but also
			// the query thus far to the trace obtained.
			// In case outputs in the trace are different, tracing an output
			// value to a sdv
			// might not be possible, since it could originate from an output
			// value the system generated
			// that was not captured by the suffix. In such cases, we terminate
			// recursion by returning a rejecting subtree.
			if (out.getBaseSymbol().equals(ps) && query.equals(trace.prefix(-1))) {

				DataValue<T> d = out.getParameterValues()[idx];
				// special case: fresh values in outputs
				// we only support == and != in this case (since fresh overlaps
				// with greater)
				if (freshValues) {
					if (d instanceof FreshValue && !potential.contains(d)) {
						d = getFreshValue(potential);
						WordValuation trueValues = new WordValuation();
						trueValues.putAll(values);
						trueValues.put(pId, new FreshValue<T>(d.getType(), d.getId()));
						SuffixValuation trueSuffixValues = new SuffixValuation(suffixValues);
						trueSuffixValues.put(sv, d);
						SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
						trueSuffixValues.addSuffGuard(trueGuard);
						SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, piv, constants, trueSuffixValues);

						log.log(Level.FINEST, " single deq SDT : " + sdt.toString());

						Map<SDTGuard, SDT> merged = new LinkedHashMap<SDTGuard, SDT>();
						merged.put(trueGuard, sdt);

						return new SDT(merged);
					} else {
						// as outputs, we can shortcut, as we only support
						// sumc/equality when fresh values are enabled.
						// merging is not necessary, since we know that the only
						// output value accepted is that generated by the system
						SymbolicDataExpression outExpr = getSDExprForDV(d, prefixValues, values, constants);
						if (outExpr == null) {
							throw new DecoratedRuntimeException("Couldn't find " + d + " in prefixValues: "
									+ prefixValues + " values:" + values + " " + "constants: " + constants + "\n query:"
									+ query + "\n trace:" + trace);
						}

						WordValuation eqValues = new WordValuation(values);
						SuffixValuation eqSuffixValues = new SuffixValuation(suffixValues);
						eqValues.put(pId, d);
						eqSuffixValues.put(sv, d);
						EqualityGuard eqGuard = new EqualityGuard(currentParam, outExpr);
						eqSuffixValues.addSuffGuard(eqGuard);
						SDT eqSdt = oracle.treeQuery(prefix, suffix, eqValues, piv, constants, eqSuffixValues);
						tempKids.put(eqGuard, eqSdt);

						WordValuation deqValues = new WordValuation(values);
						SuffixValuation deqSuffixValues = new SuffixValuation(suffixValues);
						DataValue<T> deqValue = getFreshValue(potential);
						deqValues.put(pId, deqValue);
						deqSuffixValues.put(sv, deqValue);
						DisequalityGuard deqGuard = new DisequalityGuard(currentParam, outExpr);
						deqSuffixValues.addSuffGuard(deqGuard);
						SDT deqSdt = oracle.treeQuery(prefix, suffix, deqValues, piv, constants, deqSuffixValues);

						Mapping<SymbolicDataValue, DataValue<?>> guardContext = buildContext(prefixValues, values,
								constants);
						SDTEquivalenceChecker eqChecker = new SemanticEquivalenceChecker(constants, solver, guardContext);

						Map<SDTGuard, SDT> merged = mergeEquDiseqGuards(tempKids, deqGuard, deqSdt, eqChecker);

						piv.putAll(keepMem(merged));
						return new SDT(merged);
					}
				}
			} else {
				int maxSufIndex = DataWords.paramLength(suffix.getActions()) + 1;
				SDT rejSdt = makeRejectingBranch(currentParam.getId() + 1, maxSufIndex);
				SDTTrueGuard trueGuard = new SDTTrueGuard(currentParam);
				tempKids.put(trueGuard, rejSdt);

				piv.putAll(keepMem(tempKids));
				return new SDT(tempKids);
			}
		}

		// System.out.println("potential " + potential);
		if (potential.isEmpty() || branching == BranchingStrategy.TRUE_FRESH) {
			// System.out.println("empty potential");
			WordValuation elseValues = new WordValuation(values);
			DataValue<T> fresh = getFreshValue(potential);
			elseValues.put(pId, fresh);

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation elseSuffixValues = new SuffixValuation(suffixValues);
			elseSuffixValues.put(sv, fresh);
			SDTGuard trueGuard = new SDTTrueGuard(currentParam);
			elseSuffixValues.addSuffGuard(trueGuard);

			SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, piv, constants, elseSuffixValues);
			tempKids.put(trueGuard, elseOracleSdt);
			piv.putAll(keepMem(tempKids));
			return new SDT(tempKids);
		} else if (branching == BranchingStrategy.TRUE_PREV) {
			DataValue<T> prev = context.getBranchingValue();

			WordValuation equValues = new WordValuation(values);
			equValues.put(pId, prev);

			// this is the valuation of the suffixvalues in the suffix
			SuffixValuation equSuffixValues = new SuffixValuation(suffixValues);
			equSuffixValues.put(sv, prev);
			EqualityGuard eqGuard = makeEqualityGuard(prev, prefixValues, currentParam, values, constants);
			equSuffixValues.addSuffGuard(eqGuard);

			SDT equOracleSdt = oracle.treeQuery(prefix, suffix, equValues, piv, constants, equSuffixValues);
			tempKids.put(eqGuard, equOracleSdt);
			piv.putAll(keepMem(tempKids));
			return new SDT(tempKids);
		}
		// process each '<' case
		else {

			if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.TRUE_SMALLER) {

				if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.TRUE_SMALLER) {
					// smallest case
					DataValue<T> dvRight = potential.get(0);
					IntervalGuard sguard = makeSmallerGuard(dvRight, prefixValues, currentParam, values, piv,
							constants);
					DataValue<T> smcv = pickIntervalDataValue(null, dvRight);
					guardDvs.put(sguard, smcv);
				}

				if (branching == BranchingStrategy.FULL) {
					// biggest case
					DataValue<T> dvLeft = potential.get(potSize - 1);
					IntervalGuard bguard = makeBiggerGuard(dvLeft, prefixValues, currentParam, values, piv, constants);

					DataValue<T> bgcv = pickIntervalDataValue(dvLeft, null);
					guardDvs.put(bguard, bgcv);
				}
			}

			if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.IF_INTERVALS_ELSE) {

				// middle cases
				List<Range<T>> ranges = generateRangesFromPotential(potential, false);

				for (Range<T> range : ranges) {
					DataValue<T> dvMRight = range.right;
					DataValue<T> dvMLeft = range.left;

					IntervalGuard intervalGuard = makeIntervalGuard(dvMLeft, dvMRight, prefixValues, currentParam,
							values, piv, constants);

					DataValue<T> cv = pickIntervalDataValue(dvMLeft, dvMRight);
					guardDvs.put(intervalGuard, cv);
				}

				if (branching == BranchingStrategy.IF_INTERVALS_ELSE) {
					throw new RuntimeException("Processing for " + branching.name() + " not yet implemented");
				}
			}

			if (branching == BranchingStrategy.FULL || branching == BranchingStrategy.IF_EQU_ELSE) {
				List<DataValue<T>> branchingValues = context.getBranchingValues();

				for (DataValue<T> newDv : branchingValues) {
					EqualityGuard eqGuard = makeEqualityGuard(newDv, prefixValues, currentParam, values, constants);
					guardDvs.put(eqGuard, newDv);
				}
			}

			if (branching == BranchingStrategy.IF_EQU_ELSE) {
				SDTGuard[] elseConjuncts = guardDvs.keySet().stream().map(g -> ((EqualityGuard) g).toDeqGuard())
						.toArray(SDTGuard[]::new);

				SDTGuard elseGuard = elseConjuncts.length == 1 ? elseConjuncts[0]
						: new SDTAndGuard(currentParam, elseConjuncts);
				DataValue<T> elseValue = getFreshValue(potential);
				guardDvs.put(elseGuard, elseValue);
			}
		}

		tempKids = treeQueriesForInstantiations(guardDvs, suffix, oracle, prefix, values, piv, constants, suffixValues);

		Map<SDTGuard, SDT> merged;
		Mapping<SymbolicDataValue, DataValue<?>> guardContext = buildContext(prefixValues, values, constants);
		SDTEquivalenceChecker eqChecker = new SemanticEquivalenceChecker(constants, solver, guardContext);

		if (branching == BranchingStrategy.FULL) {
			merged = mergeAllGuards(tempKids, guardDvs, eqChecker, guardContext);
		} else {
			if (tempKids.size() == 1)
				merged = tempKids;
			else {
				assert branching == BranchingStrategy.IF_EQU_ELSE;
				SDTGuard elseGuard = new ArrayList<>(tempKids.keySet()).get(tempKids.size() - 1);
				SDT elseSDT = tempKids.remove(elseGuard);
				merged = mergeEquDiseqGuards(tempKids, elseGuard, elseSDT, eqChecker);
			}
		}

		assert !merged.keySet().isEmpty();
		piv.putAll(keepMem(merged));

		log.log(Level.FINEST, "temporary guards = " + tempKids.keySet());
		log.log(Level.FINEST, "merged guards = " + merged.keySet());
		log.log(Level.FINEST, "merged pivs = " + piv.toString());

		// clear the temporary map of children
		tempKids = new LinkedHashMap<SDTGuard, SDT>();

		for (SDTGuard g : merged.keySet()) {
			assert !(g == null);
			if (g instanceof SDTTrueGuard) {
				if (merged.keySet().size() != 1) {
					throw new IllegalStateException("only one true guard allowed: \n" + prefix + " + " + suffix);
				}
			}
		}

		SDT returnSDT = new SDT(merged);
		return returnSDT;

	}

	private Mapping<SymbolicDataValue, DataValue<?>> buildContext(List<DataValue> prefixValues, WordValuation ifValues,
			Constants constants) {
		Mapping<SymbolicDataValue, DataValue<?>> context = new Mapping<SymbolicDataValue, DataValue<?>>();
		HashSet<DataValue> prSet = new HashSet<DataValue>(prefixValues);
		for (DataValue dv : prSet) {
			Register reg = this.getRegisterWithValue(dv, prefixValues);
			context.put(reg, dv);
		}

		for (DataValue dv : ifValues.values()) {
			SuffixValue suff = this.getSuffixWithValue(dv, ifValues);
			context.put(suff, dv);
		}

		context.putAll(constants.ofType(this.getType()));
		return context;
	}

	private Map<SDTGuard, SDT> treeQueriesForInstantiations(Map<SDTGuard, DataValue<T>> guardDvs,
			GeneralizedSymbolicSuffix suffix, SDTConstructor oracle, Word<PSymbolInstance> prefix,
			WordValuation wordVals, PIV piv, Constants constants, SuffixValuation suffixVals) {
		int pId = wordVals.size() + 1;
		SuffixValue sv = suffix.getDataValue(pId);
		final Map<SDTGuard, SDT> tempKids = new LinkedHashMap<>();

		for (SDTGuard sdtGuard : guardDvs.keySet()) {
			DataValue<T> dv = guardDvs.get(sdtGuard);
			WordValuation newWordVals = new WordValuation(wordVals);
			newWordVals.put(pId, dv);
			SuffixValuation newSuffixVals = new SuffixValuation(suffixVals);
			newSuffixVals.put(sv, dv);
			newSuffixVals.addSuffGuard(sdtGuard);
			SDTQuery sdtQuery = new SDTQuery(newWordVals, newSuffixVals);
			SDT sdt = oracle.treeQuery(prefix, suffix, sdtQuery.getWordValuation(), piv, constants,
					sdtQuery.getSuffValuation());
			tempKids.put(sdtGuard, sdt);
		}

		return tempKids;
	}

	/**
	 * Produces valid ranges from the sorted potential.  
	 * 
	 * @param potential  a sorted list of potential values
	 * @param includeOpenRanges  include open ranges in the result
	 * @return
	 */
	protected List<Range<T>> generateRangesFromPotential(List<DataValue<T>> potential, boolean includeOpenRanges) {
		int potSize = potential.size();
		List<Range<T>> ranges = new ArrayList<Range<T>>(potential.size()+2);
		if (includeOpenRanges && !potential.isEmpty()) 
			ranges.add(new Range<T>(null, potential.get(0)));
		
		for (int i = 1; i < potSize; i++)
			ranges.add(new Range<T>(potential.get(i - 1), potential.get(i)));
		
		if (includeOpenRanges && !potential.isEmpty()) 
			ranges.add(new Range<T>(potential.get(potential.size()-1), null));

		return ranges;
	}

	protected static class Range<T> {
		public final DataValue<T> left;
		public final DataValue<T> right;

		public Range(DataValue<T> left, DataValue<T> right) {
			super();
			this.left = left;
			this.right = right;
		}

		public String toString() {
			return left + "..." + right;
		}
	}

	/*
	 * Creates a "unary tree" of depth maxIndex - nextSufIndex which leads to a rejecting Leaf.
	 * Edges are of type {@link SDTTrueGuard}.
	 * Used to shortcut output processing.
	 */
	private SDT makeRejectingBranch(int nextSufIndex, int maxIndex) {
		if (nextSufIndex == maxIndex) {
			// map.put(guard, SDTLeaf.REJECTING);
			return SDTLeaf.REJECTING;
		} else {
			Map<SDTGuard, SDT> map = new LinkedHashMap<>();
			SDTTrueGuard trueGuard = new SDTTrueGuard(new SuffixValue(this.getType(), nextSufIndex));
			map.put(trueGuard, makeRejectingBranch(nextSufIndex + 1, maxIndex));
			SDT sdt = new SDT(map);
			return sdt;
		}
	}

	private IntervalGuard makeIntervalGuard(DataValue<T> biggerDv, DataValue<T> smallerDv,
			List<DataValue> prefixValues, SuffixValue currentParam, WordValuation ifValues, PIV pir,
			Constants constants) {
		IntervalGuard smallerGuard = makeSmallerGuard(smallerDv, prefixValues, currentParam, ifValues, pir, constants);
		IntervalGuard biggerGuard = makeBiggerGuard(biggerDv, prefixValues, currentParam, ifValues, pir, constants);
		return new IntervalGuard(currentParam, biggerGuard.getLeftExpr(), smallerGuard.getRightExpr());
	}

	private IntervalGuard makeBiggerGuard(DataValue<T> biggerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(biggerDv, prefixValues, ifValues, constants);
		IntervalGuard bg = new IntervalGuard(currentParam, regOrSuffixExpr, null);
		return bg;
	}

	private IntervalGuard makeSmallerGuard(DataValue<T> smallerDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, PIV pir, Constants constants) {
		SymbolicDataExpression regOrSuffixExpr = getSDExprForDV(smallerDv, prefixValues, ifValues, constants);
		IntervalGuard sg = new IntervalGuard(currentParam, null, regOrSuffixExpr);
		return sg;
	}

	private EqualityGuard makeEqualityGuard(DataValue<T> equDv, List<DataValue> prefixValues,
			SuffixValue currentParam, WordValuation ifValues, Constants constants) {
		SymbolicDataExpression sdvExpr = getSDExprForDV(equDv, prefixValues, ifValues, constants);
		return new EqualityGuard(currentParam, sdvExpr);
	}

	private SymbolicDataExpression getSDExprForDV(DataValue<T> dv, List<DataValue> prefixValues, WordValuation ifValues,
			Constants constants) {
		SymbolicDataValue SDV;
		if (constants.containsValue(dv)) {
			return constants.getConstantWithValue(dv);
		} else if (dv instanceof SumCDataValue) {
			SumCDataValue<T> sumDv = (SumCDataValue<T>) dv;
			SDV = getSDVForDV(sumDv.toRegular(), prefixValues, ifValues, constants);
			// if there is no previous value equal to the summed value, we pick
			// the data value referred by the sum
			// by this structure, we always pick equality before sumc equality
			// when the option is available
			if (SDV == null) {
				DataValue<T> constant = sumDv.getConstant();
				DataValue<T> prevDV = sumDv.getOperand();
				SymbolicDataValue prevSDV = getSDVForDV(prevDV, prefixValues, ifValues, constants);
				return new SumCDataExpression(prevSDV, constant);
			} else {
				return SDV;
			}
		} else {
			SDV = getSDVForDV(dv, prefixValues, ifValues, constants);
			return SDV;
		}
	}

	private SymbolicDataValue getSDVForDV(DataValue<T> dv, @Nullable List<DataValue> prefixValues,
			WordValuation ifValues, Constants constants) {

		if (constants.containsValue(dv)) {
			return constants.getConstantWithValue(dv);
		}

		SymbolicDataValue sdv = getRegisterWithValue(dv, prefixValues);

		if (sdv == null) // no register found
			sdv = getSuffixWithValue(dv, ifValues);

		return sdv;
	}

	private SuffixValue getSuffixWithValue(DataValue<T> dv, WordValuation ifValues) {
		if (ifValues.containsValue(dv)) {
			int first = Collections.min(ifValues.getAllKeys(dv));
			return new SuffixValue(type, first);
		}
		return null;
	}

	private Register getRegisterWithValue(DataValue<T> dv, List<DataValue> prefixValues) {
		if (prefixValues.contains(dv)) {
			int newDv_i = prefixValues.indexOf(dv) + 1;
			Register newDv_r = new Register(type, newDv_i);
			return newDv_r;
		}

		return null;
	}

	public abstract List<DataValue<T>> getPotential(List<DataValue<T>> vals);

	private DataValue getRegisterValue(SymbolicDataValue r, PIV piv, List<DataValue> prefixValues, Constants constants,
			ParValuation pval) {
		if (r.isRegister()) {
			log.log(Level.FINEST, "piv: " + piv + " " + r.toString() + " " + prefixValues);
			Parameter p = piv.getOneKey((Register) r);
			log.log(Level.FINEST, "p: " + p.toString());
			int idx = p.getId();
			return prefixValues.get(idx - 1);
		} else if (r.isSuffixValue()) {
			Parameter p = new Parameter(r.getType(), r.getId());
			return pval.get(p);
		} else if (r.isConstant()) {
			return constants.get((SymbolicDataValue.Constant) r);
		} else {
			throw new IllegalStateException("this can't possibly happen");
		}
	}

	@Override
	public DataValue<T> instantiate(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
			Constants constants, SDTGuard guard, Parameter param, Set<DataValue<T>> oldDvs) {
		DataType type = param.getType();
		DataValue<T> returnValue = null;
		List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
		log.log(Level.FINEST, "prefix values : " + prefixValues.toString());
		
		Valuation val = new Valuation();
		
		// set sdvs in the valuation to their corresponding values
		for (SymbolicDataValue sdv : guard.getAllSDVsFormingGuard()) {
			DataValue sdvVal = getRegisterValue(sdv, piv, prefixValues, constants, pval);
			val.setValue(toVariable(sdv), sdvVal.getId());
		}
		
		Set<DataValue<T>> alreadyUsedValues = DataWords.<T>joinValsToSet(constants.<T>values(type));
		
		// can we use an old value?
		if (!(oldDvs.isEmpty())) {
			for (DataValue<T> oldDv : oldDvs) {
				Valuation newVal = new Valuation();
				newVal.putAll(val);
				newVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), oldDv.getId());
				DataValue<T> inst = guardInstantiator.instantiate(guard, newVal, constants, alreadyUsedValues);
				if (inst != null) {
					return oldDv;
				}
			}
		}
		
		Collection<DataValue<T>> potentialSet = DataWords.<T>joinValsToSet(constants.<T>values(type),
				DataWords.<T>valSet(prefix, type), pval.<T>values(type));
		List<DataValue<T>> potential = getPotential(new ArrayList<>(potentialSet));
		
		// how about a Fresh Value ?
		DataValue<T> freshValue = getFreshValue(potential);
		Valuation freshVal = new Valuation();
		freshVal.putAll(val);
		freshVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), freshValue.getId());
		DataValue<T> freshInst = guardInstantiator.instantiate(guard, freshVal, constants, alreadyUsedValues);
		if (freshInst != null) {
			return new FreshValue<T>(freshValue.getType(), freshValue.getId());
		}
		
		
		Set<DataValue> rawAlreadyUsedValues = new LinkedHashSet<>(alreadyUsedValues);
		
		// how about an interval value?
		List<Range<T>> ranges = generateRangesFromPotential(potential, true);
		for (Range<T> range : ranges) {
			IntervalDataValue<T> intValue = pickIntervalDataValue(range.left, range.right, rawAlreadyUsedValues);
			Valuation intVal = new Valuation();
			intVal.putAll(val);
			intVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), intValue.getId());
			DataValue<T> intInst = guardInstantiator.instantiate(guard, intVal, constants, alreadyUsedValues);
			if (intInst != null)
				return intValue;
		}
		
		// how about a potential value ?
		for (DataValue<T> potValue : potential) {
			Valuation potVal = new Valuation();
			potVal.putAll(val);
			potVal.setValue(toVariable(new SuffixValue(param.getType(), param.getId())), potValue.getId());
			DataValue<T> potInst = guardInstantiator.instantiate(guard, potVal, constants, alreadyUsedValues);
			if (potInst != null) {
				return potValue;
			}
		}
		
		// ok, we have exhausted all possibilities, let's set no restrictions
		// if we do get a value, something is amiss
		DataValue<T> testInst = guardInstantiator.instantiate(guard, val, constants, alreadyUsedValues);
		if (testInst != null) {
			throw new IllegalStateException("Instantiating a guard is possible, but not within the restrictions of the theory");
			
		}
		return null;
	}
	
	private IntervalDataValue<T> pickIntervalDataValue(DataValue<T> left, DataValue<T> right,
			Set<DataValue> prohibited) {
		if (left != null && right != null && left.getId().compareTo(right.getId()) >= 0)
			return null;
		IntervalDataValue<T> dv = pickIntervalDataValue(left, right);
		if (prohibited.contains(dv)) {
			if (right != null) {
				dv = pickIntervalDataValue(left, dv, prohibited);
				if (dv != null)
					return dv;
			}
			if (left != null) {
				dv = pickIntervalDataValue(dv, right, prohibited);
				if (dv != null)
					return dv;
			}
		}
		return dv;
	}

	/**
	 * Produces an IntervalDataValue that is greater than left if left is not null, and smaller than right, if right is not null. 
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	/*
	 * Note: If fresh values are enabled, it is possible that left and right are more than a fresh step apart.
	 */
	public abstract IntervalDataValue<T> pickIntervalDataValue(@Nullable DataValue<T> left, @Nullable DataValue<T> right);

	public List<EnumSet<DataRelation>> getRelations(List<DataValue<T>> left, DataValue<T> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		left.stream().forEach((dv) -> {
			final int c = dv.getId().compareTo(right.getId());
			if (c == 0)
				ret.add(EnumSet.of(DataRelation.EQ));
			else if (c > 0)
				ret.add(EnumSet.of(DataRelation.DEFAULT));
			else
				ret.add(EnumSet.of(DataRelation.LT));
		});

		return ret;
	}

	/*
	 *	Suffix optimization should be made robust before it is enabled for this theory 
	 */
	public void setUseSuffixOpt(boolean useit) {
		//suffixOptimization = useit;
	}

}
