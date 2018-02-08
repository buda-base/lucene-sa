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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import io.bdrc.lucene.sa.Deva2SlpFilter;
import io.bdrc.lucene.sa.GeminateNormalizingFilter;
import io.bdrc.lucene.sa.Roman2SlpFilter;
import io.bdrc.lucene.sa.SiddhamFilter;
import io.bdrc.lucene.sa.SkrtWordTokenizer;
import com.sktutilities.transliteration.SLPToIAST;
import com.sktutilities.transliteration.DvnToSLP;

public class PrettyPrintResult {
    
    static OutputStreamWriter writer;
    static SLPToIAST slp2iast = new SLPToIAST();
    static DvnToSLP deva2slp = new DvnToSLP();
    
    public static void main(String[] args) throws FileNotFoundException, IOException{
        int tokensOnLine = 20;
        LinkedHashMap<String, Integer> inputFiles = new LinkedHashMap<String, Integer>(); 
//        inputFiles.put("src/test/resources/Siddham-Edition Export tester.txt", 0);
        inputFiles.put("src/test/resources/Siddham-Edition Export tester_beginning.txt", 0);
//        inputFiles.put("src/test/resources/tattvasangrahapanjika_raw_deva.txt", 1);
        
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(true);
        
        Set<String> keys = inputFiles.keySet();
        for (String fileName: keys) {
            String inputStr = new String(Files.readAllBytes(Paths.get(fileName))); 
            // ignore any BOM marker on first line
            if (inputStr.startsWith("\uFEFF")) {
                inputStr = inputStr.substring(1);
            }
            String outFileName = "./" + fileName.substring(fileName.lastIndexOf('/'), fileName.lastIndexOf('.')) + "_lemmatized.txt";
            writer = new OutputStreamWriter(new FileOutputStream(outFileName), StandardCharsets.UTF_8);
            System.out.println("Processing " + fileName + "...");
            Reader input = new StringReader(inputStr);
            CharFilter cs;
            if (inputFiles.get(fileName) == 0) {
                cs = new Roman2SlpFilter(input);
            } else {
                cs = new Deva2SlpFilter(input);
            }
            cs = new SiddhamFilter(cs);
            cs = new GeminateNormalizingFilter(cs);
            TokenStream words = tokenize(cs, skrtWordTokenizer);
            long tokenizing = System.currentTimeMillis();
            produceTokens(words, inputStr, tokensOnLine, inputFiles.get(fileName));
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

    private static void produceTokens(TokenStream tokenStream, String inputStr, int tokensOnLine, Integer encoding) { 
        int batchNum = 1;
        try {
            CharTermAttribute TermAttr = tokenStream.addAttribute(CharTermAttribute.class); 
            TypeAttribute typeAttr = tokenStream.addAttribute(TypeAttribute.class); 
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class); 
            int tokNo = 0; 
            int batchStartOffset = 0; 
            int batchEndOffset = 0; 
            
            int tmp = -1;
            HashMap<Integer, Boolean> wordsAndNonwords = new HashMap<Integer, Boolean>();
            int totalWords = 0;
            int totalNonwords = 0;
            int totalTokens = 0;
            
            StringBuilder tokensLine = new StringBuilder();
            while (tokenStream.incrementToken()) { 
                tokNo ++; 
                String token = TermAttr.toString(); 
                token += typeAttr.type().equals("word") ? '✓': '❌'; 
                batchEndOffset = offsetAttr.endOffset(); 
                wordsAndNonwords.putIfAbsent(batchEndOffset, true);
                if (token.contains("❌")) {
                    wordsAndNonwords.replace(batchEndOffset, false);
                }
                if (tokNo != 1) tokensLine.append(' '); 
                if (tmp == -1 || tmp != batchEndOffset) {
                    tmp = batchEndOffset;
                    tokensLine.append("¦ ");
                }
                tokensLine.append(token);
                totalTokens ++;
                if (tokNo >= tokensOnLine) { 
                    writeLines(batchStartOffset, batchEndOffset, tokensLine.toString(), inputStr, batchNum, encoding); 
                    tokNo = 0; 
                    tokensLine = new StringBuilder(); 
                    batchStartOffset = batchEndOffset; 
                    batchNum ++;
                } 
            }
            if (tokNo != 0) { 
                writeLines(batchStartOffset, batchEndOffset, tokensLine.toString(), inputStr, batchNum, encoding);
                batchNum ++;
            }
            
            for (Entry<Integer, Boolean> entry: wordsAndNonwords.entrySet()) {
                boolean value = entry.getValue();
                if (value) {
                    totalWords ++;
                } else {
                    totalNonwords ++;
                }
            }
            System.out.println("Counting the inflected forms in the input that were processed,");
            System.out.println("Total inflected forms: " + wordsAndNonwords.size() + " (yielding " + totalTokens + " tokens)");
            System.out.println("Forms yielding only words: " + totalWords + " (" + totalWords * 100 / wordsAndNonwords.size() + "%)");
            System.out.println("Forms yielding at least one nonword: " + totalNonwords + " (" + totalNonwords * 100 / wordsAndNonwords.size() + "%)");
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
    
    private static void writeLines(int batchStartOffset, int batchEndOffset, String tokensLine, String inputStr, int batchNum, Integer encoding) throws IOException { 
        writer.append(batchNum + "\n");
        String substring = inputStr.substring(batchStartOffset, batchEndOffset);
        String tokens = slp2iast.transform(tokensLine); 
        
        writer.append(substring+"\n"); 
        writer.append(tokens+"\n");         
    }
}
