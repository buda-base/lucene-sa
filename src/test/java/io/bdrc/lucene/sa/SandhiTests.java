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
public class SandhiTests
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
	public void testSandhi1() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tad eva";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tat", "eva");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/1.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi2() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "samyag asti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("samyak", "asti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/2.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi3() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmasya cCAtraH";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("rAmasya", "cAtraH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/3.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi4() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "mAstu";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("mA", "astu");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/4.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi5() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "gacCatIti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("gacCati", "iti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/5.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi6() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "gurUpeti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("guru", "upeti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/6.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi7() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "neti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("na", "iti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/7.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi8() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmeRoktaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmeRa", "uktaH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/8.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi9() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "maharziH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("mahA", "fziH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/9.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi10() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "nEti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("na", "eti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/10.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi11() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "mahOzaDiH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("mahA", "ozaDiH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/11.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi12() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmasyEkyam";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmasya", "Ekyam");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/12.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi13() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "ityuvAca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("iti", "uvAca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/13.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi14() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devyasti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devI", "asti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/14.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi15() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devyAgacCati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devI", "AgacCati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/15.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi16() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "kurvadya";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("kuru", "adya");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/16.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi17() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "bahviti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("bahu", "iti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/17.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi18() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "maDvadmi";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("maDu", "admi");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/18.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi19() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "gurvAsanam";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("guru", "Asanam");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/19.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi20() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "te 'pi";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("te", "api");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/20.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi21() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "ta uvAca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("te", "uvAca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/21.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi22() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "gfha uta";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("gfhe", "uta");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/22.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi23() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "SriyA arTaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("SriyE", "arTaH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/23.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi24() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "uBAvuvAca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("uBO", "uvAca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/24.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi25() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "anuzwup";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("anuzwuB", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/25.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi26() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "suhft";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("suhfd", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/26.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi27() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "vAk";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("vAc", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/27.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi28() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "virAw";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("virAj", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/28.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi29() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "dik";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("diS", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/29.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi30() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "pustakam";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("pustakam", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/30.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi31() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "karman";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("karman", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/31.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi32() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tapaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tapas", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/32.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi33() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "pitaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("pitar", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/33.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi34() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "bhavan";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("bhavant", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/34.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi35() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "bhavan";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("bhavantkgtrnp", "");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/35.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi36() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "Bavaj janma";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("Bavat", "janma");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/36.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi37() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "etad Danam";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("etat", "Danam");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/37.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi38() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "Bavad deham";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("Bavat", "deham");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/38.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi39() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tac Caram";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tat", "Saram");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/39.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi40() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "pustakaM paWati";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("pustakam", "paWati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/40.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi41() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "vanaM gacCAmi";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("vanam", "gacCAmi");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/41.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi42() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "mahAR qamaraH";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("mahAn", "qamaraH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/42.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi43() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "etAMS cCAtraH";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("etAn", "cCAtraH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/43.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi44() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "gacCaMS ca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("gacCan", "ca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/44.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi45() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tAMs tAn";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tAn", "tAn");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/45.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi46() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "asmiMz wIkA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("asmin", "wIkA");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/46.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi47() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tal lokaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tat", "lokaH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/47.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi48() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tAM lokAn";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tAn", "lokAn");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/48.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi49() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "vAg Gi";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("vAk", "hi");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/49.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi50() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tad Di";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tat", "hi");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/50.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi51() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmo gacCati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "gacCati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/51.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi52() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmo 'sti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "asti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/52.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi53() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmaH karoti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "karoti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/53.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi54() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmaS calati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "calati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/54.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi55() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmaz wIkAm";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "wIkAm");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/55.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi56() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmas tu";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "tu");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/56.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi57() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAmaH patati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "patati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/57.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi58() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "rAma uvAca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("rAmaH", "uvAca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/58.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi59() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devA vadanti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devAH", "vadanti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/59.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi60() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devA eva";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devAH", "eva");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/60.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi61() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devAH kurvanti";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("devAH", "kurvanti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/61.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi62() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devAH patanti";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("devAH", "patanti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/62.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi63() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devAS ca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devAH", "ca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/63.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi64() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devAz wIkA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devAH", "wIkA");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/64.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi65() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "devAs tu";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("devAH", "tu");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/65.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi66() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "munir vadati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("muniH", "vadati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/66.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi67() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tEr uktam";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tEH", "uktam");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/67.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi68() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "BUr Buvas";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("BUH", "Buvas");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/68.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi69() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "muniH karoti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("muniH", "karoti");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/69.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi70() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "agniS ca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("agniH", "ca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/70.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi71() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "munez wIkAm";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("muneH", "wIkAm");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/71.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi72() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "tEs tu";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tEH", "tu");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/72.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi73() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "guruH patati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("guruH", "patati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/73.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi74() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "punaH punar";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("punar", "punar");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/74.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi75() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "punar milAmaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("punar", "milAmaH");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/75.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi76() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "punaH ramati";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("punar", "ramati");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/76.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi77() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "punar uvAca";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("punar", "uvAca");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/77.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi78() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordyaword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordi", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/78.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi79() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordyaword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordI", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/79.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi80() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordvaword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordu", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/80.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi81() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordvaword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordU", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/81.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi82() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "worde 'word";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("worde", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/82.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi83() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordavAword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordo", "Aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/83.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi84() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordaNN aword";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("wordaN", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/84.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi85() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordAN aword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordAN", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/85.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi86() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordann aword";
		Reader reader = new StringReader(input);
	List<String> expected = Arrays.asList("wordan", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/86.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi87() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordAn aword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordAn", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/87.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi88() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordY Sword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordn", "Sword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/88.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi89() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordo 'word";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordaH", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/89.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi90() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "worda oword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordaH", "oword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/90.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi91() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordir aword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordiH", "aword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/91.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi92() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordI rword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordiH", "rword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/92.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi93() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordU rword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordUH", "rword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/93.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi94() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordo gword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordaH", "gword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/94.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi95() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordaS cword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordaH", "cword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/95.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi96() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordA gword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordAH", "gword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/96.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi97() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordAS cword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordAH", "cword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/97.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi98() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordir gword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordiH", "gword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/98.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void testSandhi99() throws IOException
	{
		System.out.println("Testing SkrtWordTokenizer()");
		String input = "wordiS cword";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("wordiH", "cword");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/99.txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}

}