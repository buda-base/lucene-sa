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

import java.io.FileNotFoundException;
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

import io.bdrc.lucene.sa.SkrtWordTokenizer;
import io.bdrc.lucene.stemmer.Trie;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for as many sandhi cases as possible.
 */
public class TestSandhis
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
			for (String term: expected) {
			    assertThat(termList, hasItems(term));
			}
			
		} catch (IOException e) {
			assertTrue(false);
		}
	}
	
	static private void assertSandhi(String input, List<String> expected, int trieNumber) throws FileNotFoundException, IOException {
		Reader reader = new StringReader(input);
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("resources/sanskrit-stemming-data/output/tries/"+Integer.toString(trieNumber)); 
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
	
	static private SkrtWordTokenizer buildTokenizer(String trieName) throws FileNotFoundException, IOException {
		Trie trie = BuildCompiledTrie.buildTrie(trieName + ".txt");
		
		return new SkrtWordTokenizer(true, trie);
	}
	
	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
	
    @Test
	public void testSandhi1() throws IOException
	{
    	assertSandhi("tad eva", Arrays.asList("tat", "eva"), 1);
	}

    @Test
	public void testSandhi2() throws IOException
	{
		assertSandhi("samyag asti", Arrays.asList("samyak", "asti"),  2);
	}

    @Test
	public void testSandhi3() throws IOException
	{
		assertSandhi("rAmasya cCAtraH", Arrays.asList("rAmasya", "CAtraH"),  3);
	}

    @Test
	public void testSandhi4() throws IOException
	{ 
		assertSandhi("mAstu", Arrays.asList("mA", "astu"),  4);
	}

    @Test
	public void testSandhi5() throws IOException
	{
		assertSandhi("gacCatIti", Arrays.asList("gacCati", "iti"),  5);
	}

    @Test
	public void testSandhi6() throws IOException
	{
		assertSandhi("gurUpeti", Arrays.asList("guru", "upeti"),  6);
	}

    @Test
	public void testSandhi7() throws IOException
	{
		assertSandhi("neti", Arrays.asList("na", "iti"),  7);
	}

    @Test
	public void testSandhi8() throws IOException
	{
		assertSandhi("rAmeRoktaH", Arrays.asList("rAmeRa", "uktaH"),  8);
	}

    @Test
	public void testSandhi9() throws IOException
	{
		assertSandhi("maharziH", Arrays.asList("mahA", "fziH"),  9);
	}

    @Test
	public void testSandhi10() throws IOException
	{
		assertSandhi("nEti", Arrays.asList("na", "eti"),  10);
	}

    @Test
	public void testSandhi11() throws IOException
	{
		assertSandhi("mahOzaDiH", Arrays.asList("mahA", "ozaDiH"),  11);
	}

    @Test
	public void testSandhi12() throws IOException
	{
		assertSandhi("rAmasyEkyam", Arrays.asList("rAmasya", "Ekyam"),  12);
	}

    @Test
	public void testSandhi13() throws IOException
	{
		assertSandhi("ityuvAca", Arrays.asList("iti", "uvAca"),  13);
	}

    @Test
	public void testSandhi14() throws IOException
	{
		assertSandhi("devyasti", Arrays.asList("devI", "asti"),  14);
	}

    @Test
	public void testSandhi15() throws IOException
	{
		assertSandhi("devyAgacCati", Arrays.asList("devI", "AgacCati"),  15);
	}

    @Test
	public void testSandhi16() throws IOException
	{
		assertSandhi("kurvadya", Arrays.asList("kuru", "adya"),  16);
	}

    @Test
	public void testSandhi17() throws IOException
	{
		assertSandhi("bahviti", Arrays.asList("bahu", "iti"),  17);
	}

    @Test
	public void testSandhi18() throws IOException
	{
		assertSandhi("maDvadmi", Arrays.asList("maDu", "admi"),  18);
	}

    @Test
	public void testSandhi19() throws IOException
	{
		assertSandhi("gurvAsanam", Arrays.asList("guru", "Asanam"),  19);
	}

    @Test
	public void testSandhi20() throws IOException
	{
		assertSandhi("te 'pi", Arrays.asList("te", "api"),  20);
	}

    @Test
	public void testSandhi21() throws IOException
	{
		assertSandhi("ta uvAca", Arrays.asList("te", "uvAca"),  21);
	}

    @Test
	public void testSandhi22() throws IOException
	{
		assertSandhi("gfha uta", Arrays.asList("gfhe", "uta"),  22);
	}

    @Test
	public void testSandhi23() throws IOException
	{
		assertSandhi("SriyA arTaH", Arrays.asList("SriyE", "arTaH"),  23);
	}

    @Test
	public void testSandhi24() throws IOException
	{
		assertSandhi("uBAvuvAca", Arrays.asList("uBO", "uvAca"),  24);
	}

    @Test
	public void testSandhi25() throws IOException
	{
		assertSandhi("anuzwup", Arrays.asList("anuzwuB"),  25);
	}

    @Test
	public void testSandhi26() throws IOException
	{
		assertSandhi("suhft", Arrays.asList("suhfd"),  26);
	}

    @Test
	public void testSandhi27() throws IOException
	{
		assertSandhi("vAk", Arrays.asList("vAc"),  27);
	}

    @Test
	public void testSandhi28() throws IOException
	{
		assertSandhi("virAw", Arrays.asList("virAj"),  28);
	}

    @Test
	public void testSandhi29() throws IOException
	{
		assertSandhi("dik", Arrays.asList("diS"),  29);
	}

    @Test
	public void testSandhi30() throws IOException
	{
		assertSandhi("pustakam", Arrays.asList("pustakam"),  30);
	}

    @Test
	public void testSandhi31() throws IOException
	{
		assertSandhi("karman", Arrays.asList("karman"),  31);
	}

    @Test
	public void testSandhi32() throws IOException
	{
		assertSandhi("tapaH", Arrays.asList("tapas"),  32);
	}

    @Test
	public void testSandhi33() throws IOException
	{
		assertSandhi("pitaH", Arrays.asList("pitar"),  33);
	}

    @Test
	public void testSandhi34() throws IOException
	{
		assertSandhi("Bavan", Arrays.asList("Bavant"),  34);
	}

    @Test
	public void testSandhi35() throws IOException
	{
		assertSandhi("Bavan", Arrays.asList("Bavantkgtrnp"),  35);
	}

    @Test
	public void testSandhi36() throws IOException
	{
		assertSandhi("Bavaj janma", Arrays.asList("Bavat", "janma"),  36);
	}

    @Test
	public void testSandhi37() throws IOException
	{
		assertSandhi("etad Danam", Arrays.asList("etat", "Danam"),  37);
	}

    @Test
	public void testSandhi38() throws IOException
	{
		assertSandhi("Bavad deham", Arrays.asList("Bavat", "deham"),  38);
	}

    @Test
	public void testSandhi39() throws IOException
	{
		assertSandhi("tac Caram", Arrays.asList("tat", "Saram"),  39);
	}

    @Test
	public void testSandhi40() throws IOException
	{
		assertSandhi("pustakaM paWati", Arrays.asList("pustakam", "paWati"),  40);
	}

    @Test
	public void testSandhi41() throws IOException
	{
		assertSandhi("vanaM gacCAmi", Arrays.asList("vanam", "gacCAmi"),  41);
	}

    @Test
	public void testSandhi42() throws IOException
	{
		assertSandhi("mahAR qamaraH", Arrays.asList("mahAn", "qamaraH"),  42);
	}

    @Test
	public void testSandhi43() throws IOException
	{
		assertSandhi("etAMS cakra", Arrays.asList("etAn", "cakra"),  43);
	}

    @Test
	public void testSandhi44() throws IOException
	{
		assertSandhi("gacCaMS ca", Arrays.asList("gacCan", "ca"),  44);
	}

    @Test
	public void testSandhi45() throws IOException
	{
		assertSandhi("tAMs tAn", Arrays.asList("tAn", "tAn"),  45);
	}

    @Test
	public void testSandhi46() throws IOException
	{
		assertSandhi("asmiMz wIkA", Arrays.asList("asmin", "wIkA"),  46);
	}

    @Test
	public void testSandhi47() throws IOException
	{
		assertSandhi("tal lokaH", Arrays.asList("tat", "lokaH"),  47);
	}

    @Test
	public void testSandhi48() throws IOException
	{
		assertSandhi("tAl~ lokAn", Arrays.asList("tAn", "lokAn"),  48);
	}

    @Test
	public void testSandhi49() throws IOException
	{
		assertSandhi("vAg Gi", Arrays.asList("vAk", "hi"),  49);
	}

    @Test
	public void testSandhi50() throws IOException
	{
		assertSandhi("tad Di", Arrays.asList("tat", "hi"),  50);
	}

    @Test
	public void testSandhi51() throws IOException
	{
		assertSandhi("rAmo gacCati", Arrays.asList("rAmaH", "gacCati"),  51);
	}

    @Test
	public void testSandhi52() throws IOException
	{
		assertSandhi("rAmo 'sti", Arrays.asList("rAmaH", "asti"),  52);
	}

    @Test
	public void testSandhi53() throws IOException
	{
		assertSandhi("rAmaH karoti", Arrays.asList("rAmaH", "karoti"),  53);
	}

    @Test
	public void testSandhi54() throws IOException
	{
		assertSandhi("rAmaS calati", Arrays.asList("rAmaH", "calati"),  54);
	}

    @Test
	public void testSandhi55() throws IOException
	{
		assertSandhi("rAmaz wIkAm", Arrays.asList("rAmaH", "wIkAm"),  55);
	}

    @Test
	public void testSandhi56() throws IOException
	{
		assertSandhi("rAmas tu", Arrays.asList("rAmaH", "tu"),  56);
	}

    @Test
	public void testSandhi57() throws IOException
	{
		assertSandhi("rAmaH patati", Arrays.asList("rAmaH", "patati"),  57);
	}

    @Test
	public void testSandhi58() throws IOException
	{
		assertSandhi("rAma uvAca", Arrays.asList("rAmaH", "uvAca"),  58);
	}

    @Test
	public void testSandhi59() throws IOException
	{
		assertSandhi("devA vadanti", Arrays.asList("devAH", "vadanti"),  59);
	}

    @Test
	public void testSandhi60() throws IOException
	{
		assertSandhi("devA eva", Arrays.asList("devAH", "eva"),  60);
	}

    @Test
	public void testSandhi61() throws IOException
	{
		assertSandhi("devAH kurvanti", Arrays.asList("devAH", "kurvanti"),  61);
	}

    @Test
	public void testSandhi62() throws IOException
	{
		assertSandhi("devAH patanti", Arrays.asList("devAH", "patanti"),  62);
	}

    @Test
	public void testSandhi63() throws IOException
	{
		assertSandhi("devAS ca", Arrays.asList("devAH", "ca"),  63);
	}

    @Test
	public void testSandhi64() throws IOException
	{
		assertSandhi("devAz wIkA", Arrays.asList("devAH", "wIkA"),  64);
	}

    @Test
	public void testSandhi65() throws IOException
	{
		assertSandhi("devAs tu", Arrays.asList("devAH", "tu"),  65);
	}

    @Test
	public void testSandhi66() throws IOException
	{
		assertSandhi("munir vadati", Arrays.asList("muniH", "vadati"),  66);
	}

    @Test
	public void testSandhi67() throws IOException
	{
		assertSandhi("tEr uktam", Arrays.asList("tEH", "uktam"),  67);
	}

    @Test
	public void testSandhi68() throws IOException
	{
		assertSandhi("BUr Buvas", Arrays.asList("BUH", "Buvas"),  68);
	}

    @Test
	public void testSandhi69() throws IOException
	{
		assertSandhi("muniH karoti", Arrays.asList("muniH", "karoti"),  69);
	}

    @Test
	public void testSandhi70() throws IOException
	{
		assertSandhi("agniS ca", Arrays.asList("agniH", "ca"),  70);
	}

    @Test
	public void testSandhi71() throws IOException
	{
		assertSandhi("munez wIkAm", Arrays.asList("muneH", "wIkAm"),  71);
	}

    @Test
	public void testSandhi72() throws IOException
	{
		assertSandhi("tEs tu", Arrays.asList("tEH", "tu"),  72);
	}

    @Test
	public void testSandhi73() throws IOException
	{
		assertSandhi("guruH patati", Arrays.asList("guruH", "patati"),  73);
	}

    @Test
	public void testSandhi74() throws IOException
	{
		assertSandhi("punaH punar", Arrays.asList("punar", "punar"),  74);
	}

    @Test
	public void testSandhi75() throws IOException
	{
		assertSandhi("punar milAmaH", Arrays.asList("punar", "milAmaH"),  75);
	}

    @Test
	public void testSandhi76() throws IOException
	{
		assertSandhi("punaH ramati", Arrays.asList("punar", "ramati"),  76);
	}

    @Test
	public void testSandhi77() throws IOException
	{
		assertSandhi("punar uvAca", Arrays.asList("punar", "uvAca"),  77);
	}

    @Test
	public void testSandhi78() throws IOException
	{
		assertSandhi("wordyaword", Arrays.asList("wordi", "aword"),  78);
	}

    @Test
	public void testSandhi79() throws IOException
	{
		assertSandhi("wordyaword", Arrays.asList("wordI", "aword"),  79);
	}

    @Test
	public void testSandhi80() throws IOException
	{
		assertSandhi("wordvaword", Arrays.asList("wordu", "aword"),  80);
	}

    @Test
	public void testSandhi81() throws IOException
	{
		assertSandhi("wordvaword", Arrays.asList("wordU", "aword"),  81);
	}

    @Test
	public void testSandhi82() throws IOException
	{
		assertSandhi("worde 'word", Arrays.asList("worde", "aword"),  82);
	}

    @Test
	public void testSandhi83() throws IOException
	{
		assertSandhi("wordavAword", Arrays.asList("wordo", "Aword"),  83);
	}

    @Test
	public void testSandhi84() throws IOException
	{
    	assertSandhi("wordaNN aword", Arrays.asList("wordaN", "aword"), 84);
	}

    @Test
	public void testSandhi85() throws IOException
	{
		assertSandhi("wordAN aword", Arrays.asList("wordAN", "aword"),  85);
	}

    @Test
	public void testSandhi86() throws IOException
	{
    	assertSandhi("wordann aword", Arrays.asList("wordan", "aword"), 86);
	}

    @Test
	public void testSandhi87() throws IOException
	{
		assertSandhi("wordAn aword", Arrays.asList("wordAn", "aword"),  87);
	}

    @Test
	public void testSandhi88() throws IOException
	{
		assertSandhi("wordY Sword", Arrays.asList("wordn", "Sword"),  88);
	}

    @Test
	public void testSandhi89() throws IOException
	{
		assertSandhi("wordo 'word", Arrays.asList("wordaH", "aword"),  89);
	}

    @Test
	public void testSandhi90() throws IOException
	{
		assertSandhi("worda oword", Arrays.asList("wordaH", "oword"),  90);
	}

    @Test
	public void testSandhi91() throws IOException
	{
		assertSandhi("wordir aword", Arrays.asList("wordiH", "aword"),  91);
	}

    @Test
	public void testSandhi92() throws IOException
	{
		assertSandhi("wordI rword", Arrays.asList("wordiH", "rword"),  92);
	}

    @Test
	public void testSandhi93() throws IOException
	{
		assertSandhi("wordU rword", Arrays.asList("wordUH", "rword"),  93);
	}

    @Test
	public void testSandhi94() throws IOException
	{
		assertSandhi("wordo gword", Arrays.asList("wordaH", "gword"),  94);
	}

    @Test
	public void testSandhi95() throws IOException
	{
		assertSandhi("wordaS cword", Arrays.asList("wordaH", "cword"),  95);
	}

    @Test
	public void testSandhi96() throws IOException
	{
		assertSandhi("wordA gword", Arrays.asList("wordAH", "gword"),  96);
	}

    @Test
	public void testSandhi97() throws IOException
	{
		assertSandhi("wordAS cword", Arrays.asList("wordAH", "cword"),  97);
	}

    @Test
	public void testSandhi98() throws IOException
	{
		assertSandhi("wordir gword", Arrays.asList("wordiH", "gword"),  98);
	}

    @Test
	public void testSandhi99() throws IOException
	{
		assertSandhi("wordiS cword", Arrays.asList("wordiH", "cword"),  99);
	}

    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}

}