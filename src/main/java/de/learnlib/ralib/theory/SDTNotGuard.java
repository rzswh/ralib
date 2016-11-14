package de.learnlib.ralib.theory;

import java.util.Set;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.data.Replacement;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

public class SDTNotGuard extends SDTGuard{
	private SDTGuard ifGuard;

	public SDTNotGuard(SDTGuard ifGuard) {
		super(ifGuard.getParameter());
		this.ifGuard = ifGuard;
	}

	public GuardExpression toExpr() {
		return new Negation(this.ifGuard.toExpr());
	}

	public SDTGuard relabel(VarMapping relabelling) {
		SDTGuard newIfGuard = this.ifGuard.relabel(relabelling);
		return new SDTNotGuard(newIfGuard);
	}

	public SDTGuard replace(Replacement replacing) {
		SDTIfGuard newIfGuard = (SDTIfGuard) this.ifGuard.replace(replacing); 
		return new SDTNotGuard(newIfGuard);
	}

	public Set<SymbolicDataValue> getAllSDVsFormingGuard() {
		return this.ifGuard.getAllSDVsFormingGuard();
	}

}