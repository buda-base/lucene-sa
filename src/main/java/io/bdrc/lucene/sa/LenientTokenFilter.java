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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * 
 * TokenFilter to convert clean IAST content to its lenient version
 * <br> The tokens are expected to come from correctly spelled text.
 * <p>
 * For ex., when creating an index containing lenient tokens, 
 * one would tokenize the input with SkrtSyllableTokenizer or SkrtWordTokenizer
 * and then apply this TokenFilter to lenify the token.
 * 
 * @author drupchen
 *
 */
public class LenientTokenFilter extends TokenFilter{
    
    public LenientTokenFilter(TokenStream tokenStream) {
        super(tokenStream);
    }
    
    protected CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);    
    
    private final static Map<String,String> lenientMap = new HashMap<>();
    private final static List<String> lenientMapKeys = new ArrayList<>();
    
    static {
        fillMap();
    }
    
    private final static void addReplacement(final String orig, final String repl) {
        lenientMap.put(orig, repl);
        lenientMapKeys.add(orig);
    }
    
    private final static void fillMap() {
        
        /* custom transformations (must mirror those found in LenientCharFilter) */ 
        addReplacement("sh", "s");
        //addReplacement("ri", "r");
        //addReplacement("rī", "r");
        //addReplacement("li", "l");
        //addReplacement("lī", "l");
        addReplacement("v", "b");
        
        /* IAST lenient conversions */
        addReplacement("ā", "a");
        addReplacement("ī", "i");
        addReplacement("ū", "u");
        
        addReplacement("kh", "k");   
        addReplacement("gh", "g");
        
        addReplacement("ch", "c");
        addReplacement("jh", "j");
        
        addReplacement("(?:th|ṭh|ṭ)", "t");
        
        addReplacement("(?:dh|ḍh|ḍ)", "d");
        
        addReplacement("(?:ṇ|ṅ|ñ)", "n");

        addReplacement("ph", "p");
        addReplacement("bh", "b");   

        addReplacement("(?:ś|ṣ)", "s");

        addReplacement("r", "ri");
        addReplacement("ṝ", "ri");  
        addReplacement("ṛ", "ri");

        addReplacement("l", "li");
        addReplacement("ḹ", "li");  
        addReplacement("ḷ", "li");
        addReplacement("ḻ", "li");
        addReplacement("ḻh", "l");
        
        addReplacement("ii+", "i");
        
        addReplacement("cc", "c");
        addReplacement("tt", "t");
        addReplacement("dd", "d");
        addReplacement("gg", "g");
        addReplacement("kk", "k");
        addReplacement("jj", "j");
        addReplacement("pp", "p");
        addReplacement("bb", "b");

        addReplacement("ḥ", "");  
        
        // normalization for anusvara
        addReplacement("(?:ṃ|ṁ|m)t", "nt");
        addReplacement("(?:ṃ|ṁ|m)d", "nd");
        addReplacement("(?:ṃ|ṁ|m)l", "nl");
        addReplacement("(?:ṃ|ṁ|m)s", "ns");
        addReplacement("(?:ṃ|ṁ|m)r", "nr");
        addReplacement("(?:ṃ|ṁ|m)c", "nc");
        addReplacement("(?:ṃ|ṁ|m)j", "nj");
        addReplacement("(?:ṃ|ṁ|m)y", "ny");
        addReplacement("(?:ṃ|ṁ|m)k", "nk");
        addReplacement("(?:ṃ|ṁ|m)g", "ng");
        // else m
        addReplacement("(?:ṃ|ṁ)", "m");
        
        addReplacement("\u0303", "m"); // ̃  from ~ in SLP
    }
    
    public final static String renderLenient(String token) {
        for (final String toReplace : lenientMapKeys) {
            //System.out.println("in "+token+", replace "+toReplace+" with "+lenientMap.get(toReplace));
            token = token.replaceAll(toReplace, lenientMap.get(toReplace));
            //System.out.println("results in "+token);
        }   
        return token;
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
        while(this.input.incrementToken()) {
            String currentToken = this.input.getAttribute(CharTermAttribute.class).toString();
            currentToken = renderLenient(currentToken);
            termAtt.setEmpty().append(currentToken);
            termAtt.setLength(currentToken.length());
            return true;
        }
        return false;
    }
}