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
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for the Sanskrit filters and SylTokenizer.
 */
public class TestSiddham
{
    static SkrtWordTokenizer skrtWordTokenizer = fillWordTokenizer();
    
    static private SkrtWordTokenizer fillWordTokenizer() {
        try {
            skrtWordTokenizer = new SkrtWordTokenizer();
        } catch (Exception e) {
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
    
    static private List<String> generateTokenStream(TokenStream tokenStream) {
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
        List<String> expected = Arrays.asList("loke✓", "'vinASi✓", "vyAlulitena✓", "arTa✓");
        assertThat(produced, is(expected));
    }

    @Test
    public void testAvagrahaSandhi() throws IOException
    {
        System.out.println("non-maximal match 2");
        String input = "loke ’vināśi praṇāme ’py artti";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("lok√", "loka√", "avinASin√", "praRAma√", "api√", "arti√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void testExtraToken() throws IOException
    {
        System.out.println("non-maximal match 2");
        String input = "śrī- loke śāstra anekāny evam sadṛśāny evam vṛtte praṇāme ";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("SrI√", "lok√", "loka√", "IA√", "SAstf√", "SAstra√", "Ana√", 
                "an√", "aneka√", "iva√", "kva√", "evam√", "sadfSa√", "sadfSa√", "evam√", "vftta√", "vftti√", 
                "IraRa√", "ira✓", "praRAma√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug1() throws IOException
    {
        String input = "sphuṭoddhvaṃsita";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("sPuw√", "sPuwa√", "DvaMs√", "ut√"); 
        // order of last two tokens not preserved, yet indices of both tokens are correct. 
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug2ExtraNonword() throws IOException
    {
        System.out.println("non-maximal match 2");
        String input = "praṇāme ";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("praRAma√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug3() throws IOException
    {
        String input = "bhāva";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("BA√", "BA√", "BAva√", "BU√", "Ba√", "Bu√");  // 1st BA is Noun, 2nd is Verb
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug4() throws IOException
    {
        String input = "sabhyeṣūcchvasiteṣu";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("saBya√", "vasita√", "ut√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug6() throws IOException
    {
        String input = "tattvekṣiṇā";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("tattva√", "Ikz√", "ij√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug7() throws IOException
    {
        String input = "pitrābhihito";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        // aBi: Indeclinable, aBi: Preposition
        List<String> expected = Arrays.asList("pitf√", "BI√", "aBi√", "aBi√", "DA√", "hita√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug8() throws IOException
    {
        String input = "pāhy evam";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("pA√", "evam√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug9() throws IOException
    {
        String input = "urvvīm";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("uru√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug10() throws IOException
    {
        String input = "dṛṣṭvā";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("dfz√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug12() throws IOException
    {
        String input = "keciT";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("kim√", "cid√", "cit√", "cit√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug13() throws IOException
    {
        String input = "kecic charaṇam";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("kim√", "cid√", "cit√", "aRa✓", "SaraRa√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug14() throws IOException
    {
        String input = "praṇāme ’py artti";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("praRAma√", "api√", "arti√");
        assertThat(tokens, is(expected));
    }

    @Test
    public void bug15() throws IOException
    {
        String input = "paricārakīkṛta";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("paricAraka√", "fta✓", "ij√", "kf√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug16MissingNonWord() throws IOException
    {
        String input = "nyāyārjane rthasya";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        // non-word "rTa❌" is expected, but because there are matches coming before it from initials,
        // when reaching the idemsandhi that ought to generate this non-word, it is skipped.
        List<String> expected = Arrays.asList("nyAya√", "fj√", "arjana√", "as√", "as√", "asi√", 
                "asi√", "idam√", "i√", "aya✓", "ya✓");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug17MissingToken() throws IOException
    {
        String input = "kavitākīrtti kavitākīrtti atikramati adhikaroti";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("kU√", "kIrti√", "a✓", "kU√", "kIrti√", "a✓", "ati√", "kram√", "aDi√", "kf√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug18MissingToken() throws IOException
    {
        String input = "saṃtataṃ ciraṃ sudarśanaṃ";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("sam√", "tata√", "cira√", "ciram√", "sudarSanaM✓");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug19MissingToken() throws IOException
    {
        String input = "samaye nivāsya pātra-cīvaram";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("samA√", "sama√", "samayA√", "samaya√", "iva√", "nivAsa√", "nivAsin√", 
                "pAtf√", "pAtra√", "cIvara√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug20BAvanInfiniteLoop() throws IOException
    {
        String input = "phalāprāpti-sambhāvanālakṣaṇānarthāvāptiśaṅketi";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        // Huet's reader outputs the following:
        // Pala✓, A✓, prApti✓, samBAvana✓, A✓, lakzaRa✓, an✓, arTa✓, Apti✓, SaNkA✓, iti✓
        List<String> expected = Arrays.asList("Pal√", "Pala√", "prApti√", "a√", "samBAvanAl✓", "akzaR✓", "Ana√", 
                "an√", "av√", "Apti√", "ap√", "SaNkA√", "SaNkA√", "SaNk√", "iti√", "iti√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug21indexOutOfBounds() throws IOException
    {
        // TODO: "M❌" expected ???
        String input = "uṣyaṃ tattva";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("uz√", "vas√", "M❌", "tattva✓");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug22() throws IOException
    {
        String input = "upetaiḥ| ";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("upa√", "i√", "ita√", "tad√", "H❌");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug23() throws IOException
    {
        String input = "ānarthāvāptiśaṅketi";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("Ana√", "an√", "av√", "Apti√", "ap√", "SaNkA√", "SaNkA√", "SaNk√", 
                "iti√", "iti√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug24() throws IOException
    {
        String input = "ā—lakkhoppakābbhāse";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("A√", "lak❌", "Ka√", "p❌", "paka√", "BAs√", "BAsa√", "ap√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug25NullPointer() throws IOException
    {
        String input = "nāgasen-ācyutanandi-";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        // preverb A not recognized because of maxmatch Ac
        List<String> expected = Arrays.asList("nAga√", "n❌", "Ac✓", "yu√", "nanda√", "nandi√", "nandin√");
        assertThat(tokens, is(expected));
    }
    
    @Test
    public void bug26NullPointer() throws IOException
    {
        String input = "ītāṃ ";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        while(words.incrementToken()) {}
    }

    @Test
    public void bug27EmtpyTotalTokens() throws IOException
    {
        String input = "śrr̥ṇotigrāmāntare bhojanamastīti, ";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        while(words.incrementToken()) {}
    }
    
    @AfterClass
    public static void finish() {
        System.out.println("after the test sequence");
        System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
    }
}