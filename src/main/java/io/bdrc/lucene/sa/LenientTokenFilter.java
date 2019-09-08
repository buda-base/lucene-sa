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
    
    private final static Map<String,String> lenientMap = getMap();
    
    private final static Map<String,String> getMap() {
        Map<String, String> res = new HashMap<>();//TreeMap<String, String>(new CommonHelpers.LengthComp());
        
        /* custom transformations (must mirror those found in LenientCharFilter) */ 
        res.put("sh", "s");
        res.put("ri", "r");
        res.put("rī", "r");
        res.put("li", "l");
        res.put("lī", "l");
        res.put("v", "b");
        
        /* IAST lenient conversions */
        res.put("ā", "a");
        res.put("ī", "i");
        res.put("ū", "u");
        
        res.put("kh", "k");   
        res.put("gh", "g");
        
        res.put("ch", "c");
        res.put("jh", "j");
        
        res.put("(th|ṭh|ṭ)", "t");
        
        res.put("(dh|ḍh|ḍ)", "d");
        
        res.put("(ṇ|ṅ|ñ)", "n");

        res.put("ph", "p");
        res.put("bh", "b");   

        res.put("(?:ś|ṣ)", "s");

        res.put("ṝ", "r");  
        res.put("ṛ", "r");

        res.put("ḹ", "l");  
        res.put("ḷ", "l");
        res.put("ḻ", "l");
        res.put("ḻh", "l");

        res.put("ḥ", "");  
        
        // normalization for anusvara
        res.put("(ṃ|ṁ|m)t", "nt");
        res.put("(ṃ|ṁ|m)d", "nd");
        res.put("(ṃ|ṁ|m)l", "nl");
        res.put("(ṃ|ṁ|m)s", "ns");
        res.put("(ṃ|ṁ|m)r", "nr");
        res.put("(ṃ|ṁ|m)c", "nc");
        res.put("(ṃ|ṁ|m)j", "nj");
        res.put("(ṃ|ṁ|m)y", "ny");
        res.put("(ṃ|ṁ|m)k", "nk");
        res.put("(ṃ|ṁ|m)g", "ng");
        // else m
        res.put("(ṃ|ṁ)", "m");
        
        res.put("\u0303", "m"); // ̃  from ~ in SLP
        
        return res;
    }
    
    public final static String renderLenient(String token) {
        for (String toReplace : lenientMap.keySet()) {
            token = token.replaceAll(toReplace, lenientMap.get(toReplace));
        }   
        return token;
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
        while(this.input.incrementToken()) {            
            String currentToken = this.input.getAttribute(CharTermAttribute.class).toString();
            currentToken = renderLenient(currentToken);
            
            termAtt.setEmpty().append(currentToken);
            return true;
        }
        return false;
    }
}