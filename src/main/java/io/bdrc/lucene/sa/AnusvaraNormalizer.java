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
 * Roman → SLP1 charfilter 
 * 
 * Following the naming convention used by Peter Scharf, we use "Roman" instead of "IAST" to show that, 
 * on top of supporting the full IAST character set,
 * we support the extra distinctions within devanagari found in ISO 15919
 * A list of non-Sanskrit and non-Devanagari characters (see below) are deleted. 
 * 
 * see @see <a href="https://en.wikipedia.org/wiki/ISO_15919#Comparison_with_UNRSGN_and_IAST">Comparison_with_UNRSGN_and_IAST</a> 
 * and the Overview section of the same page.
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 *
 */

public class AnusvaraNormalizer extends MappingCharFilter {

    private static final NormalizeCharMap map = getCharMap();
    
    public AnusvaraNormalizer(Reader in) {
        super(map, in);
    }

    private final static NormalizeCharMap getCharMap() {
        // The idea here is to change the anusvara (or candrabindu) for its value
        // according to context. Everything is SPL1
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        
        // before labials: m
        builder.add("Mb", "mb");
        builder.add("MB", "mB");
        builder.add("Mp", "mp");
        builder.add("MP", "mP");
        builder.add("Mv", "mv");
        
        // before dentals: n
        builder.add("Mt", "nt");
        builder.add("MT", "nT");
        builder.add("Md", "nd");
        builder.add("MD", "nD");
        builder.add("Ml", "nl");
        builder.add("Ms", "ns");

        // before retroflex: R
        builder.add("Mw", "Rw");
        builder.add("MW", "RW");
        builder.add("Mq", "Rq");
        builder.add("MQ", "RQ");
        builder.add("Mr", "Rr");
        builder.add("Mz", "Rz");
        builder.add("Mx", "Rx");
        
        // before palatals: Y
        builder.add("Mc", "Yc");
        builder.add("MC", "YC");
        builder.add("Mj", "Yj");
        builder.add("MJ", "YJ");
        builder.add("My", "Yy");
        builder.add("MS", "YS");
        
        // before velars: N
        builder.add("Mk", "Nk");
        builder.add("MK", "NK");
        builder.add("Mg", "Ng");
        builder.add("MG", "NG");
        
        // else: m
        builder.add("M", "m");
        return builder.build();
    }

}
