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
    
    public final static String renderLenient(String token) {
        
        Map<String, String> lenientMap = new TreeMap<String, String>(new CommonHelpers.LengthComp());
        
        /* custom transformations (must mirror those found in LenientCharFilter) */ 
        lenientMap.put("sh", "s");
        lenientMap.put("ri", "r");
        lenientMap.put("rī", "r");
        lenientMap.put("li", "l");
        lenientMap.put("lī", "l");
        lenientMap.put("v", "b");
        
        /* IAST lenient conversions */
        lenientMap.put("ā", "a");   

        lenientMap.put("ī", "i");   
        
        lenientMap.put("ū", "u");   
        
        lenientMap.put("kh", "k");   
        
        lenientMap.put("gh", "g");
        
        lenientMap.put("ch", "c");
        
        lenientMap.put("jh", "j");
        
        lenientMap.put("th", "t");   
        lenientMap.put("ṭh", "t");
        lenientMap.put("ṭ", "t");
        
        lenientMap.put("dh", "d");
        lenientMap.put("ḍh", "d");
        lenientMap.put("ḍ", "d");
        
        lenientMap.put("ṇ", "n");
        lenientMap.put("ṅ", "n");
        lenientMap.put("ñ", "n");

        lenientMap.put("ph", "p");
        
        lenientMap.put("bh", "b");   

        lenientMap.put("ś", "s");   
        lenientMap.put("ṣ", "s");

        lenientMap.put("ṝ", "r");  
        lenientMap.put("ṛ", "r");

        lenientMap.put("ḹ", "l");  
        lenientMap.put("ḷ", "l");
        lenientMap.put("ḻ", "l");
        lenientMap.put("ḻh", "l");

        lenientMap.put("ḥ", "h");  
        
        lenientMap.put("ṃ", "m");
        lenientMap.put("ṁ", "m");
        
        lenientMap.put("\u0303", ""); // ̃  from ~ in SLP
        
        for (String toReplace : lenientMap.keySet()) {
            if (token.contains(toReplace)) {
                token = token.replaceAll(toReplace, lenientMap.get(toReplace));
            }
        }   
        return token;
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
        CommonHelpers.logger.info("---------------");
        while(this.input.incrementToken()) {            
            String originalToken = this.input.getAttribute(CharTermAttribute.class).toString();
            String lenientToken = renderLenient(originalToken);

            CommonHelpers.logger.info(String.format("%s  ->  %s", originalToken, lenientToken));

            termAtt.setEmpty().append(lenientToken);
            return true;
        }
        return false;
    }
}
