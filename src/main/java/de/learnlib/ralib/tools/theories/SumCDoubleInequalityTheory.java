package de.learnlib.ralib.tools.theories;

import static de.learnlib.ralib.theory.DataRelation.DEFAULT;
import static de.learnlib.ralib.theory.DataRelation.DEQ;
import static de.learnlib.ralib.theory.DataRelation.DEQ_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.DEQ_SUMC2;
import static de.learnlib.ralib.theory.DataRelation.EQ;
import static de.learnlib.ralib.theory.DataRelation.EQ_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.EQ_SUMC2;
import static de.learnlib.ralib.theory.DataRelation.LT;
import static de.learnlib.ralib.theory.DataRelation.LT_SUMC1;
import static de.learnlib.ralib.theory.DataRelation.LT_SUMC2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.mapper.Determinizer;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory  implements SumCTheory{
	//maxSumC : the biggest sum constant
	
	// maxSumC multiplied this factor results in the smaller/bigger step. 
	// Smaller or bigger intervals are generated by subtracting or adding this step from the minimum / to the maximum.
	private static int SM_BG_FACTOR = 10;  
	
	// maxSumC multiplied by this factor results in the fresh step.
	// Fresh values are generated at a fresh step distance from each other, in increasing order, starting from 0.
	private static int FRESH_FACTOR = 100; 
	
	private List<DataValue<Double>> sortedSumConsts;
	private List<DataValue<Double>> regularConstants;

	// fresh step
	private Double freshStep;
	// step for selecting a smaller value or a bigger value, note smBgStep << freshStep
	private DataValue<Double> smBgStep;
	
	
	public SumCDoubleInequalityTheory() {
	}
	
	public SumCDoubleInequalityTheory(DataType dataType, List<DataValue<Double>> sumConstants,
			List<DataValue<Double>> regularConstants) {
		super(dataType);
		setConstants(sumConstants, regularConstants);
	}
	
	public void setConstants(Constants constants) {
		setConstants (constants.getSumCs(this.getType()), new ArrayList<>(constants.values(this.getType())));
	}
	
	private void setConstants (List<DataValue<Double>> sumConstants,
			List<DataValue<Double>> regularConstants) {
		sortedSumConsts = new ArrayList<>(sumConstants);
		Collections.sort(sortedSumConsts, new Cpr());
		this.regularConstants = regularConstants;
		Double step = sortedSumConsts.isEmpty() ? 1.0 : maxSumC().getId();
		freshStep = step * FRESH_FACTOR;
		smBgStep = new DataValue<Double>(type, step * SM_BG_FACTOR);
	}
	
	@Override
	public EnumSet<DataRelation> recognizedRelations() {
		return EnumSet.of(DEQ, EQ, LT, DEFAULT, LT_SUMC1, LT_SUMC2, EQ_SUMC1, EQ_SUMC2, DEQ_SUMC1, DEQ_SUMC2);
	}

	public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
		// assume we can just sort the list and get the values
		List<DataValue<Double>> sortedList = makeNewPotsWithSumC(dvs);

		// sortedList.addAll(dvs);
		Collections.sort(sortedList, new Cpr());

		// System.out.println("I'm sorted! " + sortedList.toString());
		return sortedList;
	}

	/**
	 * Creates a list of values comprising the data values supplied, plus all
	 * values obtained by adding each of the sum constants to each of the data
	 * values supplied.
	 * 
	 */
	private List<DataValue<Double>> makeNewPotsWithSumC(List<DataValue<Double>> dvs) {
		Stream<DataValue<Double>> dvWithoutConsts = 
				dvs.stream().filter(dv -> !regularConstants.contains(dv));
		Stream<DataValue<Double>> valAndSums = dvWithoutConsts
				.map(val -> Stream.concat(Stream.of(val), 
					sortedSumConsts.stream()
						.map(sum -> new SumCDataValue<Double>(val, sum))
						.filter(sumc -> !dvs.contains(sumc)))
				).flatMap(s -> s).distinct();
		
		List<DataValue<Double>> valAndSumsAndConsts = Stream.concat(valAndSums, regularConstants.stream())
				.collect(Collectors.toList()); 

		return valAndSumsAndConsts;
	}
	
	/**
	 * The next fresh value, which is the smallest {@link SumCDoubleInequalityTheory#freshStep} multiple greater than the maximum value of the list.
	 */
	public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
		List<DataValue<Double>> valsWithConsts = new ArrayList<>(vals);
		// we add regular constants
		valsWithConsts.addAll(regularConstants);

		DataValue<Double> fv = super.getFreshValue(valsWithConsts);
		Double nextFresh;
		for(nextFresh=0.0; nextFresh<fv.getId(); nextFresh+=freshStep);
		
		
		return new DataValue<Double>(fv.getType(), nextFresh);
	}
	
	
	public IntervalDataValue<Double> pickIntervalDataValue(DataValue<Double> left, DataValue<Double> right) {
		if (right != null && left!=null) 
			if (right.getId() - left.getId() > this.freshStep 
					//&& right.getId() - left.getId() < this.freshStep * 10
					) 
				throw new DecoratedRuntimeException("Right end cannot be more than a fresh step greater than left end ")
				.addDecoration("left", left)
				.addDecoration("right", right)
				.addDecoration("freshStep", this.freshStep);
		return IntervalDataValue.instantiateNew(left, right, smBgStep);
	}

	public Determinizer<Double> getDeterminizer() {
		return new SumCInequalityDeterminizer<Double>(this, this.sortedSumConsts);
	}

	public Collection<DataValue<Double>> getAllNextValues(List<DataValue<Double>> vals) {
		// adds sumc constants to interesting values
		List<DataValue<Double>> potential = getPotential(vals);

		// the superclass should complete this list with in-between values.
		return super.getAllNextValues(potential);
	}

	public List<EnumSet<DataRelation>> getRelations(List<DataValue<Double>> left, DataValue<Double> right) {

		List<EnumSet<DataRelation>> ret = new ArrayList<>();
		for (DataValue<Double> dv : left) {
			DataRelation rel = getRelation(dv, right);
			ret.add(EnumSet.of(rel));
		}
		return ret;
	}
	
	private DataValue<Double> maxSumC() {
		return sortedSumConsts.get(sortedSumConsts.size()-1);
	}
	
	private DataRelation getRelation(DataValue<Double> dv, DataValue<Double> right) {
		DataRelation rel = null;
		if (dv.equals(right))
			rel = DataRelation.EQ;
		else if (dv.getId().compareTo(right.getId()) > 0)
			rel = DataRelation.LT;
		else if ( sortedSumConsts.isEmpty() || 
				Double.valueOf(dv.getId()+ maxSumC().getId()).compareTo(right.getId()) < 0) 
			rel = DataRelation.DEFAULT;
		else 
		{
			Optional<DataValue<Double>> sumcEqual = sortedSumConsts.stream().filter(c -> Double.valueOf(c.getId() + dv.getId())
					.equals(right.getId())).findFirst();
			if (sumcEqual.isPresent()) {
				int ind = sortedSumConsts.indexOf(sumcEqual.get());
				if (ind == 0) 
					rel = DataRelation.EQ_SUMC1; 
				else if (ind == 1) 
					rel = DataRelation.EQ_SUMC2;
				else
					throw new DecoratedRuntimeException("Over 2 sumcs not supported");
			} else {
				Optional<DataValue<Double>> sumcLt = sortedSumConsts.stream().filter(c -> 
				Double.valueOf(c.getId() + dv.getId()).compareTo(right.getId()) > 0).findFirst();
				if (sumcLt.isPresent()) {
					int ind = sortedSumConsts.indexOf(sumcLt.get());
					if (ind == 0) 
						rel = DataRelation.LT_SUMC1; 
					else if (ind == 1) 
						rel = DataRelation.LT_SUMC2;
					else
						throw new DecoratedRuntimeException("Over 2 sumcs not supported");
				} else 
					throw new DecoratedRuntimeException("Exhausted all cases");
			}
		}
		return rel;
	}
}
