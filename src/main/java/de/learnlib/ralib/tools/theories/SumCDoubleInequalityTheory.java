package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class SumCDoubleInequalityTheory extends DoubleInequalityTheory {
	//maxSumC : the biggest sum constant
	
	// maxSumC multiplied this factor results in the smaller/bigger step. Smaller or bigger intervals are generated by subtracting
	// or adding this step from the minimum / to the maximum.
	private static int smBgFactor = 10;  
	
	// maxSumC multiplied this factor results in the fresh step. Fresh values at a fresh step distance, 
	// in increasing order, starting from 0.
	private static int freshFactor = 100; 
	
	private List<DataValue<Double>> sortedSumConsts;
	private List<DataValue<Double>> regularConstants;

	// fresh step
	private Double freshStep;
	// step for selecting a smaller value or a bigger value
	private DataValue<Double> smBgStep;
	
	
	public SumCDoubleInequalityTheory() {
	}
	
	// the idea is that a fresh value should be far enough away s.t. it is always bigger than all previous values.
	
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
		this.sortedSumConsts = new ArrayList<>(sumConstants);
		Collections.sort(this.sortedSumConsts, new Cpr());
		this.regularConstants = regularConstants;
		DataValue<Double> maxSumC = this.sortedSumConsts.isEmpty() ? DataValue.ZERO(this.getType())
				: maxSumC();
		this.freshStep = maxSumC.getId() * freshFactor;
		this.smBgStep = new DataValue<Double>(type, maxSumC.getId() * smBgFactor);
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
				).flatMap(s -> s).distinct()
				.filter(dv -> !canRemove(dv));
		
		List<DataValue<Double>> valAndSumsAndConsts = Stream.concat(valAndSums, regularConstants.stream())
				.collect(Collectors.toList()); 

		return valAndSumsAndConsts;
	}
	
	// if a value is already a SumCDv, it can only be operand in another SumCDv if its sum constant is ONE.
	// this is a hack optimization for TCP
	private boolean canRemove(DataValue<Double> dv) {
//		Set<Object> sumCsOtherThanOne = new HashSet<Object>();
//		while (dv instanceof SumCDataValue) {
//			SumCDataValue<Double> sum = ((SumCDataValue<Double>) dv);
//			if (!DataValue.ONE(this.getType()).equals(sum.getConstant())) {
//				if (sumCsOtherThanOne.contains(sum.getConstant()))
//					return true;
//				sumCsOtherThanOne.add(sum.getConstant());
//			}
//			dv = sum.getOperand();
//		}
		return false;
	}

	/**
	 * The next fresh value
	 */
	public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
		List<DataValue<Double>> valsWithConsts = new ArrayList<>(vals);
		// we add regular constants
		valsWithConsts.addAll(this.regularConstants);

		DataValue<Double> fv = super.getFreshValue(valsWithConsts);
		Double nextFresh;
		for(nextFresh=0.0; nextFresh<fv.getId(); nextFresh+=this.freshStep);
		
		
		return new DataValue<Double>(fv.getType(), nextFresh);
	}
	
	
	public IntervalDataValue<Double> pickIntervalDataValue(DataValue<Double> left, DataValue<Double> right) {
		if (right != null && left!=null) 
			if (right.getId() - left.getId() > this.freshStep && right.getId() - left.getId() < this.freshStep * 10) 
				throw new DecoratedRuntimeException("This shouldn't be happening").addDecoration("left", left).addDecoration("right", right);
		return IntervalDataValue.instantiateNew(left, right, smBgStep);
	}

	public ValueMapper<Double> getValueMapper() {
		return new SumCInequalityValueMapper<Double>(this, this.sortedSumConsts);
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
	
	private DataValue<Double> maxSumC () {
		return this.sortedSumConsts.get(sortedSumConsts.size()-1);
	}
	
	private DataRelation getRelation(DataValue<Double> dv, DataValue<Double> right) {
		DataRelation rel = null;
		if (dv.equals(right))
			rel = DataRelation.EQ;
		else if (dv.getId().compareTo(right.getId()) > 0)
			rel = DataRelation.LT;
		else if ( Double.valueOf(dv.getId()+ maxSumC().getId()).compareTo(right.getId()) < 0) 
			rel = DataRelation.DEFAULT;
		else 
		{
			Optional<DataValue<Double>> sumcEqual = sortedSumConsts.stream().filter(c -> Double.valueOf(c.getId() + dv.getId())
					.equals(right.getId())).findAny();
			if (sumcEqual.isPresent()) {
				int ind = this.sortedSumConsts.indexOf(sumcEqual.get());
				if (ind == 0) 
					rel = DataRelation.EQ_SUMC1; 
				else if (ind == 1) 
					rel = DataRelation.EQ_SUMC2;
				else
					throw new DecoratedRuntimeException("Over 2 sumcs not supported");
			} else {
				Optional<DataValue<Double>> sumcLt = sortedSumConsts.stream().filter(c -> 
				Double.valueOf(c.getId() + dv.getId()).compareTo(right.getId()) > 0).findAny();
				if (sumcLt.isPresent()) {
					int ind = this.sortedSumConsts.indexOf(sumcLt.get());
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
