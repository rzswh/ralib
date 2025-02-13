package de.learnlib.ralib.example.list;

import java.util.Collection;
import java.util.function.Supplier;

import de.learnlib.api.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class BoundedListDataWordOracle implements DataWordOracle {
	public static final DataType intType= new DataType("int", Double.class);

	public static final InputSymbol PUSH = new InputSymbol("push", new DataType[]{intType});
	public static final InputSymbol INSERT = new InputSymbol("insert", new DataType[]{intType, intType});
	public static final InputSymbol POP = new InputSymbol("pop", new DataType[]{intType});
	public static final InputSymbol CONTAINS = new InputSymbol("contains", new DataType[]{intType});
	public static final InputSymbol ERR = new InputSymbol("exception", new DataType[]{});
	

	private Supplier<BoundedList> factory;

	public BoundedListDataWordOracle(Supplier<BoundedList> factory) {
		this.factory = factory;
	}

	@Override
	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		for (Query<PSymbolInstance, Boolean> query : queries) {
			query.answer(answer(query.getInput()));
		}
	}

	private Boolean answer(Word<PSymbolInstance> input) {
		BoundedList list = factory.get();
		for (PSymbolInstance symInst : input) {
			boolean output = getOutput(symInst, list);
			if (!output) {
				return false;
			}
		}
		
		return true;
	}

	private boolean getOutput(PSymbolInstance symInst, BoundedList list) {
		try {
			if (symInst.getBaseSymbol().equals(PUSH)) {
				list.push((Integer) symInst.getParameterValues()[0].getId());
				return true;
			} else if (symInst.getBaseSymbol().equals(POP)) {
				Integer value = list.pop();
				return symInst.getParameterValues()[0].getId().equals(value); 
			} else if (symInst.getBaseSymbol().equals(INSERT)) {
				list.insert((Integer) symInst.getParameterValues()[0].getId(), (Integer) symInst.getParameterValues()[1].getId());
				return true;
			} else if (symInst.getBaseSymbol().equals(CONTAINS)) {
				return list.contains((Integer) symInst.getParameterValues()[0].getId());
			}
		} catch (Exception e) {
		}
		
		return false;
	}
}
