package de.learnlib.ralib.data;

import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;

public interface SymbolicDataExpression {
	public SymbolicDataValue getSDV();

	public default boolean isRegister() {
		return this.getClass().equals(Register.class);
	}

	public default boolean isParameter() {
		return this.getClass().equals(Parameter.class);
	}

	public default boolean isConstant() {
		return this.getClass().equals(Constant.class);
	}
	
	public default boolean isSumCExpression() {
		return this.getClass().equals(SumCDataExpression.class);
	}

	public default boolean isSuffixValue() {
		return this.getClass().equals(SuffixValue.class);
	}
	
	public default boolean isSDV() {
		return isRegister() || isParameter() || isConstant() || isSuffixValue();
	}
	
	
	/**
	 * Constructs a new symbolic data expression where the register is swapped
	 * by a new one. 
	 */
	public default SymbolicDataExpression swapSDV(SymbolicDataValue newSDV) {
		return newSDV;
	}
	
	public default SymbolicDataExpression relabel(VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue> relabeling) {
		if (relabeling.containsKey(getSDV())) {
			SymbolicDataValue newSDV = relabeling.get(getSDV());
			return swapSDV(newSDV);
		}
		return this;
	}
	
	/**
	 * Given a valuation of the encapsulated SDVs, instantiates the expression.
	 */
	public DataValue<?> instantiateExprForValuation(Mapping<SymbolicDataValue, DataValue<?>> valuation);
	
	public DataType getType();
}
