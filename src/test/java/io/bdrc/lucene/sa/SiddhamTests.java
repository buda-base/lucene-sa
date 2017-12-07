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

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.StopFilter;
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

        return new SkrtWordTokenizer(true, trie);
    }
    
    static private List<String> generateTokenStream(TokenStream tokenStream) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
            }
            System.out.println("2 " + String.join(" ", termList) + "\n");
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
        String input = "loke ’vināśi"; // normalizations and deletions 
        CharFilter cs = new Roman2SlpFilter(new StringReader(input));
        System.out.println("0 " + input);
        TokenStream ts = tokenize(cs, new WhitespaceTokenizer());
        List<String> produced = generateTokenStream(ts);
        List<String> expected = Arrays.asList("loke", "'vinASi");
        assertThat(produced, is(expected));
    }
    
    @Test
    public void testExtraTokenMystery() throws IOException
    {
        System.out.println("non-maximal match 2");
        String input = "śrī- loke ’vināśi śāstra anekāny sadṛśāny vṛtte praṇāme ";
        System.out.println("0 " + input);
//        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/SAstra_test");
        TokenStream words = tokenize(new Roman2SlpFilter(new StringReader(input)), skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("SAstf", "SAstf");
        assertThat(tokens, is(expected));
    }
    
    @AfterClass
    public static void finish() {    // vowel sandhi
        System.out.println("after the test sequence");
        System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
    }
}