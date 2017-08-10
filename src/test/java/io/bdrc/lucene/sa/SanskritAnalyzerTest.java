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
import java.nio.file.Files;
import java.nio.file.Paths;
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

import io.bdrc.lucene.sa.SkrtWordTokenizer;
import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

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
			System.out.println(String.join(" ", termList));
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
		// the syllabation in "expected" is the output of Scharf's script
		System.out.println("Testing SkrtSylTokenizer()");
		String input = "pfTivyA lABe pAlane ca yAvanty arTa SAstrARi pUrva AcAryEH prasTApitAni prAyaSas tAni saMhftya^ekam idam arTa SAstraM kftam //";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("pf", "Ti", "vyA", "lA", "Be", "pA", "la", "ne", "ca", "yA", "vanty", "a", "rTa", "SA", "strA", "Ri", "pU", "rva", "A", "cA", "ryEH", "pra", "sTA", "pi", "tA", "ni", "prA", "ya", "Sas", "tA", "ni", "saM", "hf", "tya^e", "kam", "i", "dam", "a", "rTa", "SA", "straM", "kf", "tam", "//");

		System.out.print(input + "\n => \n");
		TokenStream res = tokenize(reader, new SkrtSylTokenizer());
		assertTokenStream(res, expected);
	}
	
    @Test
    public void testTransliterationFilter() throws Exception {
        //CharFilter cs = new TransliterationFilter(new StringReader( "\u0915 \u0915\u094d\u0915 \u0915\u093F \u0915\u094d\u0915\u093F \u0933\u094d\u0939\u0941") );
    	System.out.println("Testing transliterationFilter()");
    	String input = "अथ राजकन्या चन्द्रवती नामाभिनवरुपयौवनसम्पन्ना सखीद्वितीयैकस्मिन्महोत्सवदिवसे नगरं निरिक्षमाणास्ति ।"; 
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        List<String> expected = Arrays.asList("aTa", "rAjakanyA", "candravatI", "nAmABinavarupayOvanasampannA", "saKIdvitIyEkasminmahotsavadivase", "nagaraM", "nirikzamARAsti", ".");
        assertTokenStream(ts, expected);
    }
    
    @Test
    public void testZwjZwnjTranscoding() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "\u0915\u094d\u0937 \u0915\u094d\u200D\u0937 \u0915\u094d\u200C\u0937"; // respectively क्ष  and क्‍ष 
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("kza", "kza", "kza");
    	assertTokenStream(ts, expected);
    }
	
    @Test
    public void testRoman2SlpNfcNfd() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "\u1e5d \u1e5b\u0304 r\u0323\u0304"; // NFC, semi-NFD and NFD versions of ṝ 
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("F", "F", "F");
    	assertTokenStream(ts, expected);
    }

    @Test
    public void testRoman2SlpIso15919() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "ẏ m̆b ē k͟h"; // normalizations and deletions 
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("y", "e");
    	assertTokenStream(ts, expected);
    }
    
    @Test
    public void thoroughTestDeva2SlpFilter() throws Exception {
    	List<String> lines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-deva.txt"));
    	String input = String.join(" ", lines);
    	CharFilter cs = new Deva2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> llines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-slp.txt"));
    	List<String> expected = Arrays.asList(String.join(" ", llines).split(" "));
    	assertTokenStream(ts, expected);
    }
    
    @Test
    public void thoroughTestRoman2SlpFilter() throws Exception {
    	List<String> lines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-roman.txt"));
    	String input = String.join(" ", lines);
    	CharFilter cs = new Roman2SlpFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> llines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-slp.txt"));
    	List<String> expected = Arrays.asList(String.join(" ", llines).split(" "));
    	assertTokenStream(ts, expected);
    }
    
	public void produceOneToken(String toAnalyze, int startCharIndex, Trie t) {
		// getting the root of the tree
		System.out.println(toAnalyze);
		Row now = t.getRow(t.getRoot());
		int w; // temporary index variable
		int lastCharIndex = -1; // the index of the last match in the string we analyze
		int lastCmdIndex = -1; // the index (inside the Trie) of the cmd corresponding to the last match
		
		int i = startCharIndex; // the current index in the string
		while (i < toAnalyze.length()) {
			Character ch = toAnalyze.charAt(i); // get the current character
			System.out.println("moving to index "+i+": "+ch);
			w = now.getCmd(ch); // get the command associated with the current character at next step in the Trie
			if (w >= 0) {
				if (i >= toAnalyze.length()-1 || !SkrtSylTokenizer.isSLP(toAnalyze.charAt(i+1))) {
						System.out.println("current row has an command for it, so it's a match");
						lastCmdIndex = w;
						lastCharIndex = i;
					}
            } else {
            	System.out.println("current row does not have a command for it, no match");
            }
			w = now.getRef(ch); // get the next row if there is one
			if (w >= 0) {
				System.out.println("current row does have a reference for this char, further matches are possible, moving one row forward in the Trie");
                now = t.getRow(w);
            } else {
            	System.out.println("current row does not have a reference to this char, so there's no further possible match, breaking the loop");
                break; // no more steps possible in our research
            }
			i++;
		}
		//w = now.getCmd(toAnalyze.charAt(i));
		if (lastCharIndex == -1) {
			System.out.println("I have found nothing");
			return;
		}
		System.out.println("I have found a token that goes from "+startCharIndex+" to "
				+ lastCharIndex);
		System.out.println("the substring is: "+toAnalyze.substring(startCharIndex, lastCharIndex+1));
		System.out.println("the command associated with this token in the Trie is: "+t.getCommandVal(lastCmdIndex));
	}
	
	@Test
	public void produceOneTokenTest() throws IOException
	{
		System.out.println("Testing Stemmer Trie (produceOneToken() )");
		Trie test = new Trie(true);
		test.add("aTa", "a");
		test.add("rAja", "a");
		test.add("kanyA", "a");
		test.add("candravatI", "a");
		test.add("nAmABinavarupayOvanasampannA", "a");
		test.add("saKI", "a");
		test.add("dvitIyA", "a");
		test.add("ekasmin", "a");
		test.add("mahA", "a");
		test.add("utsava", "a");
		test.add("divase", "a");
		test.add("na", "a");
		test.add("garam", "a");
		test.add("nirikzamARAsti", "a");
		Optimizer opt = new Optimizer();
		test.reduce(opt);
		produceOneToken("saKI", 0, test);
		produceOneToken("saKIa", 0, test);
	}
    
    @Test
	public void wordTokenizerTest() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "aTa rAjakanyA candravatI nAmABinavarupayOvanasampannA saKIdvitIyEkasminmahotsavadivase nagaraM nirikzamARAsti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa", "rAja", "kanyA", "candravatI", "nAmABinavarupayOvanasampannA", "saKI", "dvitIyA", "ekasmin", "mahA", "utsava", "divase", "na", "garam", "nirikzamARAsti");
		System.out.print(input + " => ");
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("resources/word-segmentation-resources/test_exact_entries.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
    public void testSylEndingCombinations() throws Exception {
    	
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}
