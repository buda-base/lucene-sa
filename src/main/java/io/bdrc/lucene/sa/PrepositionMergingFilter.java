package io.bdrc.lucene.sa;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.lucene.sa.PartOfSpeechAttribute.PartOfSpeech;

public class PrepositionMergingFilter extends TokenFilter{
    static final Logger logger = LoggerFactory.getLogger(PrepositionMergingFilter.class);

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);
    
    public PrepositionMergingFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while(input.incrementToken()) {
            if (posAtt.getPartOfSpeech() == PartOfSpeech.Preposition) {
                int bOffset = offsetAtt.startOffset();
                final StringBuilder sb = new StringBuilder();
                final int previousLen = termAtt.length();
                sb.append(termAtt);
                final boolean nextToken = input.incrementToken();
                // TODO: check other things like starting offsets and/or position increments 
                // In the long term, apply the Preposition not only to the first token but to all 
                // of those that share a position increment slot.
                
                // Do not merge if the preposition Token comes from a previous input string 
                // and not one of the possible token of the current input string (should work in most cases)  
                if (!nextToken || bOffset >= offsetAtt.startOffset())
                    return nextToken;
                final int totalLen = termAtt.length()+previousLen;
                if (offsetAtt.startOffset() < bOffset) {
                    logger.warn("beginning offset incorrect. start of preposition token: ", bOffset, "start of next token: ", offsetAtt.startOffset(), 
                            "preposition token: ", sb.toString(), "next token: ", termAtt.toString());
                    bOffset = offsetAtt.startOffset();  // catches the potential cases not taken care of above
                }
                try {
                    offsetAtt.setOffset(bOffset, offsetAtt.endOffset());
                } catch (Exception ex) {
                    logger.error("PrepositionMergingFilter.incrementToken error on term: " + termAtt.toString() + "; message: " + ex.getMessage());
                }
                sb.append(termAtt);
                termAtt.setEmpty();
                termAtt.resizeBuffer(totalLen);
                termAtt.append(sb.toString());
                termAtt.setLength(totalLen);
            }
            return true;
        }
        return false;
    }    
}
