/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
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

import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * SLP1 → Roman TokenFilter 
 * <p>
 * Based on {@link Roman2SlpFilter} for the mapping. Differs in the following way:
 * <br> - does not preserve the case (information not encoded in SLP)
 * <br> - only keep one mapping per SLP character, never the decomposed Unicode form.
 * 
 * @author Hélios Hildt
 *
 */

public class Slp2RomanFilter extends TokenFilter {

    private static final HashMap<String, String> map = getMapping();

    public Slp2RomanFilter(TokenStream in) {
        super(in);
    }

    public static final HashMap<String, String> getMapping() {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("M", "\u1e41"); // ṁ TODO: conflicts with lower entry with M ???

        // danda 
        map.put(".", "|");
        map.put("..", "‖");
        
        // SLP characters
        map.put("A", "\u0101"); // ā
        map.put("I", "\u012b"); // ī
        map.put("U", "\u016b"); // ū 
        map.put("f", "\u1e5b"); // ṛ
        map.put("F", "\u1e5d"); // ṝ
        map.put("x", "\u1e37"); // ḷ
        map.put("X", "\u1e39"); // ḹ
        map.put("O", "au");
        map.put("E", "ai");
        map.put("M", "\u1e43"); // ṃ
        map.put("H", "\u1e25"); // ḥ
        map.put("K", "kh");
        map.put("G", "gh");
        map.put("N", "\u1e45"); // ṅ
        map.put("C", "ch");
        map.put("J", "jh");
        map.put("Y", "\u00f1"); // ñ
        map.put("w", "\u1e6d"); // ṭ
        map.put("W", "\u1e6dh"); // ṭh 
        map.put("q", "\u1e0d"); // ḍ
        map.put("Q", "\u1e0dh"); // ḍh
        map.put("R", "\u1e47"); // ṇ
        map.put("T", "th"); 
        map.put("D", "dh");
        map.put("P", "ph");
        map.put("B", "bh");
        map.put("S", "\u015b"); // ś
        map.put("z", "\u1e63"); // ṣ
        map.put("L", "\u1e3B"); // ḻ
        map.put("|", "\u1e3Bh"); // ḻh
        map.put("~", "\u0303"); // ̃  
        return map;
    }

    CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);

    @Override
    public final boolean incrementToken() throws IOException {
        while (input.incrementToken()) {
            StringBuilder lazied = new StringBuilder();
            char[] tokenBuffer = charTermAttribute.toString().toCharArray();
            for (char t: tokenBuffer) {
                String key = String.valueOf(t);
                if (map.containsKey(key)) {
                    lazied.append(map.get(key));
                } else {
                    lazied.append(t);
                }
            }
            charTermAttribute.setEmpty().append(lazied.toString());
            return true;
        }
        return false;
    }
}