package io.bdrc.lucene.sa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        
        Map<String, String> lenientMap = new HashMap<String, String>();
        
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

        lenientMap.put("ph", "p");
        
        lenientMap.put("bh", "b");   
        lenientMap.put("v", "b");

        lenientMap.put("ś", "s");   
        lenientMap.put("ṣ", "s");

        lenientMap.put("ṝ", "r");  
        lenientMap.put("ṛ", "r");

        lenientMap.put("ḹ", "l");  
        lenientMap.put("ḷ", "l");

        lenientMap.put("ḥ", "h");  
        
        for (String toReplace : lenientMap.keySet()) {
            if (token.contains(toReplace)) {
                token = token.replaceAll(toReplace, lenientMap.get(toReplace));
            }
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