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
        HashMap<String, int[]> terms = produceTerms(words);
        String[] lines = createLines(terms, 10, inputStr);
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private String[] createLines(HashMap<String, int[]> terms, int termsPerLine, String inputStr) {
        HashMap<String, HashMap<String, Integer>> lines = new HashMap<String, HashMap<String, Integer>>(); 
        
        int i = 0;
        int batchSize = termsPerLine;
        while (i <= terms.size()) {
            if (i + batchSize > terms.size()) {
                batchSize = terms.size() - i;
            }
            
            int lineStart = 0;
            int lineEnd = 0;
            HashMap<String, Integer> batch = new HashMap<String, Integer>(); 
            for (int t = i; t < i+batchSize; t++) {
                
            }
            i += batchSize;
        }
        return null;
    }
    
    static private HashMap<String, int[]> produceTerms(TokenStream tokenStream) throws IOException {
            HashMap<String, int[]> terms = new HashMap<String, int[]>();
            CharTermAttribute TermAttr = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttr = tokenStream.addAttribute(TypeAttribute.class);
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
            while (tokenStream.incrementToken()) {
                String token = TermAttr.toString();
                int type = typeAttr.type().equals("word") ? 1: 0;//'✓': '❌';
                int[] metadata = new int[]{type, offsetAttr.startOffset(), offsetAttr.endOffset()};
                
                terms.put(token, metadata);
            }
            return terms;
    }
}
