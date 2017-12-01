package io.bdrc.lucene.sa.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import io.bdrc.lucene.sa.Deva2SlpFilter;
import io.bdrc.lucene.sa.SkrtWordTokenizer;

public class PrettyPrintResult {
    
    static PrintWriter writer;
    
    public static void main(String [] args) throws FileNotFoundException, IOException{
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer();
        String inputStr = new String(Files.readAllBytes(Paths.get("src/test/resources/tattvasangrahapanjika_raw_deva.txt")));
        writer = new PrintWriter(new FileOutputStream("./demo.txt", true));
        
        Reader input = new StringReader(inputStr);
        CharFilter cs = new Deva2SlpFilter(input);
        TokenStream words = tokenize(cs, skrtWordTokenizer);
        int tokensOnLine = 20;
        produceTokens(words, inputStr, tokensOnLine);
        writer.flush();
        writer.close();
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }

    private static void produceTokens(TokenStream tokenStream, String inputStr, int tokensOnLine) { 
        try {
            CharTermAttribute TermAttr = tokenStream.addAttribute(CharTermAttribute.class); 
            TypeAttribute typeAttr = tokenStream.addAttribute(TypeAttribute.class); 
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class); 
            int tokNo = 0; 
            int batchStartOffset = 0; 
            int batchEndOffset = 0; 
            StringBuilder tokensLine = new StringBuilder();
            while (tokenStream.incrementToken()) { 
                tokNo ++; 
                String token = TermAttr.toString(); 
                token += typeAttr.type().equals("word") ? '✓': '❌'; 
                batchEndOffset = offsetAttr.endOffset(); 
                if (tokNo != 1) tokensLine.append(' '); 
                tokensLine.append(token); 
                if (tokNo >= tokensOnLine) { 
                    generateLines(batchStartOffset, batchEndOffset, tokensLine.toString(), inputStr); 
                    tokNo = 0; 
                    tokensLine = new StringBuilder(); 
                    batchStartOffset = batchEndOffset; 
                } 
            }
            if (tokNo != 0) { 
                generateLines(batchStartOffset, batchEndOffset, tokensLine.toString(), inputStr); 
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    private static void generateLines(int batchStartOffset, int batchEndOffset, String tokensLine, String inputStr) throws IOException { 
//        writer.println(inputStr.substring(batchStartOffset, batchEndOffset)); 
        writer.println(tokensLine+"\n"); 
    }
}
