package de.learnlib.ralib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.HypVerifier;
import de.learnlib.ralib.equivalence.IOEquivalenceOracle;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * A very basic random equivalence oracle for DataWordOracle
 */
public class BasicEquivalenceOracle implements IOEquivalenceOracle  {
	
	private Map<DataType, Theory> teachers;
	private Constants consts;
	private ConstraintSolver solver;
	private Random random;
	private int bound;
	private int depth = 10;
	private double resetProb = 0.1;
	private double freshProb = 0.5;
	private List<ParameterizedSymbol> symbols;
	private DataWordOracle wordOracle;

	public BasicEquivalenceOracle(DataWordOracle membershipOracle, Map<DataType, Theory> teachers, Constants consts,
			List<ParameterizedSymbol> symbols, ConstraintSolver solver, int depth, int bound, Random random) {
		this.wordOracle = membershipOracle;
		this.teachers = teachers;
		this.consts = consts;
		this.solver = solver;
		this.random = random;
		this.depth = depth;
		this.bound = bound;
		this.symbols = symbols;
	}

	@Override
	public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(RegisterAutomaton hypothesis,
			Collection<? extends PSymbolInstance> inputs) {
		HypVerifier verifier = HypVerifier.getVerifier(false, teachers, consts);
		for (int i=0; i<bound; i++) {
			Word<PSymbolInstance> randomWord = generateRandomWord(symbols);
			DefaultQuery<PSymbolInstance, Boolean> query = new DefaultQuery<PSymbolInstance, Boolean>(randomWord);
			wordOracle.processQuery(query);
			boolean ce = verifier.isCEForHyp(query, hypothesis);
			if (ce) {
				return query;
			}
		}
		
		return null;
	}
	
	/**
	 * Implements a very basic random data word generation algorithm.
	 */
	public Word<PSymbolInstance> generateRandomWord(List<ParameterizedSymbol> symbols) {
		WordBuilder<PSymbolInstance> wordBuilder = new WordBuilder<PSymbolInstance>();
		Map<DataType, List<DataValue>> values = new HashMap<>();
		for (int i=0; i<depth; i++) {
			ParameterizedSymbol randSym = symbols.get(random.nextInt(symbols.size()));
			int arity = randSym.getArity();
			DataValue [] dataValues = new DataValue [arity];
			for (int pi=0; pi<arity; pi++) {
				DataType type = randSym.getPtypes()[pi];
				Theory teacher = teachers.get(type);
				List<DataValue> typeValues = values.get(type);
				if (typeValues == null) {
					typeValues = new LinkedList<>();
					values.put(type, typeValues);
				}
				DataValue value;
				
				if (random.nextDouble() < freshProb) {
					value = teacher.getFreshValue(typeValues);
				} else {
					Collection<DataValue> nextValues = teacher.getAllNextValues(typeValues);
					value = new ArrayList<DataValue>(nextValues).get(random.nextInt(nextValues.size()));
				}
				dataValues[pi] = value;
			}
			
			wordBuilder.add(new PSymbolInstance(randSym, dataValues));
			if (random.nextDouble() < resetProb) {
				break;
			}
		}
		
		return wordBuilder.toWord();
	}

}
