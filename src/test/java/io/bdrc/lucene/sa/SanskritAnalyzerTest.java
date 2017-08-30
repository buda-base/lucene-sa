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

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.sa.SkrtWordTokenizer;

import static org.hamcrest.CoreMatchers.*;

/**
 * Unit tests for the Sanskrit tokenizers and filters.
 */
public class SanskritAnalyzerTest
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
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}

	@Test
	public void testIsTrailingCluster() throws Exception {
		// testing trailing clusters in two contexts: before a space and before the end of the input string
		System.out.println("Testing isTrailingCluster()");
		String input = "vanty Sas";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("vanty", "Sas");
		TokenStream res = tokenize(reader, new SkrtSylTokenizer());
		assertTokenStream(res, expected);
	}
	
	@Test
	public void testSylTokenizer() throws IOException {
		// the syllabation in "expected" is the output of Scharf's script
		System.out.println("Testing SkrtSylTokenizer()");
		String input = "pfTivyA lABe pAlane ca yAvanty arTa SAstrARi pUrva AcAryEH prasTApitAni prAyaSas tAni saMhftya^ekam idam arTa SAstraM kftam //";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("pf", "Ti", "vyA", "lA", "Be", "pA", "la", "ne", "ca", "yA", "vanty", "a", "rTa", "SA", "strA", "Ri", "pU", "rva", "A", "cA", "ryEH", "pra", "sTA", "pi", "tA", "ni", "prA", "ya", "Sas", "tA", "ni", "saM", "hf", "tya^e", "kam", "i", "dam", "a", "rTa", "SA", "straM", "kf", "tam", "//");

		System.out.println("0 " + input);
		TokenStream res = tokenize(reader, new SkrtSylTokenizer());
		assertTokenStream(res, expected);
	}
	
    @Test
    public void testTransliterationFilter() throws Exception {
        //CharFilter cs = new TransliterationFilter(new StringReader( "\u0915 \u0915\u094d\u0915 \u0915\u093F \u0915\u094d\u0915\u093F \u0933\u094d\u0939\u0941") );
    	System.out.println("Testing transliterationFilter()");
    	String input = "अथ राजकन्या चन्द्रवती नामाभिनवरुपयौवनसम्पन्ना सखीद्वितीयैकस्मिन्महोत्सवदिवसे नगरं निरिक्षमाणास्ति ।"; 
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	System.out.println("0 " + input);
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        List<String> expected = Arrays.asList("aTa", "rAjakanyA", "candravatI", "nAmABinavarupayOvanasampannA", "saKIdvitIyEkasminmahotsavadivase", "nagaraM", "nirikzamARAsti", ".");
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
    
//    @Test
//    public void thoroughTestDeva2SlpFilter() throws Exception {
//    	List<String> lines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-deva.txt"));
//    	String input = String.join(" ", lines);
//    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
//  	System.out.println("0 " + input);  	
//    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
//    	List<String> llines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-slp.txt"));
//    	List<String> expected = Arrays.asList(String.join(" ", llines).split(" "));
//    	assertTokenStream(ts, expected);
//    }
//    
//    @Test
//    public void thoroughTestRoman2SlpFilter() throws Exception {
//    	List<String> lines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-roman.txt"));
//    	String input = String.join(" ", lines);
//    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
//  	System.out.println("0 " + input);  	
//    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
//    	List<String> llines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-slp.txt"));
//    	List<String> expected = Arrays.asList(String.join(" ", llines).split(" "));
//    	assertTokenStream(ts, expected);
//    }

    @Test
    public void testParseCmd() throws IOException
    {
    	System.out.println("test parseCmd()");
    	// DarmA,a~-1+an;-1+a/-+a|A~-1+an;-1+a/-+A|~-1+an;-1+a/|c~-1+an;-1+a/- c+c|C~-1+an;-1+a/- C+C
    	String input = ",a~-1+an;-1+a/-+a|A~-1+an;-1+a/-+A|~-1+an;-1+a/|c~-1+an;-1+a/- c+c|C~-1+an;-1+a/- C+C";
    	String expected = "{A=[1+an, 1+an,a, 1+an,A, 1+a, 1+a,a, 1+a,A], Ac=[1+an,c, 1+a,c], AC=[1+an,C, 1+a,C]}";
    	System.out.println("0 " + input);
    	Map<String, HashSet<String>> res = CmdParser.parse("A", input);    	
    	System.out.println("1 " + expected);
    	System.out.println("2 " + res.toString() + "\n");
    	assertTrue(res.toString().equals(expected));
    }
    
    @Test
	public void SandhiedCompoundTest() throws IOException
	{
		System.out.println("sandhied compound test");
		String input = "DarmATa";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("Darman", "Darma", "aTa");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("resources/word-segmentation-resources/DarmATa_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}

    @Test
	public void testStartingWithNonword() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "ABCDEaTaFGH IJaTa"; // Darm is not in the Trie, aTa is
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ABCDE", "aTa", "FGH", "IJ", "aTa");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("resources/word-segmentation-resources/DarmATa_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void nonSandhiedCompoundTest() throws IOException
	{
		System.out.println("non-sandhied compound test");
		String input = "?budDaDarma.";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("budDa", "Darma");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("resources/word-segmentation-resources/budDaDarma_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
//    @Test
//	public void wordTokenizerTest() throws IOException
//	{
//		System.out.println("Testing SkrtWordTokenizer()");
//		String input = "aTa rAjakanyA candravatI nAmABinavarupayOvanasampannA saKIdvitIyEkasminmahotsavadivase nagaraM nirikzamARAsti";
//		Reader reader = new StringReader(input);
//		List<String> expected = Arrays.asList("aTa", "rAja", "kanyA", "candravatI", "nAmABinavarupayOvanasampannA", "saKI", "dvitIyA", "ekasmin", "mahA", "utsava", "divase", "na", "garam", "nirikzamARAsti");
//		System.out.println("0 " + input);
//		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("resources/word-segmentation-resources/aTa_test.txt");
//		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
//		assertTokenStream(syllables, expected);
//	}
    
    @Test
    public void testSylEndingCombinations() throws Exception {
    	
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}
