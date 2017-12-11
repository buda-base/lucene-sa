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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Unit tests for the Sanskrit tokenizers and filters.
 */
public class FullTextParsingTest
{
	
//	static SkrtWordTokenizer skrtWordTokenizer = fillWordTokenizer();
//	
//	static private SkrtWordTokenizer fillWordTokenizer() {
//		try {
//			skrtWordTokenizer = new SkrtWordTokenizer(false);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return skrtWordTokenizer;
//	}
//	
//	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
//		tokenizer.close();
//		tokenizer.end();
//		tokenizer.setReader(reader);
//		tokenizer.reset();
//		return tokenizer;
//	}
//	
//	static private void printTokenStream(TokenStream tokenStream) {
//		try {
//			List<String> termList = new ArrayList<String>();
//			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
//			TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
//			while (tokenStream.incrementToken()) {
//				termList.add(charTermAttribute.toString());
//				System.out.println(charTermAttribute.toString() + " tokenType: " + typeAttribute.type());
//			}
//			System.out.println(String.join(" ", termList) + "\n");
//		} catch (IOException e) {
//			assertTrue(false);
//		}
//	}
//	
//	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
//		try {
//			List<String> termList = new ArrayList<String>();
//			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
//			TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
//			while (tokenStream.incrementToken()) {
//				termList.add(charTermAttribute.toString());
//				System.out.println(charTermAttribute.toString() + " tokenType: " + typeAttribute.type());
//			}
//			System.out.println("1 " + String.join(" ", expected));
//			System.out.println("2 " + String.join(" ", termList) + "\n");
//			assertThat(termList, is(expected));
//		} catch (IOException e) {
//			assertTrue(false);
//		}
//	}
//	
////	@Test
////    public void fullTest() throws Exception {
////    	System.out.println("parsing a SARIT text in devanagari and lemmatizing it");
////    	Reader input = new FileReader("src/test/resources/tattvasangrahapanjika_raw_deva.txt");  
////    	CharFilter cs = new Deva2SlpFilter(input);
////    	System.out.println("0 " + input);
////		TokenStream words = tokenize(cs, skrtWordTokenizer);
////		CharArraySet stopSet = StopFilter.makeStopSet(SanskritAnalyzer.getWordList("src/main/resources/skrt-stopwords.txt", "#"));
////		TokenStream result = new StopFilter(words, stopSet);
////		printTokenStream(result);
////    }
//	
//	@Test
//	public void bug1ExtraNonwordToken() throws Exception {
//		System.out.println("bug1");
//		String input = "ametaH";
//		Reader reader = new StringReader(input);
//    	System.out.println("0 " + input);
//		TokenStream words = tokenize(reader, skrtWordTokenizer);
//		List<String> expected = Arrays.asList("ameta", "H");
//		assertTokenStream(words, expected);
//	}
//	
//	@Test
//	public void bug2missingNonWord() throws Exception {
//		System.out.println("bug2");
//		String input = ". tattvasaNgrahaH";
//		Reader reader = new StringReader(input);
//    	System.out.println("0 " + input);
//		TokenStream words = tokenize(reader, skrtWordTokenizer);
//		List<String> expected = Arrays.asList("tad", "tva", "saNgraha");
//		assertTokenStream(words, expected);
//	}
//	
//	@Test
//	public void bug3WrongTokenSize() throws Exception {
//		System.out.println("bug3");
//		String input = "sAtmIBUtam";
//		Reader reader = new StringReader(input);
//    	System.out.println("0 " + input);
//		TokenStream words = tokenize(reader, skrtWordTokenizer);
//		List<String> expected = Arrays.asList("sAtma", "BU", "BUta");
//		assertTokenStream(words, expected);
//	}
//	
//    @Test
//    public void bug4missingM() throws Exception {
//        System.out.println("bug4");
//        String input = "SrIH. tattvasaNgrahaH. paYjikAsametaH. tattvasaMgrahasya";
//        Reader reader = new StringReader(input);
//        System.out.println("0 " + input);
//        TokenStream words = tokenize(reader, skrtWordTokenizer);
//        List<String> expected = Arrays.asList("SrI", "tad", "tva", "saNgraha", "paYjikAs", "ameta", "H", "tad", "tva", "sa", "M", "grahasya");
//        assertTokenStream(words, expected);
//    }
//	
//	@AfterClass
//	public static void finish() {
//		System.out.println("after the test sequence");
//		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
//	}
}