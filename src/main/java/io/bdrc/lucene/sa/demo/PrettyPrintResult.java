package io.bdrc.lucene.sa.demo;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import io.bdrc.lucene.sa.Deva2SlpFilter;
import io.bdrc.lucene.sa.SkrtWordTokenizer;

public class PrettyPrintResult {
    
    public static void main(String [] args) throws FileNotFoundException, IOException{
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer();
        String inputStr = new String(Files.readAllBytes(Paths.get("src/test/resources/tattvasangrahapanjika_raw_deva.txt")));
        Reader input = new StringReader(inputStr);
        CharFilter cs = new Deva2SlpFilter(input);
        TokenStream words = tokenize(cs, skrtWordTokenizer);
        printProducedTokens(words, inputStr);
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private void printProducedTokens(TokenStream tokenStream, String inputStr) {
        try {
            HashMap<String, int[]> termList = new HashMap<String, int[]>();
            CharTermAttribute TermAttr = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttr = tokenStream.addAttribute(TypeAttribute.class);
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
            while (tokenStream.incrementToken()) {
                String token = TermAttr.toString();
                int[] offsets = new int[]{offsetAttr.startOffset(), offsetAttr.endOffset()};
                char type = typeAttr.type().equals("word") ? '✓': '❌';
                termList.put(token+type, offsets);
                
                System.out.println(TermAttr.toString() + type + offsets[0] + '/' + offsets[1]);
            }
        } catch (IOException e) {
            assertTrue(false);
        }
    }    
}
