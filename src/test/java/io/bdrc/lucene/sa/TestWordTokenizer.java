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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Trie;

/**
 * Unit tests for the Sanskrit tokenizers and filters.
 */
public class TestWordTokenizer
{
	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}
	
	static private SkrtWordTokenizer buildTokenizer(String trieName) throws FileNotFoundException, IOException {
		Trie trie = BuildCompiledTrie.buildTrie(trieName + ".txt");

		return new SkrtWordTokenizer(true, trie);
	}
	
	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
            PartOfSpeechAttribute posAttribute= tokenStream.addAttribute(PartOfSpeechAttribute.class);
			while (tokenStream.incrementToken()) {
                if (typeAttribute.type().equals("non-word")) {
                    termList.add(charTermAttribute.toString()+"❌");
                } else if (typeAttribute.type().equals("word")) {
                    termList.add(charTermAttribute.toString()+"✓");
                } else if (typeAttribute.type().equals("lemma")) {
                    termList.add(charTermAttribute.toString()+"√");
                }
                System.out.println(charTermAttribute.toString() + ", tokenType: " + typeAttribute.type()+ ", POS: " + posAttribute.getPartOfSpeech());
			}
			System.out.println("1 " + String.join(" ", expected));
			System.out.println("2 " + String.join(" ", termList) + "\n");
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}
	
	static private String repeatChar(char c, int times) {
		 char[] array = new char[times]; 
		 Arrays.fill(array, c);
		 return String.valueOf(array);
	}

	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}

    @Test
    public void testCmdParser() throws IOException
    {
    	System.out.println("CmdParser: parse cmd of Darma");
    	String input = "$-0+n/=0£9#1|$/=0£9#1|r$-0+n;-0+/- r+f=1£1#1|cC$-0+n;-0+/- cC+C=8£4#1|A:i:I:u:U:f:e:E:o:O$-0+n;-0+/- +=1£1#1";
    	String expected = "{acC=[0+/C=8£4#1, 0+n/C=8£4#1], aA=[0+/A=1£1#1, 0+n/A=1£1#1], aE=[0+/E=1£1#1, "
    	        + "0+n/E=1£1#1], aI=[0+/I=1£1#1, 0+n/I=1£1#1], aO=[0+/O=1£1#1, 0+n/O=1£1#1], aU=[0+/U=1£1#1, "
    	        + "0+n/U=1£1#1], ae=[0+/e=1£1#1, 0+n/e=1£1#1], af=[0+/f=1£1#1, 0+n/f=1£1#1], ai=[0+/i=1£1#1, "
    	        + "0+n/i=1£1#1], ao=[0+/o=1£1#1, 0+n/o=1£1#1], ar=[0+/f=1£1#1, 0+n/f=1£1#1], au=[0+/u=1£1#1, "
    	        + "0+n/u=1£1#1], a=[0+/=0£0#1, 0+n/=0£9#1]}";
    	System.out.println("0 " + input);
    	Map<String, TreeSet<CmdParser.DiffStruct>> res = new CmdParser().parse("Darma", input);    	
    	System.out.println("1 " + expected);
    	System.out.println("2 " + res.toString() + "\n");
    	assertTrue(res.toString().equals(expected));
    }
    
    @Test
    public void testContainsSandhiedCombination1NoSandhi() throws IOException
    {
    	System.out.println("containsSandhiedCombination() 1: no sandhi");
    	RollingCharBuffer buffer = new RollingCharBuffer();
    	buffer.reset(new StringReader("budDaDarma"));
    	buffer.get(0);
    	int bufferIdx = 4;
    	String sandhied = "a";
    	boolean res = SkrtWordTokenizer.containsSandhiedCombination(buffer, bufferIdx, sandhied, 0); 
    	assertTrue(res);
    }

    @Test
    public void testContainsSandhiedCombination2VowelSandhi() throws IOException
    {
    	System.out.println("containsSandhiedCombination() 2: vowel sandhi");
    	RollingCharBuffer buffer = new RollingCharBuffer();
    	buffer.reset(new StringReader("DarmATa"));
    	buffer.get(0);
    	int bufferIdx = 4;
    	String sandhied = "A";
    	boolean res = SkrtWordTokenizer.containsSandhiedCombination(buffer, bufferIdx, sandhied, 2);
    	assertTrue(res);
    }

    @Test
    public void testContainsSandhiedCombinationAbsoluteFinals() throws IOException
    {
    	System.out.println("containsSandhiedCombination() 3: absolute finals");
    	RollingCharBuffer buffer = new RollingCharBuffer();
    	buffer.reset(new StringReader("Darmaprsti"));
    	buffer.get(0);
    	int bufferIdx = 5;
    	String sandhied = "ap";
    	boolean res = SkrtWordTokenizer.containsSandhiedCombination(buffer, bufferIdx, sandhied, 5); 
    	assertTrue(res);
    }
    
    @Test
	public void SandhiedCompoundTest() throws IOException
	{
		System.out.println("sandhied compounds");
		String input = "DarmATa DarmADa DarmATa";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("Darma√", "Darman√", "aTa√", "Darma√", "Darman√", "Da❌", 
		        "Darma√", "Darman√", "aTa√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/DarmATa_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}

    @Test
	public void testStartingWithNonword() throws IOException
	{
		System.out.println("Testing input starting with non-word");
		String input = "aTaAB CDEaTaFGH IJaTa";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa√", "AB❌", "CDE❌", "aTa√", "FGH❌", "IJ❌", "aTa√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/DarmATa_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatch1() throws IOException
	{
		System.out.println("non-maximal match 1");
		String input = "eded";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ed√", "ed√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/abab_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}

    @Test
	public void testNonMaximalMatch2() throws IOException
	{
		System.out.println("non-maximal match 2");
		String input = "abab";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aba√", "b❌");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/abab_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatch3FollowedWithNonWord() throws IOException
	{
		System.out.println("non-maximal match 3: followed by a non-word");
		String input = "ababa";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aba√", "ba❌");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/abab_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatch4FollowedWithNonWord() throws IOException
	{
		System.out.println("non-maximal match 4: followed by a non-word");
		String input = "edede";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ed√", "ed√", "e❌");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/abab_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonMaximalMatch5PrecededWithNonWord() throws IOException
	{
		System.out.println("non-maximal match 5: preceded by a non-word");
		String input = "auieabab";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("auie❌", "aba√", "b❌");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/abab_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testSpaceInSandhi() throws IOException
	{
		System.out.println("space in sandhi");
		String input = "te 'pi te'pi";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("tad√", "yuzmad√", "api√", "tad√", "yuzmad√", "api√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/te'pi_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testNonSandhiedCompound() throws IOException
	{
		System.out.println("non-sandhied compound");
		String input = "budDaDarma";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("budDa✓", "Darma√", "Darman√");
		System.out.println("0 " + input);
    	SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/budDaDarma_test");
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
	}
    
    @Test
	public void testDeletingInitialsOnEndOfInput() throws IOException
	{
		System.out.println("deleting initials on end of input");
		String input = "kanyA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("kana√", "kanyA√", "kanya√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testLongInput() throws IOException
	{
		System.out.println("long input\nSanskritHeritge outputs:\n\taTa rAja kanyA candravatI "
		        + "nAmABinavarupayOvanasampannA saKI dvitIyA ekasmin maha utsava divase na garaM "
		        + "nirikzamARAsti\n'nAmA is split here because we lemmatize and 'na' exists in the Trie"); 
		String input = "aTa rAjakanyA candravatI nAmABinavarupayOvanasampannA saKIdvitIyEkasminmahotsava"
		        + "divase nagaraM nirikzamARAsti";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa√", "kana√", "kanyA√", "kanya√", "rAj√", "rAjan√", 
		        "kcandravatI❌", "nAmABi❌", "na√", "varupayOva❌", "na√", "sampannA❌", "saKi√", "dvitIya√", 
		        "eka√", "mah√", "mahat√", "utsava√", "divasa√", "na√", "garaM❌", "nirikzamARAsti❌");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
    public void testIndexBug() throws IOException
    { 
        String input = "varupayOvanasampannA ";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("varupayOva❌", "na√", "sampannA❌");
        System.out.println("0 " + input);
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
        TokenStream syllables = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(syllables, expected);
    }
    
    @Test
	public void testNonSLPOnly() throws IOException
	{
		System.out.println("non-SLP only");
		String input = "«»(**-éàÀ%$–@)";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList();
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testSLPModifiers() throws IOException
	{
		System.out.println("SLP modifiers");
		String input = "a+Ta/8 rA+ja^1ka\\nyA^97";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa√", "rAja✓", "kanyA√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void testMixedSLPNonSLPString() throws IOException
	{
		System.out.println("mixed SLP non-SLP");
		String input = "«»(**-éàÀ%$–@)aTa rAjakanyA«»(**- éàÀ%$–@)aTa rAjakanyA «»(**- éàÀ%$–@)";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa√", "kana√", "kanyA√", "kanya√", "rAj√", "rAjan√", 
		        "aTa√", "kana√", "kanyA√", "kanya√", "rAj√", "rAjan√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
    public void testMultiTokenLemmaSplit() throws Exception
    {
        System.out.println("splitting multi-token lemmas");
        String input = "guRita-guRAjYAhatAn eva";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("guRita√", "guRa√", "AjYA√", "A√", "han√", "eva√");
        System.out.println("0 " + input);
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(true);
        TokenStream syllables = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(syllables, expected);
    }
    
    @Test
	public void bug1InWordTokenizer() throws IOException
	{
		System.out.println("bug1");
		String input = "aTa rAjakanyA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("aTa√", "kana√", "kanyA√", "kanya√", "rAj√", "rAjan√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void bug2ExtraTokenWithInitials() throws IOException
	{
		System.out.println("bug2");
		String input = "kanyA candravatI";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("kana√", "kanyA√", "kanya√", "candravatI❌");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void bug3ExtraToken() throws IOException
	{
		System.out.println("bug3");
		String input = "divase na";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("divasa√", "na√");
		System.out.println("0 " + input);
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
	public void bug4MissingToken() throws IOException
	{
		System.out.println("bug4");
		String input = "saKIdvitIyEkasmin";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("saKi√", "dvitIya√", "eka√");
		System.out.println("0 " + input);
    	SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}

    @Test
	public void bug5NonSLP() throws IOException
	{
		System.out.println("bug5");
		String input = "ka%nyA";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("ka❌", "nyA❌");
		System.out.println("0 " + input);
    	SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/aTa_test");
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
	}
    
    @Test
    public void bug6IoBufferSizeLimit() throws IOException
    {
    	System.out.println("bug6");
    	List<String> expected = Arrays.asList("budDa✓", "Darma√", "Darman√");
    	SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/budDaDarma_test");
    	
    	HashMap<Integer, Integer> ranges = new HashMap<Integer, Integer>();
    	ranges.put(2030, 2049);
    	ranges.put(4080, 4097);
    
    	for (HashMap.Entry<Integer, Integer> entry : ranges.entrySet()) {
    		for (int i=entry.getKey() ; i<entry.getValue(); i++) {
    			String input = "budDaDarma";
    			String filling = repeatChar('.', i);
    			Reader reader = new StringReader(filling+input);
    			System.out.println("0 " + String.valueOf(i) + " dots + " + input);
    			TokenStream syllables = tokenize(reader, skrtWordTokenizer);
    			assertTokenStream(syllables, expected);
    		}			
    	}
    }
    
    @Test
    public void bug7LoneInitialsAfterPunctuation() throws IOException
    {
    	System.out.println("bug7");
    	String input = "mAdivyApArarahitaM";
    	Reader reader = new StringReader(input);
    	List<String> expected = Arrays.asList("mAdin√", "vyApAra√", "ahi√", "taM❌");
    	System.out.println("0 " + input);
    	
    	SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/vyApArarahi_test");
    	TokenStream syllables = tokenize(reader, skrtWordTokenizer);
    	assertTokenStream(syllables, expected);
    }
    
    @Test
    public void bug8MatchingLoneInitials() throws IOException
    {
		System.out.println("bug8");
		String input = "praTamo BAgaH";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("praTama√", "BAga√");
		System.out.println("0 " + input);
		
		SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/pratamo_test"); 
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(words, expected);
    }
    
    @Test
    public void bug9NotRevertingToNonwordsIfNoMatch() throws IOException
    {
        System.out.println("bug9");
        String input = "kecit";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("kim√", "cid√", "cit√", "cit√");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/kecit_test"); 
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(words, expected);
    }
    
    @SuppressWarnings("resource")
    @Test
    public void testDemoWords() throws IOException
    {
        System.out.println("bug9");
        String input = "sattvasya paramārtha nāma"  
                + "bodhicaryāvatara bodhisattvacaryāvatara - "
                + "Śāntideva - "
                + "mañjuśrī nāma saṃgīti - "
                + "mañjuśrījñānasattvasya paramārtha nāma saṃgīti - "
                + "Nāmasaṃgīti - "
                + "bodhicaryāvatara";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("sattva√", "parama√", "ārtha√", "artha√", "nāman√", "bodhi√", 
                "bodhin√", "caryā√", "avatara√", "bodhisattva√", "caryā√", "avatara√", "śāntideva√", 
                "mañjuśrī√", "nāman√", "samgīti√", "mañjuśrī√", "jñāna√", "ij√", "sattva√", "parama√", 
                "ārtha√", "artha√", "nāman√", "samgīti√", "nāman√", "samgīti√", "bodhi√", "bodhin√", 
                "caryā√", "avatara√");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/demo_test"); 
        reader = new Roman2SlpFilter(reader);
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        assertTokenStream(words, expected);
    }

    @Test
    public void testZeroSandhi() throws IOException
    {
        System.out.println("bug9");
        String input = "SrIjYAna";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("SrI√", "YAna√", "ij√", "jYAna√");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/shri_jnana_test"); 
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(words, expected);
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}
