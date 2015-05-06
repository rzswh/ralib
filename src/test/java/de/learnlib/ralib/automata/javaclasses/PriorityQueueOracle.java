/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.automata.javaclasses;

import de.learnlib.api.Query;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.Collection;
import java.util.PriorityQueue;

/**
 *
 * @author Stealth
 */
public final class PriorityQueueOracle implements DataWordOracle {

    //private RegisterAutomaton.Alphabet<ParameterizedSymbol> sigma = 
    //        new Alphabet<ParameterizedSymbol>();
    public static final DataType doubleType = new DataType("DOUBLE", Double.class);
//    
//    private final PSymbolInstance ok = new PSymbolInstance(
//            new ParameterizedSymbol("ok", 0), new Object[] {});
//            
//    private final PSymbolInstance err = new PSymbolInstance(
//            new ParameterizedSymbol("err", 0), new Object[] {});    

//    public static final InputSymbol ok
//            = new InputSymbol("ok", new DataType[]{});
//
//    public static final InputSymbol err
//            = new InputSymbol("err", new DataType[]{});
    public static final InputSymbol POLL = new InputSymbol("poll", new DataType[]{doubleType});
    public static final InputSymbol OFFER = new InputSymbol("offer", new DataType[]{doubleType});

//    public PriorityQueueExample() {
//        this.sigma.add(poll);
//        this.sigma.add(offer);
//    }
    @Override
    public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> clctn) {

        for (Query<PSymbolInstance, Boolean> query : clctn) {

            if (query.getInput().length() < 1) {
                query.answer(true);
            } else {
                PriorityQueue<Double> queue = new PriorityQueue<Double>();
                PSymbolInstance[] trace = new PSymbolInstance[query.getInput().length()];
                Boolean[] answer = new Boolean[query.getInput().length()];

                for (int i = 0; i < query.getInput().length(); i++) {
//                    System.out.println("index in query: " + i);
                    //query.answer(false);
                    answer[i] = false;
                    try {
                        PSymbolInstance psi = query.getInput().getSymbol(i);
                        DataValue<Double> d = psi.getParameterValues()[0];
                        if (psi.getBaseSymbol().equals(OFFER) && queue.size() < 3) {
                            //System.out.println("executing:  queue.offer(" + d.toString() + ")");
                            queue.offer(d.getId());
                            //query.answer(true);
                            answer[i] = true;
//                            System.out.println("queue is : " + queue.toString());
                        } else if (psi.getBaseSymbol().equals(POLL)) {
//                            System.out.println("polling; queue is : " + queue.toString());
                            Double val = queue.poll();
//                            System.out.println("Val: " + val.toString());
                            if (val!=null) {
//                                System.out.println("executing: queue.poll(), which returns " + val.toString());
                                if (val.equals(d.getId())) {
//                                    System.out.println("... and equals " + d.toString());
                                    //query.answer(true);
                                    answer[i] = true;
                                }
//                            else{
//                                query.answer(false);
//                            }
                            }
                        }
                    } catch (Exception e) {
                        System.out.print("Exception caught: ");
                        System.out.println(e.getMessage());
                    }
                }
                query.answer(isArrayTrue(answer));
//                System.out.println("queue : " + queue.toString());
//                System.out.println("query : " + query.toString());
            }
        }
    }
    
     private boolean isArrayTrue(Boolean[] maybeArr) {
        boolean maybe = true;
        for (int c = 0; c < (maybeArr.length); c++) {
            //log.log(Level.FINEST,maybeArr[c]);
            if (!maybeArr[c]) {
                maybe = false;
                break;
            }
        }
        return maybe;
    }

}
