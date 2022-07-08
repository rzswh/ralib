package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SyntacticEquivalenceChecker;
import de.learnlib.ralib.theory.equality.EqualityGuard;

public class SemanticEquivalenceChecker implements de.learnlib.ralib.theory.SDTEquivalenceChecker{
	private MultiTheorySDTLogicOracle logic;
	private Mapping<SymbolicDataValue, DataValue<?>> guardContext;
	private SyntacticEquivalenceChecker syntacticChecker;

	public SemanticEquivalenceChecker(Constants constants, ConstraintSolver cSolver, Mapping<SymbolicDataValue, DataValue<?>> guardContext) {
		this.logic = new MultiTheorySDTLogicOracle(constants, cSolver);
		this.guardContext = guardContext;
		this.syntacticChecker = new SyntacticEquivalenceChecker();
	}
	
	public boolean checkSDTEquivalence(SDTGuard guard1,SDT sdt1,  SDTGuard guard2, SDT sdt2) {
		if (sdt1 instanceof SDTLeaf && sdt2 instanceof SDTLeaf) {
			return sdt1.equals(sdt2);
		}
		
		boolean syntacticEquiv = syntacticChecker.checkSDTEquivalence(guard1, sdt1, guard2, sdt2);
		if (!syntacticEquiv) {
			boolean semanticEquiv = syntacticEquiv;
			List<EqualityGuard> eqGuards =  new ArrayList<EqualityGuard>();
			if (guard1 instanceof EqualityGuard) 
				eqGuards.add((EqualityGuard)guard1);
			if (guard2 instanceof EqualityGuard)
				eqGuards.add((EqualityGuard)guard2);
			
			SDT eqSdt1 = sdt1.relabelUnderEq(eqGuards);
			SDT eqSdt2 = sdt2.relabelUnderEq(eqGuards);
			semanticEquiv = logic.areEquivalent(eqSdt1, new PIV(), new TrueGuardExpression(), eqSdt2, new PIV(), new TrueGuardExpression(),  guardContext);
			return semanticEquiv;
		}
		
		return syntacticEquiv;
	}
	
/*	
 * Case where SAT1 != SAT2 (which shows that we need to compute for SAT1 and SAT2, and cannot just return !SAT1).
	(s3!=s2)
	SDT
	[]-+
	  []-TRUE: s4
	        [Leaf-]

	(s3==s2)
	[]-+
	  []-(s4=s2)
	   |    [Leaf+]
	   +-(s4!=s2)
	        [Leaf-] */
}
