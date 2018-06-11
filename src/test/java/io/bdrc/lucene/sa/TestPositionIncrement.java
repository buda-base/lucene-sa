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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Trie;

public class TestPositionIncrement {

    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private SkrtWordTokenizer buildTokenizer(String trieName) throws FileNotFoundException, IOException {
        Trie trie = BuildCompiledTrie.buildTrie(trieName + ".txt");

        return new SkrtWordTokenizer(trie);
    }
    
    static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute incrAttribute = tokenStream.addAttribute(PositionIncrementAttribute.class); 
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString() + "_" +  incrAttribute.getPositionIncrement());
                }
            System.out.println("1 " + String.join(" ", expected));
            System.out.println("2 " + String.join(" ", termList) + "\n");
            assertThat(termList, is(expected));
        } catch (IOException e) {
            assertTrue(false);
        }
    }
    
    @Test
    public void testIncrement() throws IOException
    {
        System.out.println("Increment Position");
        String input = "Darma boDi loke loke";  // only loka for last token as only absolute final sandhi is applicable at end of input
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("Darma_1", "Darman_0", "boDi_1", "boDin_0", "loka_1", "loke_0", "loka_1");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/increment_position_test"); 
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(words, expected);
    }
    
}
