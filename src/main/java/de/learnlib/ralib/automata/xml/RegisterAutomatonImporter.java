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
package de.learnlib.ralib.automata.xml;

import de.learnlib.logging.LearnLogger;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SumCDataExpression;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.SymbolicDataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SumConstant;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ConstantGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SumConstantGenerator;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXB;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.SimpleAlphabet;

/**
 *
 * @author falk
 */
public class RegisterAutomatonImporter {

	private final Map<String, ParameterizedSymbol> sigmaMap = new LinkedHashMap<>();
	private final Map<ParameterizedSymbol, String[]> paramNames = new LinkedHashMap<>();
	private final Map<String, RALocation> stateMap = new LinkedHashMap<>();
	private final Map<String, Constant> constMap = new LinkedHashMap<>();
	private final Map<String, SumConstant> sumConstMap = new LinkedHashMap<>();
	private final Map<String, Register> regMap = new LinkedHashMap<>();
	private final Map<String, DataType> typeMap = new LinkedHashMap<>();

	private final VarValuation initialRegs = new VarValuation();
	private final Constants consts = new Constants();

	private MutableRegisterAutomaton iora;
	private Alphabet<InputSymbol> inputs;
	private Alphabet<ParameterizedSymbol> actions;

	private static final LearnLogger log = LearnLogger.getLogger(RegisterAutomatonImporter.class);

	public Collection<DataType> getDataTypes() {
		return typeMap.values();
	}

	public RegisterAutomatonImporter(InputStream is) {
		loadModel(is);
	}

	public de.learnlib.ralib.automata.RegisterAutomaton getRegisterAutomaton() {
		return this.iora;
	}

	private void loadModel(InputStream is) {

		RegisterAutomaton a = unmarschall(is);

		getAlphabet(a.getAlphabet());

		getConstants(a.getConstants());
		getSumConsts(a.getSumConstants());
		getRegisters(a.getGlobals());

		iora = new MutableRegisterAutomaton(consts, initialRegs);

		// create loc map
		for (RegisterAutomaton.Locations.Location l : a.getLocations().getLocation()) {
			if (l.getInitial() != null && l.getInitial().equals("true")) {
				stateMap.put(l.getName(), iora.addInitialState());
			} else {
				stateMap.put(l.getName(), iora.addState());
			}
		}

		// transitions
		for (RegisterAutomaton.Transitions.Transition t : a.getTransitions().getTransition()) {

			// read basic data
			ParameterizedSymbol ps = sigmaMap.get(t.getSymbol());
			RALocation from = stateMap.get(t.getFrom());
			RALocation to = stateMap.get(t.getTo());
			String[] pnames = paramNames.get(ps);
			if (t.getParams() != null) {
				pnames = t.getParams().split(",");
			}
			Map<String, Parameter> paramMap = paramMap(ps, pnames);

			// guard
			String gstring = t.getGuard();
			TransitionGuard p = new TransitionGuard();
			if (gstring != null) {
				Map<String, SymbolicDataValue> map = buildValueMap(constMap, sumConstMap, regMap,
						(ps instanceof OutputSymbol) ? new LinkedHashMap<String, Parameter>() : paramMap);
				ExpressionParser parser = new ExpressionParser(gstring, map, consts.getSumCs());
				p = new TransitionGuard(parser.getPredicate());
			}

			// assignment
			Set<Register> freshRegs = new HashSet<>();
			VarMapping<Register, SymbolicDataValue> assignments = new VarMapping<>();
			VarMapping<Parameter, SymbolicDataExpression> outputs = new VarMapping<>();
			if (t.getAssignments() != null) {
				for (RegisterAutomaton.Transitions.Transition.Assignments.Assign ass : t.getAssignments().getAssign()) {
					if (regMap.containsKey(ass.to)) {
						Register left = regMap.get(ass.to);

						SymbolicDataValue right;
						if (("__fresh__").equals(ass.value)) {
							freshRegs.add(left);
							continue;
						} else if (paramMap.containsKey(ass.value)) {
							right = paramMap.get(ass.value);
						} else if (constMap.containsKey(ass.value)) {
							right = constMap.get(ass.value);
						} else {
							right = regMap.get(ass.value);
						}
						assignments.put(left, right);
					} else {
						if (paramMap.containsKey(ass.to) && t.getParams() == null) {
							Parameter left = paramMap.get(ass.to);
							SymbolicDataExpression right = resolveExpression(ass.value, paramMap);
							outputs.put(left, right);
						}
					}
				}
			}
			Assignment assign = new Assignment(assignments);

			// output
			if (ps instanceof OutputSymbol) {
				if (t.getParams() != null) {
					Parameter[] pList = paramList(ps);
					int idx = 0;
					for (String s : pnames) {
						// Parameter param = paramMap.get(s);
						Parameter param = pList[idx++];
						SymbolicDataValue source = null;
						if (regMap.containsKey(s)) {
							Register r = regMap.get(s);
							if (freshRegs.contains(r)) {
								// add assigment to store fresh value
								assignments.put(r, param);
								continue;
							} else {
								// check if there was an assignment,
								// these seem to be meant to
								// happen before the output
								source = assignments.containsKey(r) ? assignments.get(r) : r;
							}
						} else if (constMap.containsKey(s)) {
							source = constMap.get(s);
						} else {
							throw new IllegalStateException(
									"No source for output parameter \"" + s + "\" in output " + ps + ".");
						}
						outputs.put(param, source);
					}
				}

				// all unassigned parameters have to be fresh by convention,
				// we do not allow "don't care" in outputs
				Set<Parameter> fresh = new LinkedHashSet<>(paramMap.values());
				fresh.removeAll(outputs.keySet());
				OutputMapping outMap = new OutputMapping(fresh, outputs);

				OutputTransition tOut = new OutputTransition(p, outMap, (OutputSymbol) ps, from, to, assign);
				iora.addTransition(from, ps, tOut);
				log.log(Level.FINEST, "Loading: " + tOut);
			} // input
			else {
				assert freshRegs.isEmpty();

				log.log(Level.FINEST, "Guard: " + gstring);
				InputTransition tIn = new InputTransition(p, (InputSymbol) ps, from, to, assign);
				log.log(Level.FINEST, "Loading: " + tIn);
				iora.addTransition(from, ps, tIn);
			}
		}
	}

	private SymbolicDataExpression resolveExpression(String name, Map<String, Parameter> paramMap) {
		SymbolicDataExpression right;
		if (name.contains("+")) {
			String[] terms = name.split("\\s*\\+\\s*");
			SymbolicDataExpression var = null;
			SumConstant sumConst = sumConstMap.get(terms[0]);
			if (sumConst != null) {
				var = resolveExpression(terms[1], paramMap);
			} else {
				sumConst = sumConstMap.get(terms[1]);
				var = resolveExpression(terms[0], paramMap);
			}
			if (sumConst == null || var == null) {
				throw new IllegalStateException("Invalid sum with constant " + name);
			}
			return new SumCDataExpression(var, consts.getSumCs().get(sumConst));
		}
		if (paramMap.containsKey(name)) {
			right = paramMap.get(name);
		} else if (constMap.containsKey(name)) {
			right = constMap.get(name);
		} else {
			right = regMap.get(name);
		}
		return right;
	}

	private void getAlphabet(RegisterAutomaton.Alphabet a) {
		inputs = new SimpleAlphabet<>();
		actions = new SimpleAlphabet<>();
		for (RegisterAutomaton.Alphabet.Inputs.Symbol s : a.getInputs().getSymbol()) {
			int pcount = s.getParam().size();
			String[] pNames = new String[pcount];
			DataType[] pTypes = new DataType[pcount];
			int idx = 0;
			for (RegisterAutomaton.Alphabet.Inputs.Symbol.Param p : s.getParam()) {
				pNames[idx] = p.name;
				pTypes[idx] = getOrCreateType(p.type);
				idx++;
			}
			String sName = s.getName();
			InputSymbol ps = new InputSymbol(sName, pTypes);
			inputs.add(ps);
			actions.add(ps);
			sigmaMap.put(s.getName(), ps);
			paramNames.put(ps, pNames);
			log.log(Level.FINEST, "Loading: " + ps);
		}
		for (RegisterAutomaton.Alphabet.Outputs.Symbol s : a.getOutputs().getSymbol()) {
			int pcount = s.getParam().size();
			String[] pNames = new String[pcount];
			DataType[] pTypes = new DataType[pcount];
			int idx = 0;
			for (RegisterAutomaton.Alphabet.Outputs.Symbol.Param p : s.getParam()) {
				pNames[idx] = p.name;
				pTypes[idx] = getOrCreateType(p.type);
				idx++;
			}
			String sName = s.getName();
			ParameterizedSymbol ps = new OutputSymbol(sName, pTypes);
			actions.add(ps);
			sigmaMap.put(s.getName(), ps);
			paramNames.put(ps, pNames);
			log.log(Level.FINEST, "Loading: " + ps);
		}
	}

	private void getConstants(RegisterAutomaton.Constants xml) {
		ConstantGenerator cgen = new ConstantGenerator();
		for (RegisterAutomaton.Constants.Constant def : xml.getConstant()) {
			DataType type = getOrCreateType(def.type);
			Constant c = cgen.next(type);
			constMap.put(def.value, c);
			constMap.put(def.name, c);
			log.log(Level.FINEST, def.name + " ->" + c);
			DataValue dv;
			if (type.getBase() == Integer.class)
				dv = new DataValue<Integer>(type, Integer.parseInt(def.value));
			else
				dv = new DataValue<Double>(type, Double.parseDouble(def.value));

			consts.put(c, dv);
		}
		log.log(Level.FINEST, "Loading: " + consts);
	}

	public void getSumConsts(RegisterAutomaton.SumConstants xml) {
		if (xml == null) {
			return;
		}
		SumConstants sumC = consts.getSumCs();
		SumConstantGenerator cgen = new SumConstantGenerator();
		for (RegisterAutomaton.SumConstants.SumConstant def : xml.getSumConstant()) {
			DataType type = getOrCreateType(def.type);
			SumConstant c = cgen.next(type);
			sumConstMap.put(def.value, c);
			sumConstMap.put(def.name, c);
			log.log(Level.FINEST, def.name + " ->" + c);
			DataValue dv;
			if (type.getBase() == Integer.class)
				dv = new DataValue<Integer>(type, Integer.parseInt(def.value));
			else
				dv = new DataValue<Double>(type, Double.parseDouble(def.value));

			sumC.put(c, dv);
		}
	}

	private void getRegisters(RegisterAutomaton.Globals g) {
		RegisterGenerator rgen = new RegisterGenerator();
		for (RegisterAutomaton.Globals.Variable def : g.getVariable()) {
			DataType type = getOrCreateType(def.type);
			Register r = rgen.next(type);
			regMap.put(def.name, r);
			log.log(Level.FINEST, def.name + " ->" + r);
			Object o = null;
			switch (type.getBase().getName()) {
			case "java.lang.Integer":
				o = Integer.parseInt(def.value);
				break;
			case "java.lang.Double":
				o = Double.parseDouble(def.value);
				break;
			default:
				throw new IllegalStateException("Unsupported Register Type: " + type.getBase().getName());
			}
			DataValue dv = new DataValue(type, o);
			initialRegs.put(r, dv);
		}
		log.log(Level.FINEST, "Loading: " + initialRegs);
	}

	private RegisterAutomaton unmarschall(InputStream is) {
		return JAXB.unmarshal(is, RegisterAutomaton.class);
	}

	private DataType getOrCreateType(String name) {
		DataType t = typeMap.get(name);
		if (t == null) {
			// TODO: there should be a proper way of specifying java types to be bound
			t = new DataType(name, isDoubleTempCheck(name) ? Double.class : Integer.class);
			typeMap.put(name, t);
		}
		return t;
	}

	private boolean isDoubleTempCheck(String name) {
		return name.equals("DOUBLE") || name.equals("double");
	}

	private Map<String, SymbolicDataValue> buildValueMap(Map<String, ? extends SymbolicDataValue>... maps) {
		Map<String, SymbolicDataValue> ret = new LinkedHashMap<>();
		for (Map<String, ? extends SymbolicDataValue> m : maps) {
			ret.putAll(m);
		}
		return ret;
	}

	private Map<String, Parameter> paramMap(ParameterizedSymbol ps, String... pNames) {

		if (pNames == null) {
			pNames = paramNames.get(ps);
		}

		Map<String, Parameter> ret = new LinkedHashMap<>();
		ParameterGenerator pgen = new ParameterGenerator();
		int idx = 0;
		for (String name : pNames) {
			ret.put(name, pgen.next(ps.getPtypes()[idx]));
			idx++;
		}
		return ret;
	}

	private Parameter[] paramList(ParameterizedSymbol ps) {

		Parameter[] ret = new Parameter[ps.getArity()];
		ParameterGenerator pgen = new ParameterGenerator();
		int idx = 0;
		for (DataType t : ps.getPtypes()) {
			ret[idx] = pgen.next(t);
			idx++;
		}
		return ret;
	}

	/**
	 * @return the inputs
	 */
	public Alphabet<InputSymbol> getInputs() {
		return inputs;
	}

	/**
	 * @return the inputs
	 */
	public Alphabet<ParameterizedSymbol> getActions() {
		return actions;
	}

	/**
	 * @return the consts
	 */
	public Constants getConstants() {
		return consts;
	}

}
