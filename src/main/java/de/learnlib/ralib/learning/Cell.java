/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.learning;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collection;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * A cell of an observation table.
 *
 * @author falk
 */
final class Cell {

    private final Word<PSymbolInstance> prefix;

    private final GeneralizedSymbolicSuffix suffix;

    private final SymbolicDecisionTree sdt;

    private final PIV parsInVars;

    private static LearnLogger log = LearnLogger.getLogger(Cell.class);

    private Cell(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
            SymbolicDecisionTree sdt, PIV parsInVars) {

        this.prefix = prefix;
        this.suffix = suffix;
        this.sdt = sdt;
        this.parsInVars = parsInVars;
    }

    Collection<Parameter> getMemorable() {
        return parsInVars.keySet();
    }

    PIV getParsInVars() {
        return parsInVars;
    }

    /**
     * checks whether the SDTs of the two cells are equal under a renaming from the 
     * SDT in {@code other} to to the SDT in this {@code this} 
     *
     * @param other
     * @param solver TODO
     * @return
     */
    boolean isEquivalentTo(Cell other, VarMapping renaming, ConstraintSolver solver) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }
        
        boolean check = this.suffix.equals(other.suffix)
//                && this.parsInVars.relabel(renaming).equals(other.parsInVars)
                && this.sdt.isEquivalent(other.sdt, renaming, solver);
        // log.log(Level.FINEST, this.sdt + "\nVS\n" + other.sdt + "\n");
        // log.log(Level.FINEST, this.suffix + "    " + other.suffix);
        // log.log(Level.FINEST, this.suffix.equals(other.suffix) + " " + this.parsInVars.relabel(renaming).equals(other.parsInVars) + " " + this.sdt.isEquivalent(other.sdt, renaming, solver));

       // System.out.println("EQ: " + this.prefix + " . " + other.prefix+ " : " + check);
        return check;
    }

    /**
     *
     * @param r
     * @return
     */
    boolean couldBeEquivalentTo(Cell other) {
        return this.parsInVars.typedSize().equals(other.parsInVars.typedSize());
        //TODO: call preliminary checks on SDTs
    }

    /**
     * computes a cell for a prefix and a symbolic suffix.
     *
     * @param oracle
     * @param prefix
     * @param suffix
     * @return
     */

    static Cell computeCell(TreeOracle oracle,
            Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix) {
        
        // System.out.println("START: computecell for " + prefix.toString() + "   .    " + suffix.toString());

    	try {

    	     //   if (tqr.getPiv().size() >2) {
    	        // System.out.println("START: computecell for " + prefix.toString() + "   .    " + suffix.toString());

        TreeQueryResult tqr = oracle.treeQuery(prefix, suffix);
        Cell c = new Cell(prefix, suffix, tqr.getSdt(), tqr.getPiv());
        
        //  System.out.println("END: computecell " + c.toString());
        // log.log(Level.FINE, "computeCell ...... {0}", c);
        // System.out.println(c);
        // assert tqr.getPiv().size() <= 2;
        
        return c;
    	} catch(DecoratedRuntimeException exc) {
    		throw exc.addDecoration("prefix", prefix).addDecoration("suffix", suffix);
    	}
    }

    GeneralizedSymbolicSuffix getSuffix() {
        return this.suffix;
    }

    Word<PSymbolInstance> getPrefix() {
        return this.prefix;
    }

    SymbolicDecisionTree getSDT() {
        return this.sdt;
    }

    @Override
    public String toString() {
        return "Cell: " + this.prefix + " / " + this.suffix + " : " + this.parsInVars
                + "\n" + this.sdt.toString();
    }

    void toString(StringBuilder sb) {
        sb.append("**** Cell: ").append(this.suffix).append(" : ").
                append(this.parsInVars).append("\n").
                append(this.sdt.toString()).append("\n");
    }

    Cell relabel(VarMapping relabelling) {
        return new Cell(prefix, suffix,
                sdt.relabel(relabelling),
                parsInVars.relabel(relabelling));
    }

    boolean isAccepting() {
        return this.sdt.isAccepting();
    }

    boolean anyAccepting() {
        return this.sdt.anyAccepting();
    }

}
