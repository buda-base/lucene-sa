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
 *  Filter to normalize the spelling of geminates with the following rules (C = consonant):
 *  
 *  CCr   →  Cr 
 *  rCC   →  rC
 *  CCy   →  Cy
 * 
 * See the mappings below.
 * Ex: "artTa" is normalized to "arTa",  "Darmma" to "Darma".
 * 
 * Geminates of consonants besides a "r" or "y" was a common practice in old publications.
 * These non-standard spellings need to be normalized before being handed to SkrtWordTokenizer
 * 
 * Limitations: Being applied before tokenizing, this filter will remove all the matching geminates
 * from  the input, including words whose standard spelling contains geminates.
 * 
 * input: SLP
 * output: normalized SLP
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 *
 */
public class GeminateNormalizingFilter extends MappingCharFilter {

    public GeminateNormalizingFilter(Reader in) {
        super(getSkrtNormalizeCharMap(), in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        final Map<String, String> consonants = new HashMap<>();
        
        consonants.put("kk", "k");
        consonants.put("gg", "g");
        consonants.put("cc", "c");
        consonants.put("jj", "j");
        consonants.put("ww", "w");
        consonants.put("qq", "q");
        consonants.put("RR", "R");
        consonants.put("tt", "t");
        consonants.put("dd", "d");
        consonants.put("nn", "n");
        consonants.put("pp", "p");
        consonants.put("bb", "b");
        consonants.put("mm", "m");
        consonants.put("yy", "y");
        consonants.put("vv", "v");
        consonants.put("ll", "l");
        consonants.put("ss", "s");
        consonants.put("SS", "S");
        consonants.put("zz", "z");

        consonants.put("gG", "G");
        consonants.put("kK", "K");
        consonants.put("cC", "C");
        consonants.put("jJ", "J");
        consonants.put("wW", "W");
        consonants.put("qQ", "Q");
        consonants.put("tT", "T");
        consonants.put("dD", "D");
        consonants.put("pP", "P");
        consonants.put("bB", "B");
                
        for (Map.Entry<String, String> entry : consonants.entrySet()) {
            builder.add(entry.getKey()+"r", entry.getValue()+"r");
            builder.add("r"+entry.getKey(), "r"+entry.getValue());
            builder.add(entry.getKey()+"y", entry.getValue()+"y");
        }

        return builder.build();
    }
}
