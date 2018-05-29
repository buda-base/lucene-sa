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
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Trie;

public class TestFilters {
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

        return new SkrtWordTokenizer(true, trie, true);
    }
    
    static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
        try {
            List<String> termList = new ArrayList<String>();
            PartOfSpeechAttribute posAttribute= tokenStream.addAttribute(PartOfSpeechAttribute.class);
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
            while (tokenStream.incrementToken()) {
                System.out.println(charTermAttribute.toString() + ", tokenType: " + typeAttribute.type()+ ", POS: " + posAttribute.getPartOfSpeech());
                if (typeAttribute.type().equals("non-word")) {
                    termList.add(charTermAttribute.toString()+"❌");
                } else if (typeAttribute.type().equals("word")) {
                    termList.add(charTermAttribute.toString()+"✓");
                } else if (typeAttribute.type().equals("lemma")) {
                    termList.add(charTermAttribute.toString()+"√");
                } 
            }
            System.out.println("1 " + String.join(" ", expected));
            System.out.println("2 " + String.join(" ", termList) + "\n");
            assertThat(termList, is(expected));
        } catch (IOException e) {
            assertTrue(false);
        }
    }
    
    @Test
    public void testPrepositionMergingTokenFilter() throws IOException
    {
        System.out.println("test merging TokenFilter");
        String input = "atikramati";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("atikram√");  // test passes with "ati√", "kram√"
        System.out.println("0 " + input);
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/atikramati_test");
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        TokenStream merged = new PrepositionMergingFilter(words);
        assertTokenStream(merged, expected);
    }
}
