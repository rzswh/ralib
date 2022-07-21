package de.learnlib.ralib.learning;

import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.INSERT;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.POP;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.PUSH;
import static de.learnlib.ralib.example.list.BoundedListDataWordOracle.intType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;

import org.testng.annotations.Test;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.BasicEquivalenceOracle;
import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.example.list.BoundedList;
import de.learnlib.ralib.example.list.BoundedListDataWordOracle;
import de.learnlib.ralib.example.list.BoundedListSUL;
import de.learnlib.ralib.oracles.SDTLogicOracle;
import de.learnlib.ralib.oracles.TreeOracleFactory;
import de.learnlib.ralib.oracles.mto.MultiTheorySDTLogicOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class LearnBoundedListTest extends RaLibLearningTestSuite {

//	@Test
	public void learnBoundedListDWOracleTest() {
		List<Word<PSymbolInstance>> ces = Arrays.asList(Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(POP, dv(1))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(INSERT, dv(1), dv(2)), new PSymbolInstance(INSERT, dv(3), dv(4)), new PSymbolInstance(POP, dv(0)), new PSymbolInstance(POP, dv(2))),
				Word.fromSymbols(new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(PUSH, dv(0)), new PSymbolInstance(INSERT, dv(0), dv(1)), new PSymbolInstance(POP, dv(1)))
				);
		
		learnBoundedListDWOracle(3, false, new Random(0), ces.toArray(new Word[ces.size()]));
	}

	private Hypothesis learnBoundedListDWOracle(int size, boolean useNull, Random seed, Word<PSymbolInstance> [] ces) {
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
		List<ParameterizedSymbol> alphabet = Arrays.asList(INSERT, PUSH, POP);
		RaStar rastar = new RaStar(mto, hypFactory, mlo, consts, teachers, jsolv, alphabet.toArray(new ParameterizedSymbol[alphabet.size()]));
		
		rastar.learn();
		Hypothesis hypothesis = rastar.getHypothesis();

//		BasicEquivalenceOracle eqOracle = new BasicEquivalenceOracle(dwOracle, teachers, consts, alphabet, jsolv, size + 2, 20000, seed);
//		while (ce != null) {
//			System.out.println("CE: " + ce);
//			rastar.addCounterexample(ce);
//			rastar.learn();
//			hypothesis = rastar.getHypothesis();
//			ce = eqOracle.findCounterExample(hypothesis, Collections.emptyList());;
//		}
//		
		for (Word<PSymbolInstance> ce : ces) {
			rastar.addCounterexample(new DefaultQuery<PSymbolInstance, Boolean>(ce, dwOracle.answerQuery(ce)));
			rastar.learn();
			hypothesis = rastar.getHypothesis();
		}
		return hypothesis;
	}
	
//	@Test
	public void learnBoundedListSUL() {
		Constants consts = new Constants();
//		consts.put(new Constant(BoundedListSUL.intType, 1), new DataValue<Integer>(BoundedListSUL.intType, BoundedList.NULL_VALUE));
		BoundedListSUL dwSUL = new BoundedListSUL(() -> new BoundedList(3, false));

		final Map<DataType, Theory> teachers = new LinkedHashMap<>();
		IntegerEqualityTheory dit = new IntegerEqualityTheory(BoundedListSUL.intType);
		teachers.put(BoundedListSUL.intType, dit);
		super.setSeeds(LongStream.range(9, 10).toArray());
		super.getEquOracleBuilder().setMaxRuns(20000);
		super.getEquOracleBuilder().setMaxDepth(6);
		
		super.runIOLearningExperiments(dwSUL, teachers, consts, false, TestUtil.getZ3Solver(), new ParameterizedSymbol [] {
				de.learnlib.ralib.example.list.BoundedListSUL.PUSH,
				de.learnlib.ralib.example.list.BoundedListSUL.INSERT,
				de.learnlib.ralib.example.list.BoundedListSUL.POP,
				de.learnlib.ralib.example.list.BoundedListSUL.VOID,
				de.learnlib.ralib.example.list.BoundedListSUL.OPOP,
				de.learnlib.ralib.example.list.BoundedListSUL.ERR,
		}, de.learnlib.ralib.example.list.BoundedListSUL.ERR);
	}
	
	private DataValue dv(int val) {
		return new DataValue<Integer>(intType, val);
	}
}
