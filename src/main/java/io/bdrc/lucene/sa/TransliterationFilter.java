package io.bdrc.lucene.sa;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

// Devanagari -> slp1 charfilter, to be generalized

public class TransliterationFilter extends MappingCharFilter {

    public TransliterationFilter(Reader in) {
        super(getTibNormalizeCharMap(), in);
    }

    public final static NormalizeCharMap getTibNormalizeCharMap() {
        final char VIRAMA_CHAR = '\u094d';
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        final Map<String, String> consonnants = new HashMap<>();
        final Map<String, String> vowels = new HashMap<>();

        consonnants.put("\u0915", "k");
        consonnants.put("\u0916", "K");
        consonnants.put("\u0917", "g");
        consonnants.put("\u0918", "G");
        consonnants.put("\u0919", "N");
        consonnants.put("\u091A", "c");
        consonnants.put("\u091B", "C");
        consonnants.put("\u091C", "j");
        consonnants.put("\u091D", "J");
        consonnants.put("\u091E", "Y");
        consonnants.put("\u091F", "w");
        consonnants.put("\u0920", "W");
        consonnants.put("\u0921", "q");
        consonnants.put("\u0922", "Q");
        consonnants.put("\u0923", "R");
        consonnants.put("\u0924", "t");
        consonnants.put("\u0925", "T");
        consonnants.put("\u0926", "d");
        consonnants.put("\u0927", "D");
        consonnants.put("\u0928", "n");
        consonnants.put("\u092A", "p");
        consonnants.put("\u092B", "P");
        consonnants.put("\u092C", "b");
        consonnants.put("\u092D", "B");
        consonnants.put("\u092E", "m");
        consonnants.put("\u092F", "y");
        consonnants.put("\u0930", "r");
        consonnants.put("\u0932", "l");
        consonnants.put("\u0933", "L");
        consonnants.put("\u0935", "v");
        consonnants.put("\u0936", "S");
        consonnants.put("\u0937", "z");
        consonnants.put("\u0938", "s");
        consonnants.put("\u0939", "h");
        consonnants.put("\u0933\u094d\u0939", "|");

        vowels.put("\u093E", "A");
        vowels.put("\u093F", "i");
        vowels.put("\u0940", "I");
        vowels.put("\u0941", "u");
        vowels.put("\u0942", "U");
        vowels.put("\u0943", "f");
        vowels.put("\u0944", "F");
        vowels.put("\u0962", "x");
        vowels.put("\u0963", "X");
        vowels.put("\u0947", "e");
        vowels.put("\u0948", "E");
        vowels.put("\u094B", "o");
        vowels.put("\u094C", "O");

        builder.add("\u0950", "oM");
        builder.add("\u0902", "M");
        builder.add("\u0903", "H");
        builder.add("\u0951", "^");
        builder.add("\u0901", "~");
        builder.add("\u0952", "\\");
        builder.add("\u093D", "\"");
        builder.add("\u0964", ".");
        builder.add("\u0965", "..");
        builder.add("\u0905", "a");
        builder.add("\u0906", "A");
        builder.add("\u0907", "i");
        builder.add("\u0908", "I");
        builder.add("\u0909", "u");
        builder.add("\u090A", "U");
        builder.add("\u090B", "f");
        builder.add("\u0960", "F");
        builder.add("\u090C", "x");
        builder.add("\u0961", "X");
        builder.add("\u090F", "e");
        builder.add("\u0910", "E");
        builder.add("\u0913", "o");
        builder.add("\u0914", "O");
        builder.add("\u0966", "0");
        builder.add("\u0967", "1");
        builder.add("\u0968", "2");
        builder.add("\u0969", "3");
        builder.add("\u096A", "4");
        builder.add("\u096B", "5");
        builder.add("\u096C", "6");
        builder.add("\u096D", "7");
        builder.add("\u096E", "8");
        builder.add("\u096F", "9");

        for (Map.Entry<String, String> entry : consonnants.entrySet()) {
            // CONSONNANT = ca
            builder.add(entry.getKey(), entry.getValue()+"a");
            // CONSONNANT + VIRAMA = c
            builder.add(entry.getKey()+VIRAMA_CHAR, entry.getValue());
            for (Map.Entry<String, String> ventry : vowels.entrySet()) {
                // CONSONNANT + vowel = cv
                builder.add(entry.getKey()+ventry.getKey(), entry.getValue()+ventry.getValue());
            }
        }
        return builder.build();
    }

}
