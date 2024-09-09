/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 *
 * If this file is a derivation of another work the license header will appear
 * below; otherwise, this work is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.
 *
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.bdrc.lucene.sa;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * Devanagari → SLP1 charfilter
 * 
 * Based on the devanagari tables found in @see <a href="http://unicode.org/charts/PDF/U0900.pdf">U0900.pdf</a>
 * This filter also normalizes non-Sanskrit Devanagari characters. Ex: क़ →  क
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 *
 */
public class Deva2SlpFilter extends MappingCharFilter {
    
    public static final NormalizeCharMap map = getSkrtNormalizeCharMap();

    public Deva2SlpFilter(final Reader in) {
        super(map, in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
    	final char VIRAMA_CHAR = '\u094d';
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        final Map<String, String> consonants = new HashMap<>();
        final Map<String, String> vowels = new HashMap<>();
        
        consonants.put("\u0915", "k");
        consonants.put("\u0916", "K");
        consonants.put("\u0917", "g");
        consonants.put("\u0918", "G");
        consonants.put("\u0919", "N");
        consonants.put("\u091A", "c");
        consonants.put("\u091B", "C");
        consonants.put("\u091C", "j");
        consonants.put("\u091D", "J");
        consonants.put("\u091E", "Y");
        consonants.put("\u091F", "w");
        consonants.put("\u0920", "W");
        consonants.put("\u0921", "q");
        consonants.put("\u0922", "Q");
        consonants.put("\u0923", "R");
        consonants.put("\u0924", "t");
        consonants.put("\u0925", "T");
        consonants.put("\u0926", "d");
        consonants.put("\u0927", "D");
        consonants.put("\u0928", "n");
        consonants.put("\u092A", "p");
        consonants.put("\u092B", "P");
        consonants.put("\u092C", "b");
        consonants.put("\u092D", "B");
        consonants.put("\u092E", "m");
        consonants.put("\u092F", "y");
        consonants.put("\u0930", "r");
        consonants.put("\u0932", "l");
        consonants.put("\u0933", "L");
        consonants.put("\u0935", "v");
        consonants.put("\u0936", "S");
        consonants.put("\u0937", "z");
        consonants.put("\u0938", "s");
        consonants.put("\u0939", "h");
        consonants.put("\u0933\u094d\u0939", "|");
        
        /*
         * lossy normalization of extra consonants and available NFC correspondances
         * the SLP corresponds to the character without nukta (dot), since we assume is
         * is a mistyped Sanskrit character.
         */
        consonants.put("\u0928\u093c", "n"); // NFD ऩ 
        consonants.put("\u0931", "r"); // ऱ
        consonants.put("\u0930\u093c", "r"); // NFD ऱ 
        consonants.put("\u0934", "L"); // ऴ 
        consonants.put("\u0933\u093c", "L"); // NFD ऴ
        consonants.put("\u0958", "k"); // क़
        consonants.put("\u0915\u093c", "k"); // NFD क़
        consonants.put("\u0959", "K"); // ख़
        consonants.put("\u0916\u093c", "K"); // NFD ख़
        consonants.put("\u095a", "g"); // ग़
        consonants.put("\u0917\093c", "g"); // NFD ग़
        consonants.put("\u095b", "j"); // ज़
        consonants.put("\u091c\u093c", "j"); // NFD ज़ 
        consonants.put("\u095c", "q"); // ड़
        consonants.put("\u0921\u093c", "q"); // NFD ड़ 
        consonants.put("\u095d", "Q"); // ढ़
        consonants.put("\u0922\u093c", "Q"); // NFD ढ़ 
        consonants.put("\u095e", "P"); // फ़
        consonants.put("\u092b\u093c", "Q"); // NFD फ़
        consonants.put("095f", "y"); // य़ 
        consonants.put("\u092f\u093c", "y"); // NFD य़ 

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
        builder.add("\u093D", "'");
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
        builder.add("\u200C", ""); // ZWNJ
        builder.add("\u200D", ""); // ZWJ
        
        for (Map.Entry<String, String> entry : consonants.entrySet()) {
            // CONSONANT = ca
            builder.add(entry.getKey(), entry.getValue()+"a");
            // CONSONANT + VIRAMA = c
            builder.add(entry.getKey()+VIRAMA_CHAR, entry.getValue());
            for (Map.Entry<String, String> ventry : vowels.entrySet()) {
                // CONSONANT + vowel = cv
                builder.add(entry.getKey()+ventry.getKey(), entry.getValue()+ventry.getValue());
            }
        }

        return builder.build();
    }

}
