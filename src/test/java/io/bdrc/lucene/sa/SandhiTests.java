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

import java.io.FileNotFoundException;
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
	
	static private void testSandhi(String input, String[] unsandhied, int trieNumber) throws FileNotFoundException, IOException {
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList(unsandhied);
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/sandhi-tests/"+Integer.toString(trieNumber)+".txt");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
	
    @Test
	public void testSandhi1() throws IOException
	{
		String[] unsandhied = new String[]{"tat", "eva"};
    	testSandhi("tad eva", unsandhied, 1);
	}

    @Test
	public void testSandhi2() throws IOException
	{
		String[] unsandhied = new String[]{"samyak", "samyac", "asti"};
		testSandhi("samyag asti", unsandhied, 2);
	}

    @Test
	public void testSandhi3() throws IOException
	{
		String[] unsandhied = new String[]{"rAmasya", "CAtraH"};
		testSandhi("rAmasya cCAtraH", unsandhied, 3);
	}

    @Test
	public void testSandhi4() throws IOException
	{
		String[] unsandhied = new String[]{"mA", "astu"};
		testSandhi("mAstu", unsandhied, 4);
	}

    @Test
	public void testSandhi5() throws IOException
	{
		String[] unsandhied = new String[]{"gacCati", "iti"};
		testSandhi("gacCatIti", unsandhied, 5);
	}

    @Test
	public void testSandhi6() throws IOException
	{
		String[] unsandhied = new String[]{"guru", "upeti"};
		testSandhi("gurUpeti", unsandhied, 6);
	}

    @Test
	public void testSandhi7() throws IOException
	{
		String[] unsandhied = new String[]{"na", "iti"};
		testSandhi("neti", unsandhied, 7);
	}

    @Test
	public void testSandhi8() throws IOException
	{
		String[] unsandhied = new String[]{"rAmeRa", "uktaH"};
		testSandhi("rAmeRoktaH", unsandhied, 8);
	}

    @Test
	public void testSandhi9() throws IOException
	{
		String[] unsandhied = new String[]{"mahA", "fziH"};
		testSandhi("maharziH", unsandhied, 9);
	}

    @Test
	public void testSandhi10() throws IOException
	{
		String[] unsandhied = new String[]{"na", "eti"};
		testSandhi("nEti", unsandhied, 10);
	}

    @Test
	public void testSandhi11() throws IOException
	{
		String[] unsandhied = new String[]{"mahA", "ozaDiH"};
		testSandhi("mahOzaDiH", unsandhied, 11);
	}

    @Test
	public void testSandhi12() throws IOException
	{
		String[] unsandhied = new String[]{"rAmasya", "Ekyam"};
		testSandhi("rAmasyEkyam", unsandhied, 12);
	}

    @Test
	public void testSandhi13() throws IOException
	{
		String[] unsandhied = new String[]{"iti", "uvAca"};
		testSandhi("ityuvAca", unsandhied, 13);
	}

    @Test
	public void testSandhi14() throws IOException
	{
		String[] unsandhied = new String[]{"devI", "asti"};
		testSandhi("devyasti", unsandhied, 14);
	}

    @Test
	public void testSandhi15() throws IOException
	{
		String[] unsandhied = new String[]{"devI", "AgacCati"};
		testSandhi("devyAgacCati", unsandhied, 15);
	}

    @Test
	public void testSandhi16() throws IOException
	{
		String[] unsandhied = new String[]{"kuru", "adya"};
		testSandhi("kurvadya", unsandhied, 16);
	}

    @Test
	public void testSandhi17() throws IOException
	{
		String[] unsandhied = new String[]{"bahu", "iti"};
		testSandhi("bahviti", unsandhied, 17);
	}

    @Test
	public void testSandhi18() throws IOException
	{
		String[] unsandhied = new String[]{"maDu", "admi"};
		testSandhi("maDvadmi", unsandhied, 18);
	}

    @Test
	public void testSandhi19() throws IOException
	{
		String[] unsandhied = new String[]{"guru", "Asanam"};
		testSandhi("gurvAsanam", unsandhied, 19);
	}

    @Test
	public void testSandhi20() throws IOException
	{
		String[] unsandhied = new String[]{"te", "api"};
		testSandhi("te 'pi", unsandhied, 20);
	}

    @Test
	public void testSandhi21() throws IOException
	{
		String[] unsandhied = new String[]{"te", "uvAca"};
		testSandhi("ta uvAca", unsandhied, 21);
	}

    @Test
	public void testSandhi22() throws IOException
	{
		String[] unsandhied = new String[]{"gfhe", "uta"};
		testSandhi("gfha uta", unsandhied, 22);
	}

    @Test
	public void testSandhi23() throws IOException
	{
		String[] unsandhied = new String[]{"SriyE", "arTaH"};
		testSandhi("SriyA arTaH", unsandhied, 23);
	}

    @Test
	public void testSandhi24() throws IOException
	{
		String[] unsandhied = new String[]{"uBO", "uvAca"};
		testSandhi("uBAvuvAca", unsandhied, 24);
	}

    @Test
	public void testSandhi25() throws IOException
	{
		String[] unsandhied = new String[]{"anuzwuB"};
		testSandhi("anuzwup", unsandhied, 25);
	}

    @Test
	public void testSandhi26() throws IOException
	{
		String[] unsandhied = new String[]{"suhfd"};
		testSandhi("suhft", unsandhied, 26);
	}

    @Test
	public void testSandhi27() throws IOException
	{
		String[] unsandhied = new String[]{"vAc"};
		testSandhi("vAk", unsandhied, 27);
	}

    @Test
	public void testSandhi28() throws IOException
	{
		String[] unsandhied = new String[]{"virAj"};
		testSandhi("virAw", unsandhied, 28);
	}

    @Test
	public void testSandhi29() throws IOException
	{
		String[] unsandhied = new String[]{"diS"};
		testSandhi("dik", unsandhied, 29);
	}

    @Test
	public void testSandhi30() throws IOException
	{
		String[] unsandhied = new String[]{"pustakam"};
		testSandhi("pustakam", unsandhied, 30);
	}

    @Test
	public void testSandhi31() throws IOException
	{
		String[] unsandhied = new String[]{"karman"};
		testSandhi("karman", unsandhied, 31);
	}

    @Test
	public void testSandhi32() throws IOException
	{
		String[] unsandhied = new String[]{"tapas"};
		testSandhi("tapaH", unsandhied, 32);
	}

    @Test
	public void testSandhi33() throws IOException
	{
		String[] unsandhied = new String[]{"pitar"};
		testSandhi("pitaH", unsandhied, 33);
	}

    @Test
	public void testSandhi34() throws IOException
	{
		String[] unsandhied = new String[]{"Bavan"};
		testSandhi("Bavant", unsandhied, 34);
	}

    @Test
	public void testSandhi35() throws IOException
	{
		String[] unsandhied = new String[]{"Bavan"};
		testSandhi("Bavantkgtrnp", unsandhied, 35);
	}

    @Test
	public void testSandhi36() throws IOException
	{
		String[] unsandhied = new String[]{"Bavat", "janma"};
		testSandhi("Bavaj janma", unsandhied, 36);
	}

    @Test
	public void testSandhi37() throws IOException
	{
		String[] unsandhied = new String[]{"etat", "Danam"};
		testSandhi("etad Danam", unsandhied, 37);
	}

    @Test
	public void testSandhi38() throws IOException
	{
		String[] unsandhied = new String[]{"Bavat", "deham"};
		testSandhi("Bavad deham", unsandhied, 38);
	}

    @Test
	public void testSandhi39() throws IOException
	{
		String[] unsandhied = new String[]{"tat", "Saram"};
		testSandhi("tac Caram", unsandhied, 39);
	}

    @Test
	public void testSandhi40() throws IOException
	{
		String[] unsandhied = new String[]{"pustakam", "paWati"};
		testSandhi("pustakaM paWati", unsandhied, 40);
	}

    @Test
	public void testSandhi41() throws IOException
	{
		String[] unsandhied = new String[]{"vanam", "gacCAmi"};
		testSandhi("vanaM gacCAmi", unsandhied, 41);
	}

    @Test
	public void testSandhi42() throws IOException
	{
		String[] unsandhied = new String[]{"mahAn", "qamaraH"};
		testSandhi("mahAR qamaraH", unsandhied, 42);
	}

    @Test
	public void testSandhi43() throws IOException
	{
		String[] unsandhied = new String[]{"etAn", "cakra"};
		testSandhi("etAMS cakra", unsandhied, 43);
	}

    @Test
	public void testSandhi44() throws IOException
	{
		String[] unsandhied = new String[]{"gacCan", "ca"};
		testSandhi("gacCaMS ca", unsandhied, 44);
	}

    @Test
	public void testSandhi45() throws IOException
	{
		String[] unsandhied = new String[]{"tAn", "tAn"};
		testSandhi("tAMs tAn", unsandhied, 45);
	}

    @Test
	public void testSandhi46() throws IOException
	{
		String[] unsandhied = new String[]{"asmin", "wIkA"};
		testSandhi("asmiMz wIkA", unsandhied, 46);
	}

    @Test
	public void testSandhi47() throws IOException
	{
		String[] unsandhied = new String[]{"tat", "lokaH"};
		testSandhi("tal lokaH", unsandhied, 47);
	}

    @Test
	public void testSandhi48() throws IOException
	{
		String[] unsandhied = new String[]{"tAn", "lokAn"};
		testSandhi("tAM lokAn", unsandhied, 48);
	}

    @Test
	public void testSandhi49() throws IOException
	{
		String[] unsandhied = new String[]{"vAk", "hi"};
		testSandhi("vAg Gi", unsandhied, 49);
	}

    @Test
	public void testSandhi50() throws IOException
	{
		String[] unsandhied = new String[]{"tat", "hi"};
		testSandhi("tad Di", unsandhied, 50);
	}

    @Test
	public void testSandhi51() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "gacCati"};
		testSandhi("rAmo gacCati", unsandhied, 51);
	}

    @Test
	public void testSandhi52() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "asti"};
		testSandhi("rAmo 'sti", unsandhied, 52);
	}

    @Test
	public void testSandhi53() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "karoti"};
		testSandhi("rAmaH karoti", unsandhied, 53);
	}

    @Test
	public void testSandhi54() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "calati"};
		testSandhi("rAmaS calati", unsandhied, 54);
	}

    @Test
	public void testSandhi55() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "wIkAm"};
		testSandhi("rAmaz wIkAm", unsandhied, 55);
	}

    @Test
	public void testSandhi56() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "tu"};
		testSandhi("rAmas tu", unsandhied, 56);
	}

    @Test
	public void testSandhi57() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "patati"};
		testSandhi("rAmaH patati", unsandhied, 57);
	}

    @Test
	public void testSandhi58() throws IOException
	{
		String[] unsandhied = new String[]{"rAmaH", "uvAca"};
		testSandhi("rAma uvAca", unsandhied, 58);
	}

    @Test
	public void testSandhi59() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "vadanti"};
		testSandhi("devA vadanti", unsandhied, 59);
	}

    @Test
	public void testSandhi60() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "eva"};
		testSandhi("devA eva", unsandhied, 60);
	}

    @Test
	public void testSandhi61() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "kurvanti"};
		testSandhi("devAH kurvanti", unsandhied, 61);
	}

    @Test
	public void testSandhi62() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "patanti"};
		testSandhi("devAH patanti", unsandhied, 62);
	}

    @Test
	public void testSandhi63() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "ca"};
		testSandhi("devAS ca", unsandhied, 63);
	}

    @Test
	public void testSandhi64() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "wIkA"};
		testSandhi("devAz wIkA", unsandhied, 64);
	}

    @Test
	public void testSandhi65() throws IOException
	{
		String[] unsandhied = new String[]{"devAH", "tu"};
		testSandhi("devAs tu", unsandhied, 65);
	}

    @Test
	public void testSandhi66() throws IOException
	{
		String[] unsandhied = new String[]{"muniH", "vadati"};
		testSandhi("munir vadati", unsandhied, 66);
	}

    @Test
	public void testSandhi67() throws IOException
	{
		String[] unsandhied = new String[]{"tEH", "uktam"};
		testSandhi("tEr uktam", unsandhied, 67);
	}

    @Test
	public void testSandhi68() throws IOException
	{
		String[] unsandhied = new String[]{"BUH", "Buvas"};
		testSandhi("BUr Buvas", unsandhied, 68);
	}

    @Test
	public void testSandhi69() throws IOException
	{
		String[] unsandhied = new String[]{"muniH", "karoti"};
		testSandhi("muniH karoti", unsandhied, 69);
	}

    @Test
	public void testSandhi70() throws IOException
	{
		String[] unsandhied = new String[]{"agniH", "ca"};
		testSandhi("agniS ca", unsandhied, 70);
	}

    @Test
	public void testSandhi71() throws IOException
	{
		String[] unsandhied = new String[]{"muneH", "wIkAm"};
		testSandhi("munez wIkAm", unsandhied, 71);
	}

    @Test
	public void testSandhi72() throws IOException
	{
		String[] unsandhied = new String[]{"tEH", "tu"};
		testSandhi("tEs tu", unsandhied, 72);
	}

    @Test
	public void testSandhi73() throws IOException
	{
		String[] unsandhied = new String[]{"guruH", "patati"};
		testSandhi("guruH patati", unsandhied, 73);
	}

    @Test
	public void testSandhi74() throws IOException
	{
		String[] unsandhied = new String[]{"punar", "punar"};
		testSandhi("punaH punar", unsandhied, 74);
	}

    @Test
	public void testSandhi75() throws IOException
	{
		String[] unsandhied = new String[]{"punar", "milAmaH"};
		testSandhi("punar milAmaH", unsandhied, 75);
	}

    @Test
	public void testSandhi76() throws IOException
	{
		String[] unsandhied = new String[]{"punar", "ramati"};
		testSandhi("punaH ramati", unsandhied, 76);
	}

    @Test
	public void testSandhi77() throws IOException
	{
		String[] unsandhied = new String[]{"punar", "uvAca"};
		testSandhi("punar uvAca", unsandhied, 77);
	}

    @Test
	public void testSandhi78() throws IOException
	{
		String[] unsandhied = new String[]{"wordi", "aword"};
		testSandhi("wordyaword", unsandhied, 78);
	}

    @Test
	public void testSandhi79() throws IOException
	{
		String[] unsandhied = new String[]{"wordI", "aword"};
		testSandhi("wordyaword", unsandhied, 79);
	}

    @Test
	public void testSandhi80() throws IOException
	{
		String[] unsandhied = new String[]{"wordu", "aword"};
		testSandhi("wordvaword", unsandhied, 80);
	}

    @Test
	public void testSandhi81() throws IOException
	{
		String[] unsandhied = new String[]{"wordU", "aword"};
		testSandhi("wordvaword", unsandhied, 81);
	}

    @Test
	public void testSandhi82() throws IOException
	{
		String[] unsandhied = new String[]{"worde", "aword"};
		testSandhi("worde 'word", unsandhied, 82);
	}

    @Test
	public void testSandhi83() throws IOException
	{
		String[] unsandhied = new String[]{"wordo", "Aword"};
		testSandhi("wordavAword", unsandhied, 83);
	}

    @Test
	public void testSandhi84() throws IOException
	{
	String[] unsandhied = new String[]{"wordaN", "aword"};
	testSandhi("wordaNN aword", unsandhied, 84);
	}

    @Test
	public void testSandhi85() throws IOException
	{
		String[] unsandhied = new String[]{"wordAN", "aword"};
		testSandhi("wordAN aword", unsandhied, 85);
	}

    @Test
	public void testSandhi86() throws IOException
	{
	String[] unsandhied = new String[]{"wordan", "aword"};
	testSandhi("wordann aword", unsandhied, 86);
	}

    @Test
	public void testSandhi87() throws IOException
	{
		String[] unsandhied = new String[]{"wordAn", "aword"};
		testSandhi("wordAn aword", unsandhied, 87);
	}

    @Test
	public void testSandhi88() throws IOException
	{
		String[] unsandhied = new String[]{"wordn", "Sword"};
		testSandhi("wordY Sword", unsandhied, 88);
	}

    @Test
	public void testSandhi89() throws IOException
	{
		String[] unsandhied = new String[]{"wordaH", "aword"};
		testSandhi("wordo 'word", unsandhied, 89);
	}

    @Test
	public void testSandhi90() throws IOException
	{
		String[] unsandhied = new String[]{"wordaH", "oword"};
		testSandhi("worda oword", unsandhied, 90);
	}

    @Test
	public void testSandhi91() throws IOException
	{
		String[] unsandhied = new String[]{"wordiH", "aword"};
		testSandhi("wordir aword", unsandhied, 91);
	}

    @Test
	public void testSandhi92() throws IOException
	{
		String[] unsandhied = new String[]{"wordiH", "rword"};
		testSandhi("wordI rword", unsandhied, 92);
	}

    @Test
	public void testSandhi93() throws IOException
	{
		String[] unsandhied = new String[]{"wordUH", "rword"};
		testSandhi("wordU rword", unsandhied, 93);
	}

    @Test
	public void testSandhi94() throws IOException
	{
		String[] unsandhied = new String[]{"wordaH", "gword"};
		testSandhi("wordo gword", unsandhied, 94);
	}

    @Test
	public void testSandhi95() throws IOException
	{
		String[] unsandhied = new String[]{"wordaH", "cword"};
		testSandhi("wordaS cword", unsandhied, 95);
	}

    @Test
	public void testSandhi96() throws IOException
	{
		String[] unsandhied = new String[]{"wordAH", "gword"};
		testSandhi("wordA gword", unsandhied, 96);
	}

    @Test
	public void testSandhi97() throws IOException
	{
		String[] unsandhied = new String[]{"wordAH", "cword"};
		testSandhi("wordAS cword", unsandhied, 97);
	}

    @Test
	public void testSandhi98() throws IOException
	{
		String[] unsandhied = new String[]{"wordiH", "gword"};
		testSandhi("wordir gword", unsandhied, 98);
	}

    @Test
	public void testSandhi99() throws IOException
	{
		String[] unsandhied = new String[]{"wordiH", "cword"};
		testSandhi("wordiS cword", unsandhied, 99);
	}

    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}

}