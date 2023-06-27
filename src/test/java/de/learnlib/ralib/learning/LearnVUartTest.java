package de.learnlib.ralib.learning;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.RaLibLearningTestSuite;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.equivalence.IORandomWalk;
import de.learnlib.ralib.example.vuart.VirtualUartSUL;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.ConstraintSolverFactory;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.theories.IntegerEqualityTheory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.testng.annotations.Test;

public class LearnVUartTest extends RaLibLearningTestSuite {
    
    @Test
    public void learnLoginExampleIO() {
        // LearnLogger.getLogger(IORandomWalk.class).setLevel(Level.FINE);

        final Map<DataType, Theory> teachers = new LinkedHashMap<>();
        teachers.put(VirtualUartSUL.BYTE_TYPE,
                new IntegerEqualityTheory(VirtualUartSUL.BYTE_TYPE));

        final Constants consts = new Constants();
        final SymbolicDataValueGenerator.ConstantGenerator cgen = new SymbolicDataValueGenerator.ConstantGenerator();
        consts.put(cgen.next(VirtualUartSUL.BYTE_TYPE), new DataValue<Integer>(VirtualUartSUL.BYTE_TYPE, 0));

        VirtualUartSUL sul = new VirtualUartSUL();
        ConstraintSolver solv = ConstraintSolverFactory.createSimpleConstraintSolver();

        super.runIOLearningExperiments(sul, teachers, consts, false, solv, sul.getActionSymbols(), VirtualUartSUL.ERROR);

        /* Legacy code */
        /*
        IOOracle ioOracle = new SULOracle(sul, VirtualUartSUL.ERROR);

        MultiTheoryTreeOracle mto = TestUtil.createMTO(
                ioOracle, teachers, consts, solv, sul.getInputSymbols());

        MultiTheorySDTLogicOracle mlo
                = new MultiTheorySDTLogicOracle(consts, solv);

        TreeOracleFactory hypFactory = (RegisterAutomaton hyp)
                -> new MultiTheoryTreeOracle(new SimulatorOracle(hyp), teachers, consts, solv);

        RaStar rastar = new RaStar(mto, hypFactory, mlo,
                consts, true, sul.getActionSymbols());

        IORandomWalk iowalk = new IORandomWalk(random,
                sul,
                false, // do not draw symbols uniformly 
                0.1, // reset probability 
                0.8, // prob. of choosing a fresh data value
                10000, // 1000 runs 
                100, // max depth
                consts,
                false, // reset runs 
                teachers,
                sul.getInputSymbols());

        IOCounterexampleLoopRemover loops = new IOCounterexampleLoopRemover(ioOracle);
        IOCounterExamplePrefixReplacer asrep = new IOCounterExamplePrefixReplacer(ioOracle);
        IOCounterExamplePrefixFinder pref = new IOCounterExamplePrefixFinder(ioOracle);

        int check = 0;
        while (check < 100) {
            check++;
            rastar.learn();
            Hypothesis hyp = rastar.getHypothesis();
            System.out.println(hyp.toString());

            DefaultQuery<PSymbolInstance, Boolean> ce
                    = iowalk.findCounterExample(hyp, null);

            System.out.println("CE: " + ce);
            if (ce == null) {
                break;
            }

        //     ce = loops.optimizeCE(ce.getInput(), hyp);
        //     ce = asrep.optimizeCE(ce.getInput(), hyp);
        //     ce = pref.optimizeCE(ce.getInput(), hyp);
            System.out.println("CE opted: " + ce);
            rastar.addCounterexample(ce);
        }

        RegisterAutomaton hyp = rastar.getHypothesis();
        RegisterAutomatonImporter imp = TestUtil.getLoader(
                "/de/learnlib/ralib/automata/xml/vuart.xml");

        IOEquivalenceTest checker = new IOEquivalenceTest(
                imp.getRegisterAutomaton(), teachers, consts, true,
                sul.getActionSymbols()
        );

        Assert.assertNull(checker.findCounterExample(hyp, null));
         */

    }
}
