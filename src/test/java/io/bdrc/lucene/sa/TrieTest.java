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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

import static org.hamcrest.CoreMatchers.*;

/**
 * Unit tests for the Sanskrit tokenizers and filters.
 */
public class TrieTest
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
	public void sylTokenizerTest() throws IOException
	{
		System.out.println("Testing SkrtSylTokenizer()");
		String input = "AtmA atma";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("A", "tmA", "a" ,"tma");

		System.out.print(input + " => ");
		TokenStream res = tokenize(reader, new SkrtSylTokenizer());
		assertTokenStream(res, expected);
	}
	
	public boolean isTibLetter(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');
	}

	/**
	 *  this function is inspired from getLastOnPath() in stemmer's Trie.java
	 * @param toAnalyze the string to analyse
	 * @param startCharIndex the index from which we want to analyze
	 * @param t the Trie containing the data
	 */
	// 
	public void produceOneToken(String toAnalyze, int startCharIndex, Trie t) {
		// getting the root of the tree
//		System.out.println(toAnalyze);
		Row now = t.getRow(t.getRoot());
		int w; // temporary index variable
		int lastCharIndex = -1; // the index of the last match in the string we analyze
//		int lastCmdIndex = -1; // the index (inside the Trie) of the cmd corresponding to the last match
		
		int i = startCharIndex; // the current index in the string
		while (i < toAnalyze.length()) {
			Character ch = toAnalyze.charAt(i); // get the current character
//			System.out.println("moving to index "+i+": "+ch);
			w = now.getCmd(ch); // get the command associated with the current character at next step in the Trie
			if (w >= 0) {
				if (i >= toAnalyze.length()-1 || !isTibLetter(toAnalyze.charAt(i+1))) {
//						System.out.println("current row has an command for it, so it's a match");
//						lastCmdIndex = w;
						lastCharIndex = i;
					}
            } else {
//            	System.out.println("current row does not have a command for it, no match");
            }
			w = now.getRef(ch); // get the next row if there is one
			if (w >= 0) {
//				System.out.println("current row does have a reference for this char, further matches are possible, moving one row forward in the Trie");
                now = t.getRow(w);
            } else {
//            	System.out.println("current row does not have a reference to this char, so there's no further possible match, breaking the loop");
                break; // no more steps possible in our research
            }
			i++;
		}
		//w = now.getCmd(toAnalyze.charAt(i));
		if (lastCharIndex == -1) {
//			System.out.println("I have found nothing");
			return;
		}
//		System.out.println("I have found a token that goes from "+startCharIndex+" to "
//				+ lastCharIndex);
//		System.out.println("the substring is: "+toAnalyze.substring(startCharIndex, lastCharIndex+1));
//		System.out.println("the command associated with this token in the Trie is: "+t.getCommandVal(lastCmdIndex));
	}
	
	@Test
	public void produceOneTokenTest() throws IOException
	{
		System.out.println("Testing Stemmer Trie (produceOneToken() )");
		Trie test = new Trie(true);
		test.add("དྲོའི",">a");
		test.add("བདེ་ལེགས","=");
		test.add("བདེ", "=");
		test.add("བཀྲ་ཤིས","=");
		test.add("བཀྲ", "=");
		test.add("དྲོ","=");
		test.add("དགའི", ">A");
		test.add("དགའ","=");
		Optimizer opt = new Optimizer();
		test.reduce(opt);
		produceOneToken("དག", 0, test);
		produceOneToken("དགའི", 0, test);
		produceOneToken("བཀྲ་", 0, test);
		produceOneToken("བཀྲད", 0, test);
		produceOneToken("བདེ་ལེགས", 0, test);
	}
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}