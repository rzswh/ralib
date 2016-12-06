package de.learnlib.ralib.theory.inequality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class BranchingLogic<T extends Comparable<T>> {
	private DataType<T> type;
	private TypedTheory<T> theory;

	public BranchingLogic(TypedTheory<T> theory) {
		this.theory = theory;
		this.type = theory.getType();
	}

	public BranchingContext<T> computeBranchingContext(int pid, List<DataValue<T>> potential,
			Word<PSymbolInstance> prefix, Constants constants, SuffixValuation suffixValues,
			GeneralizedSymbolicSuffix suffix) {
		EnumSet<DataRelation> suffixRel = getSuffixRelations(suffix, pid);
		EnumSet<DataRelation> prefixRel = suffix.getPrefixRelations(pid);
		List<DataValue<T>> sumC = constants.getSumCs(this.type);
		Supplier<List<DataValue<T>>> prefVals = () -> Arrays.asList(DataWords.valsOf(prefix, this.type));
		Function<DataRelation, DataValue<T>> fromPrevSuffVal = (rel) -> {
			DataValue<T> eqSuffix = (DataValue<T>) suffixValues.get(findLeftMostRelatedSuffix(suffix, pid, rel));
			if (eqSuffix == null)
				eqSuffix = this.theory.getFreshValue(potential);
			else {
				if (rel == DataRelation.EQ_SUMC1)
					return new SumCDataValue<T>(eqSuffix, sumC.get(0));
				if (rel == DataRelation.EQ_SUMC2)
					return new SumCDataValue<T>(eqSuffix, sumC.get(1));
			}
			return eqSuffix;
		};

		BranchingContext<T> action = null;
		// if any of the pref/suff relations contains all, we do FULL and skip
		if (prefixRel.contains(DataRelation.ALL) && suffixRel.contains(DataRelation.ALL))
			action = new BranchingContext<>(BranchingStrategy.FULL, potential);
		else {
			// branching processing based on relations
			if (prefixRel.isEmpty()) {
				if (suffixRel.isEmpty() || suffixRel.equals(EnumSet.of(DataRelation.DEQ)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_FRESH);
				else if (suffixRel.contains(EnumSet.of(DataRelation.EQ)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ));
				else if (suffixRel.contains(EnumSet.of(DataRelation.EQ_SUMC1)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC1));
				else if (suffixRel.contains(EnumSet.of(DataRelation.EQ_SUMC2)))
					action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC2));
				else if (suffixRel.equals(EnumSet.of(DataRelation.LT))) 
					action = new BranchingContext<T>(BranchingStrategy.TRUE_SMALLER);
				else if (suffixRel.equals(EnumSet.of(DataRelation.GT))) 
					action = new BranchingContext<T>(BranchingStrategy.TRUE_GREATER);
			} else {
				if (prefixRel.equals(EnumSet.of(DataRelation.DEQ))) {
					if (suffixRel.isEmpty() || suffixRel.equals(EnumSet.of(DataRelation.DEQ)))
						action = new BranchingContext<T>(BranchingStrategy.TRUE_FRESH);
					else if (suffixRel.contains(DataRelation.EQ))
						action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ));
					else if (suffixRel.contains(EnumSet.of(DataRelation.EQ_SUMC1)))
						action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC1));
					else if (suffixRel.contains(EnumSet.of(DataRelation.EQ_SUMC2)))
						action = new BranchingContext<T>(BranchingStrategy.TRUE_PREV, fromPrevSuffVal.apply(DataRelation.EQ_SUMC2));
					else if (suffixRel.equals(EnumSet.of(DataRelation.LT))) 
						action = new BranchingContext<T>(BranchingStrategy.TRUE_SMALLER);
					else if (suffixRel.equals(EnumSet.of(DataRelation.GT))) 
						action = new BranchingContext<T>(BranchingStrategy.TRUE_GREATER);
				}

				else {
					if (EnumSet.of(DataRelation.EQ, DataRelation.EQ_SUMC1, DataRelation.EQ_SUMC2, DataRelation.DEQ).containsAll(prefixRel)) {
						if (EnumSet.of(DataRelation.EQ, DataRelation.EQ_SUMC1, DataRelation.EQ_SUMC2, DataRelation.DEQ).containsAll(suffixRel)) {
							//getRelatedSuffixValues(suffix, pid, suffixValues, EnumSet.of(DataRelation.EQ, DataRelation.DEQ));
							// if (suffixRel.contains(DataRelation.EQ) )
							// action = new
							// BranchingContext<T>(BranchingStrategy.TRUE_PREV,
							// eqSuffVal.get());
							// else
							//prefixRel.stream().map(rel -> filter(potential, sumC, rel)).flatMap(l -> l.stream()).c
							EnumSet<DataRelation> all = EnumSet.copyOf(prefixRel);
							all.addAll(suffixRel);
							
							List<DataValue<T>> newPotential = new ArrayList<>();//filter(potential, sumC, all);
							List<DataValue<T>> regVals = prefVals.get(); 
//									suffix.getPrefixSources(pid).stream()
//								.map(src -> src.getDataValuesWithSignature(prefix))
//								.flatMap(vals -> vals.stream()).map(dv -> (DataValue<T>) dv).collect(Collectors.toList());
							List<DataValue<T>> regPotential = pots(regVals, sumC, prefixRel);
							Collection<DataValue<T>> sufVals = suffixValues.values(type);//this.getRelatedSuffixValues(suffix, pid, suffixValues);
							List<DataValue<T>> sufPotential = pots(sufVals, sumC, suffixRel);
							newPotential.addAll(regPotential);
							newPotential.addAll(sufPotential);
							newPotential.addAll(constants.values(type));
							Collections.sort(newPotential, (dv1, dv2) -> dv1.getId().compareTo(dv2.getId()));
							action = new BranchingContext<T>(BranchingStrategy.IF_EQU_ELSE, newPotential);
						}
					}

				}
			}
		}
		
		if (action == null)
			action = new BranchingContext<>(BranchingStrategy.FULL, potential);

//		System.out.println(action.getStrategy() + " pref rel: " + prefixRel + " suf rel: " + suffixRel + " " + action.getBranchingValues());
		// return new BranchingContext<>(BranchingStrategy.FULL, potential);
		return action;
	}
	
	private List<DataValue<T>> pots(Collection<DataValue<T>> vals, List<DataValue<T>> sumConstants, EnumSet<DataRelation> equRels) {
		Set<DataValue<T>> newPots = new LinkedHashSet<>();
		for (DataRelation rel : equRels) {
			switch(rel) {
			case EQ:
				newPots.addAll(vals);
				break;
			case EQ_SUMC1:
				vals.forEach(val -> newPots.add(new SumCDataValue<T>(val, sumConstants.get(0))));
				break;
			case EQ_SUMC2:
				vals.forEach(val -> newPots.add(new SumCDataValue<T>(val, sumConstants.get(1))));
				break;
			}
		}
		
		return new ArrayList<>(newPots);
	}
	
	private List<DataValue<T>> filter(List<DataValue<T>> potential, List<DataValue<T>> sumConstants, EnumSet<DataRelation> equRels) {
		List<DataValue<T>> newPots = new ArrayList<>();
		Stream<DataValue<T>> potsStream = potential.stream();
			if (!equRels.contains(DataRelation.EQ)) 
				potsStream = potsStream.filter(dv -> (dv instanceof SumCDataValue));
			if (!equRels.contains(DataRelation.EQ_SUMC1)) {
				DataValue<T> sumC= sumConstants.get(0);
				potsStream = potsStream.filter(dv -> !(dv instanceof SumCDataValue) || 
						!((SumCDataValue<T>) dv).getConstant().equals(sumC));
			}
			if (!equRels.contains(DataRelation.EQ_SUMC2)) {
				DataValue<T> sumC= sumConstants.get(1);
				potsStream = potsStream.filter(dv -> !(dv instanceof SumCDataValue) || 
						!((SumCDataValue<T>) dv).getConstant().equals(sumC));
			}
			
			newPots = potsStream.collect(Collectors.toList());
		return newPots;
	}

	private List<DataValue<T>> getRelatedSuffixValues(GeneralizedSymbolicSuffix suffix, int pId,
			SuffixValuation suffixValues, DataRelation ... relations) {
		List<DataValue<T>> relatedValues = new ArrayList<DataValue<T>>();
		//List<DataRelation> rels = Arrays.asList(relations);
		DataType<?> t = suffix.getDataValue(pId).getType();
		for (int i = 1; i < pId; i++) {
			if (!t.equals(suffix.getDataValue(i).getType())) {
				continue;
			}
			EnumSet<DataRelation> suffRelations = suffix.getSuffixRelations(i, pId);
			if (!suffRelations.isEmpty()) { //&& rels.contains(suffRelations)) {
				DataValue<T> suffValue = (DataValue<T>) suffixValues.get(suffix.getDataValue(i));
				relatedValues.add(suffValue);
			}
		}
		return relatedValues;
	}

	private EnumSet<DataRelation> getSuffixRelations(GeneralizedSymbolicSuffix suffix, int idx) {
		// FIXME: support muliple types
		EnumSet<DataRelation> dset;
		if (idx == 1) {
			dset = EnumSet.noneOf(DataRelation.class);
		} else {
			dset = EnumSet.noneOf(DataRelation.class);
			for (int i = 1; i < idx; i++) {
				dset.addAll(suffix.getSuffixRelations(i, idx));
			}
		}

		return dset;
	}
	
	private int findLeftMostRelatedSuffix(GeneralizedSymbolicSuffix suffix, int pId, DataRelation rel) {
		// System.out.println("findLeftMostEqual (" + pId + "): " + suffix);
		DataType t = suffix.getDataValue(pId).getType();
		for (int i = 1; i < pId; i++) {
			if (!t.equals(suffix.getDataValue(i).getType())) {
				continue;
			}
			if (suffix.getSuffixRelations(i, pId).contains(rel))
				return i;
		}
		return -1;
	}

	public static class BranchingContext<T> {
		private final BranchingStrategy strategy;
		private final List<DataValue<T>> branchingValues;

		public BranchingContext(BranchingStrategy strategy) {
			this.strategy = strategy;
			this.branchingValues = null;
		}

		public BranchingContext(BranchingStrategy strategy, DataValue<T> potValue) {
			this(strategy, Arrays.asList(potValue));
		}

		public BranchingContext(BranchingStrategy strategy, List<DataValue<T>> potValues) {
			this.strategy = strategy;
			this.branchingValues = potValues;
		}

		@SafeVarargs
		public BranchingContext(BranchingStrategy strategy, List<DataValue<T>>... potValues) {
			this.strategy = strategy;
			this.branchingValues = Arrays.stream(potValues).flatMap(potV -> potV.stream()).collect(Collectors.toList());
		}

		public List<DataValue<T>> getBranchingValues() {
			return this.branchingValues;
		}

		public DataValue<T> getBranchingValue() {
			if (this.branchingValues != null && !this.branchingValues.isEmpty())
				return branchingValues.get(0);
			return null;
		}

		public BranchingStrategy getStrategy() {
			return this.strategy;
		}
	}

	public static enum BranchingStrategy {
		TRUE_FRESH, TRUE_PREV, IF_EQU_ELSE, IF_INTERVALS_ELSE, FULL, TRUE_SMALLER, TRUE_GREATER;
	}
}
