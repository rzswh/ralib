package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;


/**
 * First selects a symbol randomly, then concretizes it accounting for the probabilities given.
 */
public class RandomSymbolSelector extends InputSelector{ 

	private double drawRegister;
	private double drawHistory;
	private double drawRelated;
	private boolean uniform;
	private RegisterAutomaton hyp;

	public RandomSymbolSelector(Random rand, Map<DataType, Theory> teachers, Constants constants,
			boolean uniform, double regProb, double hisProb, double relatedProb,
			ParameterizedSymbol[] inputs) {
		super(rand, teachers, constants, inputs);
		this.drawRegister = regProb;
		this.drawHistory = hisProb;
		this.drawRelated = relatedProb;
		this.uniform = uniform;
	}

	
	protected PSymbolInstance nextInput(Word<PSymbolInstance> run, RegisterAutomaton hyp) {
		this.hyp = hyp;
		ParameterizedSymbol ps = nextSymbol(run);
		RALocation location = this.hyp.getLocation(run);
		if (location == null)
			return null;
		PSymbolInstance psi = nextDataValues(run, ps, rand);
		return psi;
	}
	
	private PSymbolInstance nextDataValues(Word<PSymbolInstance> run, ParameterizedSymbol ps, Random rand) {

		DataValue[] vals = new DataValue[ps.getArity()];

		int i = 0;
		for (DataType t : ps.getPtypes()) {
			Theory teacher = teachers.get(t);
			// TODO: generics hack?
			// TODO: add constants?
			final Set<DataValue<Object>> oldSet = DataWords.valSet(run, t);
			
			List<DataValue<?>> regs = getRegisterValuesForType(run, t);
			Double draw = rand.nextDouble();
			if (draw <= drawRegister && !regs.isEmpty()) {
				vals[i] = pick(regs);
			}

			List<DataValue<?>> history = new ArrayList<>(oldSet);
			history.removeAll(regs);
			if (draw > drawRegister && draw <= drawHistory + drawRegister && !history.isEmpty()) {
				vals[i] = pick(history);
			}

			List<DataValue<?>> related = new ArrayList<>(oldSet);
			related.addAll(constants.values(t));
			related = new ArrayList<>(teacher.getAllNextValues(related));
			if (draw > drawRegister + drawHistory && draw <= drawRegister + drawHistory + drawRelated
					&& !related.isEmpty()) {
				vals[i] = pick(related);
			}

			if (vals[i] == null) {
				List<DataValue<?>> old = new ArrayList<>(oldSet);
				old.addAll(constants.values(t));
				vals[i] = teacher.getFreshValue(old);
			}

			i++;
		}
		return new PSymbolInstance(ps, vals);
	}

	private DataValue<?> pick(List<DataValue<?>> list) {
		return list.get(rand.nextInt(list.size()));
	}

	private List<DataValue<?>> getRegisterValuesForType(Word<PSymbolInstance> run, DataType t) {
		List<DataValue<?>> values = new ArrayList<>();
		values.addAll(hyp.getRegisterValuation(run).values(t));
		values.addAll(constants.values(t));
		return values;
	}

	private ParameterizedSymbol nextSymbol(Word<PSymbolInstance> run) {
		ParameterizedSymbol ps = null;
		Map<DataType, Integer> tCount = new LinkedHashMap<>();
		if (uniform) {
			ps = inputs[rand.nextInt(inputs.length)];
		} else {
			int MAX_WEIGHT = 0;
			int[] weights = new int[inputs.length];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1;
				for (DataType t : inputs[i].getPtypes()) {
					Integer old = tCount.get(t);
					if (old == null) {
						// TODO: what about constants?
						old = 0;
					}
					weights[i] *= (old + 1);
					tCount.put(t, ++old);
				}
				MAX_WEIGHT += weights[i];
			}

			int idx = rand.nextInt(MAX_WEIGHT) + 1;
			int sum = 0;
			for (int i = 0; i < inputs.length; i++) {
				sum += weights[i];
				if (idx <= sum) {
					ps = inputs[i];
					break;
				}
			}
		}
		return ps;
	}
}