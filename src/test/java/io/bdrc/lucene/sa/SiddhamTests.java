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
import java.util.List;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Trie;

/**
 * Unit tests for the Sanskrit filters and SylTokenizer.
 */
public class SiddhamTests
{
    static SkrtWordTokenizer skrtWordTokenizer = fillWordTokenizer();
    
    static private SkrtWordTokenizer fillWordTokenizer() {
        try {
            skrtWordTokenizer = new SkrtWordTokenizer(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return skrtWordTokenizer;
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private SkrtWordTokenizer buildTokenizer(String trieName) throws FileNotFoundException, IOException {        
        List<String> inputFiles = Arrays.asList(trieName + ".txt");
        
        Trie trie = BuildCompiledTrie.buildTrie(inputFiles);

        return new SkrtWordTokenizer(trie);
    }
    
    static private List<String> generateTokenStream(TokenStream tokenStream) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
            }
            System.out.println("1 " + String.join(" ", termList) + "\n");
            return termList;
        } catch (IOException e) {
            assertTrue(false);
        }
        return null;
    }

    @BeforeClass
    public static void init() {
        System.out.println("before the test sequence");
    }
 
    @Test
    public void testAvagrahaNormalization() throws Exception {
        System.out.println("Avagraha normalization test");
        String input = "loke ’vināśi vyāluḷitena arttha"; // normalizations and deletions 
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        System.out.println("0 " + input);
        TokenStream ts = tokenize(geminates, new WhitespaceTokenizer());
        List<String> produced = generateTokenStream(ts);
        List<String> expected = Arrays.asList("loke", "'vinASi", "vyAlulitena", "arTa");
        assertThat(produced, is(expected));
    }

    @Test
    public void testAvagrahaSandhi() throws IOException
    {
        System.out.println("non-maximal match 2");
        String input = "loke ’vināśi praṇāme ’py artti";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/avagraha_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("lok", "loka", "avinASin", "praRAma", "api", "arti");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void testExtraToken() throws IOException
    {
        System.out.println("non-maximal match 2");
        String input = "śrī- loke śāstra anekāny evam sadṛśāny evam vṛtte praṇāme ";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("SrI", "lok", "loka", "SAstf", "aneka", "evam", "sadfSa", "evam", "vftti", "vftta", "praRAma");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug1() throws IOException
    {
        String input = "sphuṭoddhvaṃsita";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("sPuwa", "sPuw", "udDvaMs");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug3() throws IOException
    {
        String input = "bhāva";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("BU", "Bu", "Ba", "BA");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug4() throws IOException
    {
        String input = "sabhyeṣūcchvasiteṣu";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("saBya", "ut_Svas");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug6() throws IOException
    {
        String input = "tattvekṣiṇā";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("tattva", "ikzin");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug7() throws IOException
    {
        String input = "pitrābhihito";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("pitf", "aBi_DA");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug8() throws IOException
    {
        String input = "pāhy evam";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("pA", "pAhi", "evam");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug9() throws IOException
    {
        String input = "urvvīm";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("urvI", "uru");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug10() throws IOException
    {
        String input = "dṛṣṭvā";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("dfz");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug12() throws IOException
    {
        String input = "keciT";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("kim", "cid");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug13() throws IOException
    {
        String input = "kecic charaṇam";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("kim", "cit", "cid", "SaraRa");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug14() throws IOException
    {
        String input = "praṇāme ’py artti";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("praRAma", "api", "arti");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug15() throws IOException
    {
        String input = "paricārakīkṛta";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("paricAraka", "ij", "fta");   // ought to be kfta, maxmatch issue
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug16MissingNonWord() throws IOException
    {
        String input = "nyāyārjane rthasya";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("nyAya", "arjana", "rTa", "sA");
        assertThat(tokens, is(expected));
    }
    
    @AfterClass
    public static void finish() {    // vowel sandhi
        System.out.println("after the test sequence");
        System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
    }
}