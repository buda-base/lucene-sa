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
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * 
 * A filter implementing the specific needs of Siddham project
 * input: SLP
 * output: normalized SLP 
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 *
 */

public class SiddhamFilter extends MappingCharFilter {

    public SiddhamFilter(Reader in) {
        super(getSkrtNormalizeCharMap(), in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
        
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        
        /* 
         * ḷ transcribes two different things: 
         *      - the vocalic l, 
         *      - a character used to write the retroflex l. 
         *
         * The rule to distinguish the vocalic ḷ from the retroflex consonant ḷ: 
         *      - if the IAST character ḷ is followed by a vowel, then it represents the consonant; 
         *      - in all other cases it represents the vocalic sound.
         * 
         * The retroflex l is not a phoneme in classical Sanskrit, but it is used in Vedic 
         * (where it is an allophone of ḍ used depending on the phonetic context) 
         * and in most Dravidian languages (where it is a distinct phoneme). 
         * It is also used in some Sanskrit inscriptions from my period. 
         * For parsing purposes it is equivalent to the regular l. 
         * 
         * */
        builder.add("xa", "la");
        builder.add("xA", "lA");
        builder.add("xi", "li");
        builder.add("xI", "lI");
        builder.add("xu", "lu");
        builder.add("xU", "lU");
        builder.add("xe", "le");
        builder.add("xE", "lE");
        builder.add("xo", "lo");
        builder.add("xO", "lO");

        return builder.build();
    }

}
