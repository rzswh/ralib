package de.learnlib.ralib.example.vuart;

import javax.annotation.Nullable;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class VirtualUartSUL extends DataWordSUL {

    public static final DataType BYTE_TYPE = new DataType("BYTE", Integer.class);

    public static final ParameterizedSymbol EX_SEND = 
            new InputSymbol("externalSend", new DataType[]{BYTE_TYPE});
    public static final ParameterizedSymbol EX_RECV = 
            new InputSymbol("externalRecv", new DataType[]{});
    public static final ParameterizedSymbol MMIO_SEND = 
            new InputSymbol("mmioSend", new DataType[]{BYTE_TYPE});
    public static final ParameterizedSymbol MMIO_RECV = 
            new InputSymbol("mmioRecv", new DataType[]{});
    public static final ParameterizedSymbol WAIT = 
            new InputSymbol("waitT", new DataType[]{});
    public static final ParameterizedSymbol GET_FLAGS = 
            new InputSymbol("getFlags", new DataType[]{});
    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { EX_SEND, EX_RECV, MMIO_SEND, MMIO_RECV, WAIT, GET_FLAGS };
    }

    public static final ParameterizedSymbol EX_VAL = new OutputSymbol("_ex_val", new DataType[]{BYTE_TYPE});
    public static final ParameterizedSymbol MMIO_VAL = new OutputSymbol("_mmio_val", new DataType[]{BYTE_TYPE});
    public static final ParameterizedSymbol VOID = new OutputSymbol("_void", new DataType[]{});
    public static final ParameterizedSymbol ERROR = new OutputSymbol("_error", new DataType[]{});
    public static final ParameterizedSymbol genFlagsSym(int flag) {
        return new OutputSymbol("_flags_" + String.valueOf(flag), new DataType[]{});
    }
    public static final ParameterizedSymbol[] FLAGS = new ParameterizedSymbol[4];
    static {
        for (int i = 0; i < 4; i++) {
            FLAGS[i] = genFlagsSym(i);
        }
    }
    
    public final ParameterizedSymbol[] getOutputSymbols() {
        return new ParameterizedSymbol[] { EX_VAL, MMIO_VAL, VOID, ERROR, FLAGS[0], FLAGS[1], FLAGS[2], FLAGS[3] };
    }

    public final ParameterizedSymbol[] getActionSymbols() {
        ParameterizedSymbol[] inputs = getInputSymbols();
        ParameterizedSymbol[] outputs = getOutputSymbols();
        ParameterizedSymbol[] actions = new ParameterizedSymbol[inputs.length + outputs.length];
        for (int i = 0; i < inputs.length; i++) {
            actions[i] = inputs[i];
        }
        for (int i = 0; i < outputs.length; i++) {
            actions[i + inputs.length] = outputs[i];
        }
        return actions;
    }

    private VirtualUart vuart;

    @Override
    public void pre() {
        countResets(1);
        this.vuart = new VirtualUart();
    }

    @Override
    public void post() {
        this.vuart = null;
    }

    @Override
    public PSymbolInstance step(@Nullable PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i == null) {
            throw new IllegalStateException("null input symbol");
        } else if (i.getBaseSymbol().equals(EX_SEND)) {
            vuart.externalSend(((Integer)i.getParameterValues()[0].getId()).byteValue());
            return new PSymbolInstance(VOID);
        } else if (i.getBaseSymbol().equals(MMIO_SEND)) {
            vuart.mmioSend(((Integer)i.getParameterValues()[0].getId()).byteValue());
            return new PSymbolInstance(VOID);
        } else if (i.getBaseSymbol().equals(EX_RECV)) {
            try {
                byte ret = vuart.externalRecv();
                return new PSymbolInstance(EX_VAL, new DataValue<Integer>(BYTE_TYPE, (int)ret));
            } catch (IllegalAccessError e) {
                return new PSymbolInstance(VOID);
            }
        } else if (i.getBaseSymbol().equals(MMIO_RECV)) {
            try {
                byte ret = vuart.mmioRecv();
                return new PSymbolInstance(MMIO_VAL, new DataValue<Integer>(BYTE_TYPE, (int)ret));
            } catch (IllegalAccessError e) {
                return new PSymbolInstance(VOID);
            }
        } else if (i.getBaseSymbol().equals(WAIT)) {
            vuart.waitT();
            return new PSymbolInstance(VOID);
        } else if (i.getBaseSymbol().equals(GET_FLAGS)) {
            byte flag = vuart.getFlags();
            assert flag <= 3 && flag >= 0;
            return new PSymbolInstance(FLAGS[flag]);
        } else {
            throw new IllegalStateException("undefined input symbol");
        }
    }

    public static PSymbolInstance genSym(ParameterizedSymbol s, DataValue<?>... d) {
        return new PSymbolInstance(s, d);
    }
    private static DataValue<Byte> genVal(int v) {
        return new DataValue<Byte>(BYTE_TYPE, (byte)v);
    }

    @Test
    public static void testSymbols() {
        Assert.assertTrue(genSym(EX_SEND).equals(genSym(EX_SEND)));
        Assert.assertFalse(genSym(EX_SEND).equals(genSym(EX_RECV, genVal(1))));
        Assert.assertFalse(genSym(EX_SEND).equals(genSym(FLAGS[1])));
        Assert.assertFalse(genSym(EX_VAL, genVal(1)).equals(genSym(EX_RECV, genVal(1))));
        Assert.assertTrue(genSym(EX_RECV, genVal(1)).equals(genSym(EX_RECV, genVal(1))));
        Assert.assertFalse(genSym(EX_RECV, genVal(1)).equals(genSym(EX_RECV, genVal(12))));
    }
}
