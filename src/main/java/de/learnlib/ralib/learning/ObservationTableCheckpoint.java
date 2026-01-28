package de.learnlib.ralib.learning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class ObservationTableCheckpoint implements Serializable {

    public static class GeneralizedSymbolicSuffixData implements Serializable {
        private static final long serialVersionUID = 1L;
        public final ParameterizedSymbol symbols[];
        public final SymbolicDataValue values[];
        public final DataRelation suffixRelations[][][];
        public final DataRelation prefixRelations[][];

        public GeneralizedSymbolicSuffixData(ParameterizedSymbol symbols[], SymbolicDataValue values[],
                DataRelation suffixRelations[][][], DataRelation prefixRelations[][]) {
            this.symbols = symbols;
            this.values = values;
            this.suffixRelations = suffixRelations;
            this.prefixRelations = prefixRelations;
        }
    }

    private static final long serialVersionUID = 1L;
    public final ParameterizedSymbol symbols[];
    public final List<List<PSymbolInstance>> components;
    public final GeneralizedSymbolicSuffixData suffixData[];

    ObservationTableCheckpoint(ParameterizedSymbol symbols[], List<List<PSymbolInstance>> components,
                    GeneralizedSymbolicSuffixData suffixData[]) {
        this.symbols = symbols;
        this.components = components;
        this.suffixData = suffixData;
    }

    static GeneralizedSymbolicSuffixData generateSuffixData(GeneralizedSymbolicSuffix suffix) {
        List<SymbolicDataValue> sdv = new ArrayList<>();
        List<DataRelation[][]> sufRelations = new ArrayList<>();
        List<DataRelation[]> preRelations = new ArrayList<>();
        
        for (int i = 1; i <= suffix.getActions().size(); i++) {
            sdv.add(suffix.getDataValue(i));
            DataRelation[] preRel = suffix.getPrefixRelations(i).stream().collect(Collectors.toList())
                    .toArray(DataRelation[]::new);
            preRelations.add(preRel);
            List<DataRelation[]> sufRel = new ArrayList<>();
            for (int j = 1; j < i; j++) {
                sufRel.add(suffix.getSuffixRelations(j, i).stream().collect(Collectors.toList())
                        .toArray(DataRelation[]::new));
            }
            sufRelations.add(sufRel.toArray(DataRelation[][]::new));
        }
        return new GeneralizedSymbolicSuffixData(
                suffix.getActions().asList().toArray(ParameterizedSymbol[]::new),
                sdv.toArray(SymbolicDataValue[]::new),
                sufRelations.toArray(DataRelation[][][]::new),
                preRelations.toArray(DataRelation[][]::new));
    }

    static ObservationTableCheckpoint instantiate(ParameterizedSymbol[] inputs,
            Collection<Word<PSymbolInstance>> components, List<GeneralizedSymbolicSuffix> suffix) {
        List<GeneralizedSymbolicSuffixData> suffixData = suffix.stream().map(x -> generateSuffixData(x)).collect(Collectors.toList());
        return new ObservationTableCheckpoint(
                inputs,
                components.stream().map(x -> x.asList()).collect(Collectors.toList()),
                suffixData.toArray(GeneralizedSymbolicSuffixData[]::new));
    }
}
