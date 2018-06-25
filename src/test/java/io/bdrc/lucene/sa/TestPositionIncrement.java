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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Test;

public class TestPositionIncrement {

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
        String input = "SrIjYAna Darma boDi loke loke";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("SrI_1", "IjY_1", "Ana_0", "an_0", "jYAna_0", "Darma_1", "Darman_0", 
                "boDi_1", "boDin_0", "lok_1", "loka_0", "oka_1", "lok_0", "loka_0");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(); 
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(words, expected);
    }
}
