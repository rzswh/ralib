package de.learnlib.ralib.oracles.mto;

import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ConstantGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class HasCounterexampleTest {
	
	
    @Test
    public void testNoCounterexample() {
    	DataType intType = new DataType("int", Integer.class);
    	InputSymbol parameterizedSymbol = new InputSymbol("sym", intType); 
    	
    	ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
    	RegisterGenerator rgen = new SymbolicDataValueGenerator.RegisterGenerator();
    	ConstantGenerator cgen = new SymbolicDataValueGenerator.ConstantGenerator();
    	SuffixValueGenerator sgen = new SymbolicDataValueGenerator.SuffixValueGenerator();
    	
    	Parameter p1 = pgen.next(intType);
    	Parameter p2 = pgen.next(intType);
    	Register r1 = rgen.next(intType);
    	Register r2 = rgen.next(intType);
    	Constant c1 = cgen.next(intType);
    	SuffixValue s1 = sgen.next(intType);
    	SuffixValue s2 = sgen.next(intType);
    	
    	Constants consts = new Constants();
    	consts.put(c1, new DataValue<Integer>(intType, 0));
    	
    	MultiTheorySDTLogicOracle oracle = new MultiTheorySDTLogicOracle(consts, TestUtil.getZ3Solver());
    	
    	// original
    	Map<SDTGuard, SDT> s1eqc1Map = new LinkedHashMap<SDTGuard, SDT>();
    	s1eqc1Map.put(new EqualityGuard(s2, r1), SDTLeaf.REJECTING);
    	s1eqc1Map.put(new DisequalityGuard(s2, r1), SDTLeaf.ACCEPTING);
    	
    	Map<SDTGuard, SDT> s1eqr1Map = new LinkedHashMap<SDTGuard, SDT>();
    	s1eqr1Map.put(new SDTTrueGuard(s2), SDTLeaf.REJECTING);
    	
    	Map<SDTGuard, SDT> s1eqr2Map = new LinkedHashMap<SDTGuard, SDT>();
    	s1eqr2Map.put(new EqualityGuard(s2, r1), SDTLeaf.REJECTING);
    	s1eqr2Map.put(new DisequalityGuard(s2, r1), SDTLeaf.ACCEPTING);
    	
    	Map<SDTGuard, SDT> s1deqMap = new LinkedHashMap<SDTGuard, SDT>();
    	s1deqMap.put(new EqualityGuard(s2, c1), SDTLeaf.ACCEPTING);
    	s1deqMap.put(new EqualityGuard(s2, r2), SDTLeaf.ACCEPTING);
    	s1deqMap.put(new EqualityGuard(s2, s1), SDTLeaf.ACCEPTING);
    	s1deqMap.put(new SDTAndGuard(s2, new DisequalityGuard(s2, c1), new DisequalityGuard(s2, r2), new DisequalityGuard(s2, s1)), SDTLeaf.REJECTING);
    	
    	Map<SDTGuard, SDT> sdtMap = new LinkedHashMap<SDTGuard, SDT>();
    	sdtMap.put(new EqualityGuard(s1, c1), new SDT(s1eqc1Map));
    	sdtMap.put(new EqualityGuard(s1, r1), new SDT(s1eqr1Map));
    	sdtMap.put(new EqualityGuard(s1, r2), new SDT(s1eqr2Map));
    	sdtMap.put(new SDTAndGuard(s1, new DisequalityGuard(s1, c1), new DisequalityGuard(s1, r1), new DisequalityGuard(s1, r2)), new SDT(s1deqMap));
    	
// 	    simpler SDT
//    	Map<SDTGuard, SDT> s1eqr1Map = new LinkedHashMap<SDTGuard, SDT>();
//    	s1eqr1Map.put(new SDTTrueGuard(s2), SDTLeaf.ACCEPTING);
//    	
//    	Map<SDTGuard, SDT> s1eqr2Map = new LinkedHashMap<SDTGuard, SDT>();
//    	s1eqr2Map.put(new SDTTrueGuard(s2), SDTLeaf.REJECTING);
//    	
//    	Map<SDTGuard, SDT> s1deqMap = new LinkedHashMap<SDTGuard, SDT>();
//    	s1deqMap.put(new EqualityGuard(s2, r2), SDTLeaf.ACCEPTING);
//    	s1deqMap.put(new EqualityGuard(s2, s1), SDTLeaf.ACCEPTING);
//    	s1deqMap.put(new SDTAndGuard(s2, new DisequalityGuard(s2, r2), new DisequalityGuard(s2, s1)), SDTLeaf.REJECTING);
//    	
//    	Map<SDTGuard, SDT> sdtMap = new LinkedHashMap<SDTGuard, SDT>();
//    	sdtMap.put(new EqualityGuard(s1, r1), new SDT(s1eqr1Map));
//    	sdtMap.put(new EqualityGuard(s1, r2), new SDT(s1eqr2Map));
//    	sdtMap.put(new SDTAndGuard(s1, new DisequalityGuard(s1, r1), new DisequalityGuard(s1, r2)), new SDT(s1deqMap));
    	
    	SDT sdt = new SDT(sdtMap);
    	
    	Word<PSymbolInstance> prefixWithTrans = Word.fromSymbols(new PSymbolInstance(parameterizedSymbol, new DataValue<Integer>(intType, 1)),
    			new PSymbolInstance(parameterizedSymbol, new DataValue<Integer>(intType, 2)),
    			new PSymbolInstance(parameterizedSymbol, new DataValue<Integer>(intType, 3))
    			);
    	Word<PSymbolInstance> prefix = prefixWithTrans.prefix(-1);
    	PIV piv = new PIV();
    	piv.put(p1, r1);
    	piv.put(p2, r2);
    	
    	Assert.assertFalse(oracle.hasCounterexample(prefix, sdt, piv, sdt, piv, new TransitionGuard(), prefixWithTrans));
    }
}
