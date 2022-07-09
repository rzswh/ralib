package de.learnlib.ralib.learning;

import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.CONTAINS;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INSERT;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.POP;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.PUSH;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.intType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.example.list.BoundedList;
import de.learnlib.ralib.example.list.BoundedListDataWordOracle;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class LearnBoundedListTest {

	@Test
	public void learnBoundedListTest() {
		List<Word<PSymbolInstance>> ces = Arrays.asList(Word.fromSymbols(new PSymbolInstance(INSERT, dv(0), dv(1)), new PSymbolInstance(POP, dv(1))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(POP, dv(0))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(INSERT, dv(1), dv(2)), new PSymbolInstance(POP, dv(0)), new PSymbolInstance(POP, dv(2))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(PUSH, dv(1)), new PSymbolInstance(POP, dv(1)), new PSymbolInstance(POP, dv(0))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(PUSH, dv(1)), new PSymbolInstance(INSERT, dv(2), dv(3)), 
						new PSymbolInstance(PUSH, dv(4)), new PSymbolInstance(POP, dv(1))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(CONTAINS, dv(0))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(CONTAINS, dv(0))));
//		
//		Hypothesis hyp = learnBoundedList(3, false, 
//				Word.fromSymbols(new PSymbolInstance(INSERT, dv(0), dv(1)), new PSymbolInstance(POP, dv(1))),
//				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(POP, dv(0))),
//				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(INSERT, dv(1), dv(2)), new PSymbolInstance(POP, dv(0)), new PSymbolInstance(POP, dv(2))),
//				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(PUSH, dv(1)), new PSymbolInstance(POP, dv(1)), new PSymbolInstance(POP, dv(0))),
//				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(PUSH, dv(1)), new PSymbolInstance(INSERT, dv(2), dv(3)), 
//						new PSymbolInstance(PUSH, dv(4)), new PSymbolInstance(POP, dv(1))),
//				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(CONTAINS, dv(0))),
//				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(CONTAINS, dv(0)))
//				);
//		System.out.println(hyp);
		
		Random rand = new Random(0);
		for (int i=0; i<100; i++) {
			Collections.shuffle(ces);
			learnBoundedList(4, true, ces.toArray(new Word[ces.size()])); 
		}
		
	}

	public Hypothesis learnBoundedList(int size, boolean useNull, Word<PSymbolInstance>... ces) {
		Constants consts = new Constants();
		if (useNull) {
			consts.put(new Constant(intType, 1), new DataValue<>(intType, BoundedList.NULL_VALUE));
		}

		BoundedListDataWordOracle dwOracle = new BoundedListDataWordOracle(() -> new BoundedList(size, useNull));

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		IntegerEqualityTheory dit = new IntegerEqualityTheory(intType);
		teachers.put(intType, dit);

		JConstraintsConstraintSolver jsolv = TestUtil.getZ3Solver();
		MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(dwOracle, null, teachers, consts, jsolv);

		SDTLogicOracle mlo = new MultiTheorySDTLogicOracle(consts, jsolv);

		TreeOracleFactory hypFactory = (RegisterAutomaton hyp) -> TestUtil.createSimulatorMTO(hyp, teachers,
				consts, jsolv);

		RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, teachers, jsolv, PUSH, INSERT, POP, CONTAINS);

		rastar.learn();
		Hypothesis hypothesis = rastar.getHypothesis();
		for (Word<PSymbolInstance> ce : ces) {
			rastar.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, dwOracle.answerQuery(ce)));
			rastar.learn();
			hypothesis = rastar.getHypothesis();
		}
		return hypothesis;
	}
	
	private DataValue dv(int val) {
		return new DataValue<Integer>(intType, val);
	}
}
