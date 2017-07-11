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
    	CharFilter cs = new TransliterationFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        List<String> expected = Arrays.asList("aTa", "rAjakanyA", "candravatI", "nAmABinavarupayOvanasampannA", "saKIdvitIyEkasminmahotsavadivase", "nagaraM", "nirikzamARAsti", ".");
        assertTokenStream(ts, expected);
    }
    
    @Test
    public void testZwjZwnjTranscoding() throws Exception {
    	System.out.println("Testing the filtering of ZWJ and ZWNJ in transliterationFilter()");
    	String input = "\u0915\u094d\u0937 \u0915\u094d\u200D\u0937 \u0915\u094d\u200C\u0937"; // respectively क्ष  and क्‍ष 
    	CharFilter cs = new TransliterationFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> expected = Arrays.asList("kza", "kza", "kza");
    	assertTokenStream(ts, expected);
    }
	
    @Test
    public void thoroughTestTransliterationFilter() throws Exception {
    	List<String> lines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-deva.txt"));
    	String input = String.join(" ", lines);
    	CharFilter cs = new TransliterationFilter(new StringReader(input));
    	TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
    	List<String> llines = Files.readAllLines(Paths.get("resources/transcoding-test-data/nala-slp.txt"));
    	List<String> expected = Arrays.asList(String.join(" ", llines).split(" "));
    	assertTokenStream(ts, expected);
    }
    
    @Test
    public void testSylEndingCombinations() throws Exception {
    	
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}
