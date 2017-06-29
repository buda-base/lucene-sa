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
	public void sylTokenizerTest() throws IOException
	{
		System.out.println("Testing SkrtSylTokenizer()");
		String input = "pfTivyA lABe pAlane ca yAvanty arTa SAstrARi pUrva AcAryEH prasTApitAni prAyaSas tAni saMhftya^ekam idam arTa SAstraM kftam //";
		// output from Sanskrit Library's syllabifier:
		// pf-Ti-vyA lA-Be pA-la-ne ca yA-vanty a-rTa SA-strA-Ri pU-rva A-cA-ryEH pra-sTA-pi-tA-ni prA-ya-Sas tA-ni saM-hf-tya^e-kam i-dam a-rTa SA-straM kf-tam //
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("pf", "Ti", "vyA", "lA", "Be", "pA", "la", "ne", "ca", "yA", "vanty", "a", "rTa", "SA", "strA", "Ri", "pU", "rva", "A", "cA", "ryEH", "pra", "sTA", "pi", "tA", "ni", "prA", "ya", "Sas", "tA", "ni", "saM", "hf", "tya^e", "kam", "i", "dam", "a", "rTa", "SA", "straM", "kf", "tam");

		System.out.print(input + "\n => \n");
		TokenStream res = tokenize(reader, new SkrtSylTokenizer());
		assertTokenStream(res, expected);
	}
	
	@Test
	public void testTransliterationFilter() throws Exception {
	    CharFilter cs = new TransliterationFilter(new StringReader( "k" ) );
	    TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
	    List<String> expected = Arrays.asList("k");
	    assertTokenStream(ts, expected);
	}
	
	@Test
	public void isSylEndTest() throws IOException
	{
		System.out.println("Testing isSylEnd()");
		SkrtSylTokenizer test = new SkrtSylTokenizer();
		char nonSLP = '?';
		char M = '#';
		char C = 'k';
		char X = 'M';
		char V = 'a';
		// I there is a syllable ending
		boolean syl_boundary = true;
		boolean no_syl_boundary = true;
		if (!test.isSylEnd(M, nonSLP)) {
			System.out.println("M + nonSLP: end of syl not recognized");
			syl_boundary = false;
		}
		if (!test.isSylEnd(C, nonSLP)) {
			System.out.println("C + nonSLP: end of syl not recognized");
			syl_boundary = false;
		}
		if (!test.isSylEnd(X, nonSLP)) {
			System.out.println("X + nonSLP: end of syl not recognized");
			syl_boundary = false;
		}
		if (!test.isSylEnd(V, nonSLP)) {
			System.out.println("V + nonSLP: end of syl not recognized");
			syl_boundary = false;
		}
		if (syl_boundary) {
			System.out.println("All syllable boundaries detected.");
		}
		// II there is no syllable ending
		if (test.isSylEnd(nonSLP, nonSLP)) {
			System.out.println("nonSLP nonSLP : boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(nonSLP, M)) {
			System.out.println("nonSLP M: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(M, M)) {
			System.out.println("M M: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(C, M)) {
			System.out.println("C M: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(X, M)) {
			System.out.println("X M: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(V, M)) {
			System.out.println("V M: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(nonSLP, C)) {
			System.out.println("nonSLP C: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(C, C)) {
			System.out.println("C C: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(nonSLP, X)) {
			System.out.println("nonSLP X: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(M, X)) {
			System.out.println("M X: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(C, X)) {
			System.out.println("C X: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(X, X)) {
			System.out.println("X X: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(V, X)) {
			System.out.println("V X: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(nonSLP, V)) {
			System.out.println("nonSLP V: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(M, V)) {
			System.out.println("M V: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(C, V)) {
			System.out.println("C V: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(X, V)) {
			System.out.println("X V: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (test.isSylEnd(V, V)) {
			System.out.println("V V: boundary wrongly detected");
			no_syl_boundary = false;
		}
		if (no_syl_boundary) {
			System.out.println("All non-boundaries left aside.");
		}
		test.close();
		assertThat(Arrays.asList(syl_boundary, no_syl_boundary), everyItem(is(true)));
	}
	
	@Test
	public void testTransliterationFilter() throws Exception {
	    CharFilter cs = new TransliterationFilter(new StringReader( "\u0915 \u0915\u094d\u0915 \u0915\u093F \u0915\u094d\u0915\u093F \u0933\u094d\u0939\u0941") );
	    TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
	    List<String> expected = Arrays.asList("ka", "kka", "ki", "kki", "|u");
	    assertTokenStream(ts, expected);
	}

	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}
