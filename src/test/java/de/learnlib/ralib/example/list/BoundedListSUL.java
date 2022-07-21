package de.learnlib.ralib.example.list;

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;

public class BoundedListSUL extends DataWordSUL {
	
	public static final DataType intType= new DataType("int", Integer.class);
	public static final InputSymbol PUSH = new InputSymbol("push", new DataType[]{intType});
	public static final InputSymbol ADD = new InputSymbol("add", new DataType[]{intType});
	public static final InputSymbol INSERT = new InputSymbol("insert", new DataType[]{intType, intType});
	public static final InputSymbol POP = new InputSymbol("pop", new DataType[]{});
	public static final InputSymbol CONTAINS = new InputSymbol("contains", new DataType[]{intType});
	public static final OutputSymbol TRUE = new OutputSymbol("TRUE", new DataType[]{});
	public static final OutputSymbol FALSE = new OutputSymbol("FALSE", new DataType[]{});
	public static final OutputSymbol VOID = new OutputSymbol("VOID", new DataType[]{});
	public static final OutputSymbol OPOP = new OutputSymbol("OPOP", new DataType[]{intType});
	public static final OutputSymbol ERR = new OutputSymbol("ERR", new DataType[]{});

	private Supplier<BoundedList> factory;
	private BoundedList list;

	public BoundedListSUL(Supplier<BoundedList> factory) {
		this.factory = factory;
	}
	
	@Override
	public void pre() {
		list = factory.get();
	}

	@Override
	public void post() {
	}

	@Override
	public PSymbolInstance step(PSymbolInstance symInst) throws SULException {
		try {
			if (symInst.getBaseSymbol().equals(PUSH)) {
				list.push((Integer) symInst.getParameterValues()[0].getId());
				return new PSymbolInstance(VOID);
			} else if (symInst.getBaseSymbol().equals(POP)) {
				Integer value = list.pop();
				return new PSymbolInstance(OPOP, new DataValue<Integer>(intType, value)); 
			} else if (symInst.getBaseSymbol().equals(INSERT)) {
				list.insert((Integer) symInst.getParameterValues()[0].getId(), (Integer) symInst.getParameterValues()[1].getId());
				return new PSymbolInstance(VOID);
			} else if (symInst.getBaseSymbol().equals(CONTAINS)) {
				if ( list.contains((Integer) symInst.getParameterValues()[0].getId())) {
					return new PSymbolInstance(TRUE);
				} else {
					return new PSymbolInstance(FALSE);
				}
			}
		} catch (Exception e) {
			return new PSymbolInstance(ERR);
		}
		return null;
	}
	
}
