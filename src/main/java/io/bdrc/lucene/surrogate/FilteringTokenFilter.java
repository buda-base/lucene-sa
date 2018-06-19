package io.bdrc.lucene.surrogate;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;


/**
 * Abstract base class for TokenFilters that may remove tokens.
 * You have to implement {@link #accept} and return a boolean if the current
 * token should be preserved. {@link #incrementToken} uses this method
 * to decide if a token should be passed to the caller.
 */
public abstract class FilteringTokenFilter extends TokenFilter {

    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private int skippedPositions;

    /**
     * Create a new {@link FilteringTokenFilter}.
     * @param in      the {@link TokenStream} to consume
     */
    public FilteringTokenFilter(TokenStream in) {
        super(in);
    }

    /** Override this method and return if the current input token should be returned by {@link #incrementToken}. */
    protected abstract boolean accept() throws IOException;

    @Override
    public final boolean incrementToken() throws IOException {
        skippedPositions = 0;
        while (input.incrementToken()) {
            if (accept()) {
                if (skippedPositions != 0) {
                    posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
                }
                return true;
            }
            skippedPositions += posIncrAtt.getPositionIncrement();
        }

        // reached EOS -- return false
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        skippedPositions = 0;
    }

    @Override
    public void end() throws IOException {
        super.end();
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
    }
}
