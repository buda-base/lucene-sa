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

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LenientTests {

	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}
	
	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString());
			}
			System.out.println("1 " + String.join(" ", expected));
			System.out.println("2 " + String.join(" ", termList) + "\n");
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}

	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}
	
    @Test
    public void test1() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "kṛṣṇa āa īi ūu ōo khk ghg chc jhj thtṭhṭ dhdḍhḍ ṇn php bhbv śsṣ ṝṛri ḹḷli ḥh"; 
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	ts = new LenientSearchFilter(ts);
    	List<String> expected = Arrays.asList("krisna", "aa", "ii", "uu", "oo", "kk", "gg", "cc", "jj", "tttt", "dddd", 
    			"nn", "pp", "bbb", "sss", "ririri", "lilili", "hh");
    	assertTokenStream(ts, expected);
    }
	
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}
