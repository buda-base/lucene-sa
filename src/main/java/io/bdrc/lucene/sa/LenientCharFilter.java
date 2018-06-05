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

import java.io.Reader;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * SLP1 CharFilter that turns SLP content into lenient IAST 
 * <p>
 * 
 *  
 * @author Hélios Hildt
 *
 */

public class LenientCharFilter extends MappingCharFilter {

    public LenientCharFilter(Reader in) {
        super(getSkrtNormalizeCharMap(), in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        /* custom transformations (must mirror those found in LenientTokenFilter)*/
        builder.add("sh", "s");
        builder.add("ri", "r");
        builder.add("li", "l");
        builder.add("v", "b");
        
        /* lenient SLP transformations */
        // neutralize vowel length
        builder.add("A", "a"); // ā
        builder.add("I", "i"); // ī
        builder.add("U", "u"); // ū
        
        // ri and li vowels become r and l
        builder.add("f", "r"); // ṛ
        builder.add("F", "r"); // ṝ 
        builder.add("x", "l"); // ḷ
        builder.add("X", "l"); // Ḹ 
        
        // nasals and visarga
        builder.add("M", "m"); // ṃ 
        builder.add("N", "n"); // ṅ
        builder.add("Y", "n"); // ñ
        builder.add("~", "");  // ̃  (simply deleted)
        builder.add("H", "h"); // ḥ
        
        // aspirated consonants
        builder.add("K", "k"); // kh
        builder.add("G", "g"); // gh
        builder.add("C", "c"); // ch
        builder.add("J", "j"); // jh
        builder.add("T", "t"); // th 
        builder.add("D", "d"); // dh
        builder.add("Q", "d"); // ḍh
        builder.add("P", "p"); // ph
        builder.add("B", "b"); // bh
        
        // retroflexes
        builder.add("w", "t"); // ṭ
        builder.add("W", "t"); // ṭh
        builder.add("q", "d"); // ḍ  
        builder.add("R", "n"); // ṇ 
        
        builder.add("z", "s"); // ṣ
        builder.add("S", "s"); // ś 
        
        /* SLP to IAST */
        builder.add("O", "au");
        builder.add("E", "ai");
        builder.add(".", "|");
        builder.add("..", "‖");
        return builder.build();
    }
}
