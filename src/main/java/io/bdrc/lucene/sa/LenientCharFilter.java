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

    public final static NormalizeCharMap map = getSkrtNormalizeCharMap();
    
    public LenientCharFilter(Reader in) {
        super(map, in);
    }

    private final static NormalizeCharMap getSkrtNormalizeCharMap() {
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        /* custom transformations (must mirror those found in LenientTokenFilter)*/
        builder.add("sh", "s");
        //builder.add("ri", "ri");
        //builder.add("li", "li");
        //builder.add("rI", "ri");
        //builder.add("lI", "li");
        builder.add("v", "b");
        
        /* lenient SLP transformations */
        // neutralize vowel length
        builder.add("A", "a"); // ā
        builder.add("I", "i"); // ī
        builder.add("U", "u"); // ū
        
        // ri and li vowels become r and l
        builder.add("f", "ri"); // ṛ
        builder.add("F", "ri"); // ṝ 
        builder.add("x", "li"); // ḷ
        builder.add("X", "li"); // Ḹ 
        builder.add("L", "li"); // ḻ
        builder.add("|", "l"); // ḻh
        
        // here's an interesting trick: we want to normalize
        // M and m to n when it could be an anusvara, this means
        // that some more false positives will appear, but
        // it's necessary in order to allow users to search
        // m for anusvara
        // see AnusvaraNormalizer
        // before dentals
        builder.add("Mt", "nt");
        builder.add("MT", "nT");
        builder.add("Md", "nd");
        builder.add("MD", "nD");
        builder.add("Ml", "nl");
        builder.add("Ms", "ns");
        // before retroflex
        builder.add("Mw", "nt");
        builder.add("MW", "nt");
        builder.add("Mq", "nd");
        builder.add("MQ", "nd");
        builder.add("Mr", "nr");
        builder.add("Mz", "ns");
        builder.add("Mx", "nl");
        // before palatals
        builder.add("Mc", "nc");
        builder.add("MC", "nc");
        builder.add("Mj", "nj");
        builder.add("MJ", "nj");
        builder.add("My", "ny");
        builder.add("MS", "ns");
        // before velars
        builder.add("Mk", "nk");
        builder.add("MK", "nl");
        builder.add("Mg", "ng");
        builder.add("MG", "ng");
        // else m
        builder.add("M", "m");
        
        // then doing the same for m
        builder.add("mt", "nt");
        builder.add("mT", "nt");
        builder.add("md", "nd");
        builder.add("mD", "nd");
        builder.add("ml", "nl");
        builder.add("ms", "ns");
        // before retroflex
        builder.add("mw", "nt");
        builder.add("mW", "nt");
        builder.add("mq", "nd");
        builder.add("mQ", "nd");
        builder.add("mr", "nr");
        builder.add("mz", "ns");
        builder.add("mx", "nl");
        // before palatals
        builder.add("mc", "nc");
        builder.add("mC", "nc");
        builder.add("mj", "nj");
        builder.add("mJ", "nj");
        builder.add("my", "ny");
        builder.add("mS", "ns");
        // before velars
        builder.add("mk", "nk");
        builder.add("mK", "nl");
        builder.add("mg", "ng");
        builder.add("mG", "ng");
        
        // nasals and visarga
        builder.add("N", "n"); // ṅ
        builder.add("Y", "n"); // ñ
        builder.add("~", "m");  // ̃  or m̐, this could be expanded like the anusvara... but I'm not sure...
        builder.add("H", ""); // ḥ, ignoring visargas
        
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
        builder.add("Kh", "k");
        builder.add("Gh", "g");
        builder.add("Ch", "c");
        builder.add("Jh", "j");
        builder.add("Th", "t"); 
        builder.add("Dh", "d");
        builder.add("Qh", "d");
        builder.add("Ph", "p");
        builder.add("Bh", "b");

        // geminates
        builder.add("kK", "k"); // kkh
        builder.add("gG", "g"); // ggh
        builder.add("cC", "c"); // cch
        builder.add("jJ", "j"); // jjh
        builder.add("tT", "t"); // tth 
        builder.add("dD", "d"); // ddh
        builder.add("qQ", "d"); // ḍḍh
        builder.add("pP", "p"); // pph
        builder.add("bB", "b"); // bbh
        builder.add("kk", "k");
        builder.add("gg", "g");
        builder.add("cc", "c");
        builder.add("jj", "j");
        builder.add("tt", "t"); 
        builder.add("dd", "d");
        builder.add("qq", "d");
        builder.add("pp", "p");
        builder.add("bb", "b");
        builder.add("hh", "h");
        builder.add("vv", "b");
        
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
