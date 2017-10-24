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
		// the syllabation in "expected(char) c" is the output of Scharf's script
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

    @Test
    public void testParseCmd() throws IOException
    {
    	System.out.println("test parseCmd()");
    	// Darma,~/=0|c~-0+n/- cC+c=6|C~-0+n/- cC+C=6|A:i:u:U:f:e:E:o:O~-0+n/- +=1
    	String input = "$/=0|c$-0+n/- cC+c=6|C$-0+n/- cC+C=6|A:i:u:U:f:e:E:o:O$-0+n/- +=1";
    	String expected = "{aA=[0+n/A=1], au=[0+n/u=1], aU=[0+n/U=1], ae=[0+n/e=1], aE=[0+n/E=1], af=[0+n/f=1], macC=[0+n/c=6, 0+n/C=6], ai=[0+n/i=1], ao=[0+n/o=1], aO=[0+n/O=1]}";
    	System.out.println("0 " + input);
    	Map<String, HashSet<String>> res = new CmdParser().parse("Darma", input);    	
    	System.out.println("1 " + expected);
    	System.out.println("2 " + res.toString() + "\n");
    	assertTrue(res.toString().equals(expected));
    }
    
    @Test
    public void testContainsSandhiedCombinationNoSandhi() throws IOException
    {
    	System.out.println("test containsSandhiedCombination()");
    	char[] buffer = "budDaDarma".toCharArray();
    	int bufferIdx = 4;
    	String sandhied = "a";
    	boolean res = SkrtWordTokenizer.containsSandhiedCombination(buffer, bufferIdx, sandhied, 0); 
    	assertFalse(res);
    }

    @Test
    public void testContainsSandhiedCombinationVowelSandhi() throws IOException
    {
    	System.out.println("test containsSandhiedCombination()");
    	char[] buffer = "DarmATa".toCharArray();
    	int bufferIdx = 4;
    	String sandhied = "A";
    	boolean res = SkrtWordTokenizer.containsSandhiedCombination(buffer, bufferIdx, sandhied, 1); 
    	assertTrue(res);
    }

    @Test
    public void testContainsSandhiedCombinationAbsoluteFinals() throws IOException
    {
    	System.out.println("test containsSandhiedCombination()");
    	char[] buffer = "Darmaprsti".toCharArray();
    	int bufferIdx = 5;
    	String sandhied = "ap";
    	boolean res = SkrtWordTokenizer.containsSandhiedCombination(buffer, bufferIdx, sandhied, 5); 
    	assertTrue(res);
    }
    
    @Test
	public void SandhiedCompoundTest() throws IOException
	{
		System.out.println("sandhied compound test");
		String input = "DarmATa DarmADa DarmATa";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("Darman", "Darma", "aTa", "Darman", "Darma", "ADa", "Darman", "Darma", "aTa");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/DarmATa_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}

    @Test
	public void testStartingWithNonword() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "aTaAB CDEaTaFGH IJaTa"; // Darm is not in the Trie, aTa is
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa", "AB", "CDE", "aTa", "FGH", "IJ", "aTa");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/DarmATa_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatchEd() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "eded";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ed", "ed");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/abab_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}

    @Test
	public void testNonMaximalMatch() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "abab";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aba", "b");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/abab_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatchFollowedWithNonWord() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "ababa";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aba", "ba");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/abab_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatchFollowedWithNonWordEd() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "edede";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ed", "ed", "e");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(true, "src/test/resources/tries/abab_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatchPrecededWithNonWord() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "auieabab";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("auie", "aba", "b");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/abab_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testSandhiWithSpace() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "te 'pi te'pi"; // Darm is not in the Trie, aTa is
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tad", "api", "tad", "api");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/te'pi_test.txt");
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
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/budDaDarma_test.txt");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void bug1WordTokenizerTest() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "aTa rAjakanyA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa", "rAja", "kanya", "kana");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testDeletingInitialsOnEndOfInput() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "kanyA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("kanya", "kana");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testExtraTokenWithInitialsBug() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "kanyA candravatI";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("kanya", "kana", "candravatI");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testExtraTokenBug() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "divase na";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("divasa", "na");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testMissingTokenBug() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "saKIdvitIyEkasmin";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("saKi", "dvitIya", "ekasmin");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void wordTokenizerTest() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		// Sanskrit Heritage site outputs: 
		//    "aTa rAja kanyA candravatI nAmABinavarupayOvanasampannA saKI dvitIyA ekasmin maha utsava divase na garaM nirikzamARAsti"
		// We do lemmatization and "nAmA..." is split because "na" exists in the Trie 
		String input = "aTa rAjakanyA candravatI nAmABinavarupayOvanasampannA saKIdvitIyEkasminmahotsavadivase nagaraM nirikzamARAsti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa", "rAja", "kanya", "kana", "candravatI", "nAmABi", "na", "varupayOva", "na", "sampannA", "saKi", "dvitIya", "ekasmin", "maho", "maha", "utsava", "divasa", "na", "garaM", "nirikzamARAsti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testBugNonSLP() throws IOException
	{
		System.out.println("Testing nonSLP input");
		String input = "ka%nyA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ka", "nyA");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(true, "src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testNonSLPString() throws IOException
	{
		System.out.println("Testing nonSLP input");
		String input = "«»(**-éàÀ%$–@)";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList();
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testSLPStringWithModifiers() throws IOException
	{
		System.out.println("Testing nonSLP input");
		String input = "a+Ta/8 rA+ja^1ka\\nyA^97";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa", "rAja", "kanya", "kana");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testMixedSLPNonSLPString() throws IOException
	{
		System.out.println("Testing nonSLP input");
		String input = "«»(**-éàÀ%$–@)aTa rAjakanyA«»(**- éàÀ%$–@)aTa rAjakanyA «»(**- éàÀ%$–@)";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa", "rAja", "kanya", "kana", "aTa", "rAja", "kanya", "kana");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/aTa_test.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
    public void testSylEndingCombinations() throws Exception {
    	
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}
