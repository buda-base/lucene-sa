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
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.AfterClass;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Trie;

/**
 * Test showing the Trie optimization modifies the entries in the Trie
 */
public class TrieTests
{
	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}
	
	static private List<String> generateTermList(TokenStream tokenStream) {
		List<String> termList = new ArrayList<String>();
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
		try {
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString() + "_" + typeAttribute.type());
			}
		} catch (IOException e) {
			assertTrue(false);
		}
		return termList;
	}
	
    @Test
    public void optimizationTest() throws IOException
    {
		Trie nonOptimizedTrie = BuildCompiledTrie.buildTrie(Arrays.asList("src/test/resources/tries/optimization_test_tattva.txt"));
		Trie optimizedTrie = BuildCompiledTrie.optimizeTrie(nonOptimizedTrie, new Optimizer());
		String input = "tattva";
		
		long one = System.currentTimeMillis();
		TokenStream fromOptimized = tokenize(new StringReader(input), new SkrtWordTokenizer(optimizedTrie));
		long two = System.currentTimeMillis();
		System.out.println("Loading time of the optimized Trie: " + (two - one) / 1000 + "s.");
		
		TokenStream fromNonOptimized = tokenize(new StringReader(input), new SkrtWordTokenizer(nonOptimizedTrie));		
		long three = System.currentTimeMillis();
		System.out.println("Loading time of the non-optimized Trie: " + (three - two) / 1000 + "s.");
		
		List<String> termsFromOptimized = generateTermList(fromOptimized);
		List<String> termsFromNonOptimized = generateTermList(fromNonOptimized);
		System.out.println(termsFromOptimized + "\n" + termsFromNonOptimized);
		
		assertThat(termsFromOptimized, is(not(termsFromNonOptimized)));
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}