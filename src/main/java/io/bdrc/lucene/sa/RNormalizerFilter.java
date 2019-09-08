package io.bdrc.lucene.sa;

import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

public class RNormalizerFilter extends PatternReplaceCharFilter {
    
    public RNormalizerFilter(Reader in) {
        super(rCatcherLenient, repl, in);
    }
    
    public static final Pattern rCatcherLenient = Pattern.compile("([^aiuoe]|^)r([svbmtndlcjykg]|$)");
    public static final String repl = "$1ri$2";
    
    // convenience function to be used elsewhere
    public final static String normalizeR(final String in) {
        final Matcher matcher = rCatcherLenient.matcher(in);
        return matcher.replaceAll(repl);
    }
}
