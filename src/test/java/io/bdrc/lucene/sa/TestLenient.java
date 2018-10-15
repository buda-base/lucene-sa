package io.bdrc.lucene.sa;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLenient {

    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}

    static private void assertTokenStream(TokenStream tokenStream1, TokenStream tokenStream2) {
        try {
            List<String> termList1 = new ArrayList<String>();
            CharTermAttribute charTermAttribute1 = tokenStream1.addAttribute(CharTermAttribute.class);
            while (tokenStream1.incrementToken()) {
                termList1.add(charTermAttribute1.toString());
            }
            List<String> termList2 = new ArrayList<String>();
            CharTermAttribute charTermAttribute2 = tokenStream2.addAttribute(CharTermAttribute.class);
            while (tokenStream2.incrementToken()) {
                termList2.add(charTermAttribute2.toString());
            }
            System.out.println("found: " + String.join(" ", termList1) + " and");
            System.out.println("found: " + String.join(" ", termList2) + "\n");
            assertThat(termList1, is(termList2));
        } catch (IOException e) {
            assertTrue(false);
        }
    }

	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString());
			}
			System.out.println("found: " + String.join(" ", termList) + "\n");
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}

	// Both tests have the same input and the same expected output.
	// This ensures an equivalent treatment at indexing and querying times.
	private static String input = "kṛṣṇa āa īi ūu ōo khk ghg chc jhj thtṭhṭ dhdḍhḍ ṇn php bhbv śsṣ ṝṛri ḹḷli ḥh śrī";
	private static final List<String> expected = Arrays.asList("krsna", "aa", "ii", "uu", "oo", "kk", "gg", "cc", "jj", "tttt", "dddd", 
            "nn", "pp", "bbb", "sss", "rrr", "lll", "hh", "sr");	
	
	@BeforeClass
	public static void init() {
		System.out.println("input:\n       " + input);
		System.out.println("expected:\n       " + String.join(" ", expected));
	}
	
    @Test
    public void testLenientTokenFilter() throws Exception {
    	System.out.println("Testing LenientTokenFilter");
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	ts = new Slp2RomanFilter(ts);
    	ts = new LenientTokenFilter(ts);
        assertTokenStream(ts, expected);
    }

    @Test
    public void testLenientAnalyzer() throws Exception {
        System.out.println("Testing Lenient Analyzer");
        String i = "kṛṣṇa mañjuśrī mañjuśrījñā";
        String li = "krishna manjushri manjushrijna";
        Analyzer indexingAnalyzer = new SanskritAnalyzer("syl", "roman", false, true, "index");
        Analyzer queryAnalyzer = new SanskritAnalyzer("syl", "roman", false, false, "query");
        TokenStream indexTk = indexingAnalyzer.tokenStream("", i);
        indexTk.reset();
        TokenStream queryTk = queryAnalyzer.tokenStream("", li);
        queryTk.reset();
        assertTokenStream(indexTk, queryTk);
        indexingAnalyzer.close();
        queryAnalyzer.close();
    }

    @Test
    public void testLenientCharFilter() throws Exception {
        System.out.println("Testing LenientCharFilter"); 
        CharFilter cs = new Roman2SlpFilter(new StringReader(input));
        cs = new LenientCharFilter(cs);
        TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        assertTokenStream(ts, expected);
    }
}
