package io.bdrc.lucene.sa;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
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
	private static String input = "kṛṣṇa āa īi ūu ōo kkh ggh cch jjh tth ṭṭh ddh ḍḍh ṇnñṅ oṃ ama amta aṃta anta amba aṃba pph bbh śsṣ ṝṛri ḹḷli ḥh śrī";
	private static final List<String> expected = Arrays.asList("krisna", "aa", "i", "uu", "oo", "k", "g", "c", "j", "t", "t", "d", "d", 
            "nnnn", "om", "ama", "anta", "anta", "anta", "amba", "amba" ,"p", "b", "sss", "ririri", "lilili", "h", "sri");	
	
	@BeforeClass
	public static void init() {
		System.out.println("input:\n       " + input);
		System.out.println("expected:\n       " + String.join(" ", expected));
	}

	public static void assertRnorm(String in, String expected) {
	    //System.out.println(RNormalizerFilter.normalizeR(in));
	    assertTrue(expected.equals(RNormalizerFilter.normalizeR(in)));
	}

	@Test
	public void testRnormalizer() {
	    assertRnorm("amrta", "amrita");
        assertRnorm("arta", "arta");
        assertRnorm("r", "ri");
        assertRnorm("ara", "ara");
	    assertRnorm("rtavan", "ritavan");
	    assertRnorm("ritavan", "ritavan");
	    assertRnorm("ksatriya", "ksatriya");
	    assertRnorm("ksatrya", "ksatriya");
	}
	
    @Test
    public void testLenientTokenFilter() throws Exception {
    	System.out.println("Testing LenientTokenFilter");
    	assertEquals("ri", LenientTokenFilter.renderLenient("rii"));
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	ts = new Slp2RomanFilter(ts);
    	ts = new LenientTokenFilter(ts);
        assertTokenStream(ts, expected);
    }
    
    @Test
    public void testLenientAnalyzer() throws Exception {
        System.out.println("Testing Lenient Analyzer");
        String i = "ṛtāvan kṛṣṇa mañjuśrī mañjuśrījñā Pramāṇavārttikaṭīkā vajracchedikā vajracchedikā dharmma dharmma aṛtti aṛtti";
        String li = "rtavan krishna mamjushri manjushrijna pramāṇavārttikatika vajracedika vajrachedika dharma dharmma aṛtti arti";
        Analyzer indexingAnalyzer = new SanskritAnalyzer("syl", "roman", false, false, "index");
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
