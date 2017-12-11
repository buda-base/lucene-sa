/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for the Sanskrit filters and SylTokenizer.
 */
public class SkrtAnalyzerTests
{
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
	public void testAfterConsonantCluster() throws Exception {
		System.out.println("Testing clusters after consonants 1. before a space, 2. at the end of input");
		String input = "vanty Sas";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("vanty", "Sas");
		TokenStream res = tokenize(reader, new SkrtSyllableTokenizer());
		assertTokenStream(res, expected);
	}
	
	@Test
	public void testSylTokenizer() throws IOException {
		System.out.println("Testing SkrtSylTokenizer() against output of Peter Scharf's script");
		String input = "pfTivyA lABe pAlane ca yAvanty arTa SAstrARi pUrva AcAryEH prasTApitAni prAyaSas tAni "
																	+"saMhftya^ekam idam arTa SAstraM kftam //";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("pf", "Ti", "vyA", "lA", "Be", "pA", "la", "ne", "ca", "yA", "vanty", 
				"a", "rTa", "SA", "strA", "Ri", "pU", "rva", "A", "cA", "ryEH", "pra", "sTA", "pi", "tA", "ni", 
				"prA", "ya", "Sas", "tA", "ni", "saM", "hf", "tya^e", "kam", "i", "dam", "a", "rTa", "SA", "straM", 
				"kf", "tam", "//");

		System.out.println("0 " + input);
		TokenStream res = tokenize(reader, new SkrtSyllableTokenizer());
		assertTokenStream(res, expected);
	}
	
    @Test
    public void testDeva2SlpFilter() throws Exception {
    	System.out.println("Testing transliterating from devanagari");
    	String input = "अथ राजकन्या चन्द्रवती नामाभिनवरुपयौवनसम्पन्ना सखीद्वितीयैकस्मिन्महोत्सवदिवसे नगरं निरिक्षमाणास्ति ।"; 
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        List<String> expected = Arrays.asList("aTa", "rAjakanyA", "candravatI", "nAmABinavarupayOvanasampannA", "saKIdvitIyEkasminmahotsavadivase", "nagaraM", "nirikzamARAsti", ".");
        assertTokenStream(ts, expected);
    }
    
    @Test
    public void testDeva2SlpFilterVedicExtensions() throws Exception {
    	System.out.println("Testing transliterating from devanagari");
    	String input = "अ\u1CE5थ रा\u1CD0जकन्\u1CDBया चन्द्रवती\u1CE0 "; 
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	cs = new VedicFilter(cs);
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        List<String> expected = Arrays.asList("aTa", "rAjakanyA", "candravatI");
        assertTokenStream(ts, expected);
    }
    
    @Test
    public void testZwjZwnjTranscoding() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "\u0915\u094d\u0937 \u0915\u094d\u200D\u0937 \u0915\u094d\u200C\u0937"; // respectively क्ष  and क्‍ष 
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("kza", "kza", "kza");
    	assertTokenStream(ts, expected);
    }
	
    @Test
    public void testRoman2SlpNfcNfd() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "\u1e5d \u1e5b\u0304 r\u0323\u0304"; // NFC, semi-NFD and NFD versions of ṝ 
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("F", "F", "F");
    	assertTokenStream(ts, expected);
    }

    @Test
    public void testRoman2SlpIso15919() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "ẏ m̆b ē k͟h"; // normalizations and deletions 
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("y", "e");
    	assertTokenStream(ts, expected);
    }
    
    @Test
    public void testParseStopwords() throws Exception {
    	System.out.println("Parse stopwords file");
    	ArrayList<String> result = SanskritAnalyzer.getWordList(new FileInputStream("src/main/resources/skrt-stopwords.txt"), "#");
    	boolean res = true;
    	for (String stop: result) {
    		if (stop.contains("#") || stop.equals("")) {
    			res = false;
    		}
    	}
    	assertTrue(res);
    }

	@Test
	public void stopwordFilterTest() throws IOException
	{
		System.out.println("Testing SanskritAnalyzer.skrtStopWords");
		String input = "one aham AvAByAm etAByAm two";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("one", "two");

		System.out.print(input + " => ");
		TokenStream syllables = tokenize(reader, new WhitespaceTokenizer());
		CharArraySet stopSet = StopFilter.makeStopSet(SanskritAnalyzer.getWordList(new FileInputStream("src/main/resources/skrt-stopwords.txt"), "#"));
		StopFilter res = new StopFilter(syllables, stopSet);
		assertTokenStream(res, expected);
	} 
	
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}