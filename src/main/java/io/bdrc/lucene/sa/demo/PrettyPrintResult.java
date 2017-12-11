package io.bdrc.lucene.sa.demo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

//import io.bdrc.lucene.sa.Deva2SlpFilter;
import io.bdrc.lucene.sa.Roman2SlpFilter;
import io.bdrc.lucene.sa.SkrtWordTokenizer;

public class PrettyPrintResult {
    
    static OutputStreamWriter writer;
    
    public static void main(String[] args) throws FileNotFoundException, IOException{
        int tokensOnLine = 20;
        List<String> inputFiles = Arrays.asList(
//                "src/test/resources/tattvasangrahapanjika_raw_deva.txt"
                "src/test/resources/Siddham-Edition Export tester.txt"
                );
        
        System.out.println("Loading the Trie...");
        long loading = System.currentTimeMillis();
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer();
        long loaded = System.currentTimeMillis();
        System.out.println("Time: " + (loaded - loading) / 1000 + "s.\n");
        
        for (String fileName: inputFiles) {
            String inputStr = new String(Files.readAllBytes(Paths.get(fileName))); 
            String outFileName = "./" + fileName.substring(fileName.lastIndexOf('/'), fileName.lastIndexOf('.')) + "_lemmatized.txt";
            writer = new OutputStreamWriter(new FileOutputStream(outFileName), StandardCharsets.UTF_8);
            System.out.println("Processing " + fileName + "...");
            Reader input = new StringReader(inputStr);
            CharFilter cs = new Roman2SlpFilter(input);
            TokenStream words = tokenize(cs, skrtWordTokenizer);
            long tokenizing = System.currentTimeMillis();
            produceTokens(words, inputStr, tokensOnLine);
            long tokenized = System.currentTimeMillis();
            System.out.println("Time: " + (tokenized - tokenizing) / 1000 + "s.");
            writer.flush();
            writer.close();
        }
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }

    private static void produceTokens(TokenStream tokenStream, String inputStr, int tokensOnLine) { 
        int batchNum = 1;
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
                    generateLines(batchStartOffset, batchEndOffset, tokensLine.toString(), inputStr, batchNum); 
                    tokNo = 0; 
                    tokensLine = new StringBuilder(); 
                    batchStartOffset = batchEndOffset; 
                    batchNum ++;
                } 
            }
            if (tokNo != 0) { 
                generateLines(batchStartOffset, batchEndOffset, tokensLine.toString(), inputStr, batchNum);
                batchNum ++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    private static void generateLines(int batchStartOffset, int batchEndOffset, String tokensLine, String inputStr, int batchNum) throws IOException { 
        writer.append(batchNum + "\n");
        writer.append(inputStr.substring(batchStartOffset, batchEndOffset)+"\n"); 
        writer.append(tokensLine+"\n"); 
    }
}
