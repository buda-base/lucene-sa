package io.bdrc.lucene.sa.demo;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import io.bdrc.lucene.sa.Deva2SlpFilter;
import io.bdrc.lucene.sa.SkrtWordTokenizer;

public class PrettyPrintResult {
    
    public static void main(String [] args) throws FileNotFoundException, IOException{
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer();
        Reader input = new FileReader("src/test/resources/tattvasangrahapanjika_raw_deva.txt");  
        CharFilter cs = new Deva2SlpFilter(input);
        TokenStream words = tokenize(cs, skrtWordTokenizer);
        printProducedTokens(words);
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private void printProducedTokens(TokenStream tokenStream) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
                char type = typeAttribute.type().equals("word") ? '✓': '❌'; 
                System.out.println(charTermAttribute.toString() + type);
            }
        } catch (IOException e) {
            assertTrue(false);
        }
    }    
}
