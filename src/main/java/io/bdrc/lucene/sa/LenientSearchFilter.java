package io.bdrc.lucene.sa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public class LenientSearchFilter extends TokenFilter{
	
	public LenientSearchFilter(TokenStream tokenStream) {
		super(tokenStream);
	}
	
    protected CharTermAttribute cta = addAttribute(CharTermAttribute.class);
    protected PositionIncrementAttribute pia = addAttribute(PositionIncrementAttribute.class);    
    
	@Override
	public final boolean incrementToken() throws IOException {
		
        while(this.input.incrementToken()) {        	
            String currentToken = this.input.getAttribute(CharTermAttribute.class).toString();
            currentToken = renderLenient(currentToken);
            
            this.cta.setEmpty().append(currentToken);
            this.pia.setPositionIncrement(1);
            return true;
        }
		return false;
	}
    
    public final static String renderLenient(String token) {
		
		Map<String, String> lenientMap = new HashMap<String, String>();
		lenientMap.put("A", "a");	// ā = a
		
		lenientMap.put("I", "i");	// ī = i
		
		lenientMap.put("U", "u");	// ū = u
		
		lenientMap.put("O", "o");	// ō = o

		lenientMap.put("K", "k");	// kh = k
		
		lenientMap.put("G", "g");	// gh = g
		
		lenientMap.put("C", "c");	// ch = c
		
		lenientMap.put("J", "j");	// jh = j
		
		lenientMap.put("T", "t");	// th = t = ṭh = ṭ
		lenientMap.put("W", "t");
		lenientMap.put("w", "t");
		
		lenientMap.put("D", "d");	// dh = d = ḍh = ḍ
		lenientMap.put("Q", "d");
		lenientMap.put("q", "d");
		
		lenientMap.put("R", "n");	// ṇ = n
		
		lenientMap.put("P", "p");	// ph = p
		
		lenientMap.put("B", "b");	// bh = b = v
		lenientMap.put("v", "b");
		
		lenientMap.put("S", "s");	// ś = s = ṣ
		lenientMap.put("z", "s");
		
		lenientMap.put("F", "ri");	// ṝ = ṛ = ri
		lenientMap.put("f", "ri");
		
		lenientMap.put("X", "li");	// ḹ = ḷ = li
		lenientMap.put("x", "li");
		
		lenientMap.put("H", "h");	// ḥ = h
		
		for (String toReplace : lenientMap.keySet()) {
			if (token.contains(toReplace)) {
				token = token.replaceAll(toReplace, lenientMap.get(toReplace));
			}
		}	
		return token;
	}
}