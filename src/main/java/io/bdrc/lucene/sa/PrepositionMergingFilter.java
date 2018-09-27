package io.bdrc.lucene.sa;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import io.bdrc.lucene.sa.PartOfSpeechAttribute.PartOfSpeech;

public class PrepositionMergingFilter extends TokenFilter{

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);
    
    protected PrepositionMergingFilter(TokenStream input) {
        super(input);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        while(input.incrementToken()) {
            if (posAtt.getPartOfSpeech() == PartOfSpeech.Preposition) {
                final int bOffset = offsetAtt.startOffset();
                final StringBuilder sb = new StringBuilder();
                final int previousLen = termAtt.length();
                sb.append(termAtt);
                final boolean nextToken = input.incrementToken();
                // TODO: check other things like starting offsets and/or position increments 
                // In the long term, apply the Preposition not only to the first token but to all 
                // of those that share a position increment slot.
                if (!nextToken)
                    return true;
                final int totalLen = termAtt.length()+previousLen;
                offsetAtt.setOffset(bOffset, offsetAtt.endOffset());
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
