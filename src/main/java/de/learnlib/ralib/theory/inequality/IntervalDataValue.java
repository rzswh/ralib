package de.learnlib.ralib.theory.inequality;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;

public class IntervalDataValue<T extends Comparable<T>> extends DataValue<T>{
	/**
	 * Constructs interval DVs from left and right ends, by selecting a value
	 * in between. One of left or right can be null, meaning there is no boundary. 
	 * 
	 * In case where there is no boundary, smBgStep is deducted from/added to the end.
	 */
	public static <T extends Comparable<T>>  IntervalDataValue<T>  instantiateNew(DataValue<T> left, DataValue<T> right, DataValue<T> smBgStep) {
		DataType type = left != null ? left.getType() : right.getType();
		Class<T> cls = left != null ? left.getIdType() : right.getIdType();
		
		T intvVal;
		
		// in case either is null, we just provide an increment/decrement
		if (left == null && right != null) {
			// we select a value at least 
			intvVal = cls.cast(DataValue.sub(right, smBgStep).getId());
		} else if (left != null && right == null) {
			intvVal = cls.cast(DataValue.add(left, smBgStep).getId());
		} else if (left != null && right != null) {
			intvVal = pickInBetweenValue(cls, left.getId(), right.getId());
			if (intvVal == null)
				throw new DecoratedRuntimeException("Invalid interval, left end bigger or equal to right end \n ")
				.addDecoration("left", left).addDecoration("right", right);
		} else {
			throw new RuntimeException("Both ends of the Interval cannot be null");
		}

		return new IntervalDataValue<T>(new DataValue<T>(type, intvVal), left, right);
	}
	
	private static <T extends Comparable<T>> T pickInBetweenValue(Class<T> clz, T leftVal, T rightVal) {
		T betweenVal;
		if (leftVal.compareTo(rightVal) >= 0 ) {
			return null;
		}
		
		if (clz.isAssignableFrom(Integer.class)) {
			Integer intVal = 
					Math.addExact((Integer) leftVal, Math.subtractExact((Integer) rightVal, (Integer) leftVal)/2) ;
			betweenVal =  clz.cast( intVal);
		} else {
			if(clz.isAssignableFrom(Double.class)) {
				Double doubleVal = (((Double) rightVal) + ((Double) leftVal))/2 ;
				betweenVal = clz.cast(doubleVal);
			} else if (clz.isAssignableFrom(Long.class)) {
				Long longVal = (((Long) rightVal) + ((Long) leftVal))/2 ;
				betweenVal = clz.cast(longVal);
				} 
				else {
						throw new RuntimeException("Unsupported type " + leftVal.getClass());
				}
		}
		
		return betweenVal;
	}

	private DataValue<T> left;
	private DataValue<T> right;
	
	public IntervalDataValue(DataValue<T> dv, DataValue<T> left, DataValue<T> right) {
		super(dv.getType(), dv.getId());
		this.left = left;
		this.right = right;
	//	assert left.getId().compareTo(right.getId()) < 0;
		
	}
	
	public String toString() {
		return super.toString() + " ( " + (getLeft() != null ? getLeft().toString() : "") + ":" +
					(getRight() != null ? getRight().toString() : "") + ")"; 
	}

	
	public DataValue<T> getLeft() {
		return left;
	}
	
	
	public DataValue<T> toRegular() {
		return new DataValue<T> (type, id);
	}


	public DataValue<T> getRight() {
		return right;
	}
}
