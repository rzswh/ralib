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
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 * A row in an observation table.
 *
 * @author falk
 */
class Row {

    private final Word<PSymbolInstance> prefix;

    private final Map<SymbolicSuffix, Cell> cells;

    private final PIV memorable = new PIV();

    private final RegisterGenerator regGen = new RegisterGenerator();

    private static final LearnLogger log = LearnLogger.getLogger(Row.class);

    private final boolean ioMode;

    private Row(Word<PSymbolInstance> prefix, boolean ioMode) {
        this.prefix = prefix;
        this.cells = new LinkedHashMap<>();
        this.ioMode = ioMode;
    }

    private Row(Word<PSymbolInstance> prefix, List<Cell> cells, boolean ioMode) {
        this(prefix, ioMode);

        for (Cell c : cells) {
            this.cells.put(c.getSuffix(), c);
        }
    }

    void addSuffix(SymbolicSuffix suffix, TreeOracle oracle) {
        if (ioMode && suffix.getActions().length() > 0) {
            // error row
            if (getPrefix().length() > 0 && !isAccepting()) {
                log.log(Level.INFO, "Not adding suffix " + suffix + " to error row " + getPrefix());
                return;
            }
            // unmatching suffix                 
            if ((getPrefix().length() < 1 && (suffix.getActions().firstSymbol() instanceof OutputSymbol))
                    || (prefix.length() > 0 && !(prefix.lastSymbol().getBaseSymbol() instanceof InputSymbol
                    ^ suffix.getActions().firstSymbol() instanceof InputSymbol))) {
                log.log(Level.INFO, "Not adding suffix " + suffix + " to unmatching row " + getPrefix());
                return;
            }
        }

        Cell c = Cell.computeCell(oracle, prefix, suffix);
        addCell(c);
    }

    private void addCell(Cell c) {

        assert c.getPrefix().equals(this.prefix);
        if (this.cells.containsKey(c.getSuffix())) {
        	System.out.println("Adding cell: " + c);
        	System.out.println("But already have cell: " + this.cells.get(c.getSuffix()) + " for suffix  " + c.getSuffix());
//        	return;
        }
        assert !this.cells.containsKey(c.getSuffix());

        // make sure that pars-in-vars is consistant with 
        // existing cells in his row
        PIV cpv = c.getParsInVars();
        VarMapping relabelling = new VarMapping();
        for (Entry<Parameter, Register> e : cpv.entrySet()) {
            Register r = this.memorable.get(e.getKey());
            if (r == null) {
                r = regGen.next(e.getKey().getType());
                memorable.put(e.getKey(), r);
            }
            relabelling.put(e.getValue(), r);
        }

        this.cells.put(c.getSuffix(), c.relabel(relabelling));
    }

    SymbolicSuffix getSuffixForMemorable(Parameter p) {
        for (Entry<SymbolicSuffix, Cell> c : cells.entrySet()) {
            if (c.getValue().getParsInVars().containsKey(p)) {
                return c.getKey();
            }
        }

        throw new IllegalStateException("This line is not supposed to be reached.");
    }

    SymbolicDecisionTree[] getSDTsForInitialSymbol(ParameterizedSymbol ps) {
        List<SymbolicDecisionTree> sdts = new ArrayList<>();
        for (Entry<SymbolicSuffix, Cell> c : cells.entrySet()) {
            Word<ParameterizedSymbol> acts = c.getKey().getActions();
            if (acts.length() > 0 && acts.firstSymbol().equals(ps)) {
//                System.out.println("Using " + c.getKey() + " for branching of " + ps + " after " + prefix);
                sdts.add(c.getValue().getSDT());
            }
        }
        return sdts.toArray(new SymbolicDecisionTree[]{});
    }

    PIV getParsInVars() {
        return this.memorable;
    }

    Word<PSymbolInstance> getPrefix() {
        return this.prefix;
    }

    /**
     * checks rows for equality (disregarding of the prefix!). It is assumed
     * that both rows have the same set of suffixes.
     *
     * @param other
     * @return true if rows are equal
     */
    boolean isEquivalentTo(Row other, VarMapping renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }

        if (!this.memorable.relabel(renaming).equals(other.memorable)) {
            return false;
        }

        for (SymbolicSuffix s : this.cells.keySet()) {
            Cell c1 = this.cells.get(s);
            Cell c2 = other.cells.get(s);

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!c1.isEquivalentTo(c2, renaming)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param r
     * @return
     */
    boolean couldBeEquivalentTo(Row other) {
        if (!this.memorable.typedSize().equals(other.memorable.typedSize())) {
            return false;
        }

        for (SymbolicSuffix s : this.cells.keySet()) {
            Cell c1 = this.cells.get(s);
            Cell c2 = other.cells.get(s);

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!c1.couldBeEquivalentTo(c2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * computes a new row object from a prefix and a set of symbolic suffixes.
     *
     * @param oracle
     * @param prefix
     * @param suffixes
     * @return
     */
    static Row computeRow(TreeOracle oracle,
            Word<PSymbolInstance> prefix, List<SymbolicSuffix> suffixes, boolean ioMode) {

        Row r = new Row(prefix, ioMode);
        for (SymbolicSuffix s : suffixes) {
            if (ioMode && s.getActions().length() > 0) {
                // error row
                if (r.getPrefix().length() > 0 && !r.isAccepting()) {
                    log.log(Level.INFO, "Not adding suffix " + s + " to error row " + r.getPrefix());
                    continue;
                }
                // unmatching suffix                 
                if ((r.getPrefix().length() < 1 && (s.getActions().firstSymbol() instanceof OutputSymbol))
                        || (prefix.length() > 0 && !(prefix.lastSymbol().getBaseSymbol() instanceof InputSymbol
                        ^ s.getActions().firstSymbol() instanceof InputSymbol))) {
                    log.log(Level.INFO, "Not adding suffix " + s + " to unmatching row " + r.getPrefix());
                    continue;
                }
            }
            r.addCell(Cell.computeCell(oracle, prefix, s));
        }
        return r;
    }

    boolean isAccepting() {
        Cell c = this.cells.get(RaStar.EMPTY_SUFFIX);
        return c.isAccepting();
    }

    @Override
    public String toString() {
        return this.prefix.toString();
    }

    void toString(StringBuilder sb) {
        sb.append("****** ROW: ").append(prefix).append("\n");
        for (Entry<SymbolicSuffix, Cell> c : this.cells.entrySet()) {
            c.getValue().toString(sb);
        }
    }

}
