package io.bdrc.lucene.sa.demo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import io.bdrc.lucene.sa.Slp2RomanFilter;
import io.bdrc.lucene.sa.SiddhamFilter;
import io.bdrc.lucene.sa.SkrtWordTokenizer;

public class PrettyPrintResult {
    
    static OutputStreamWriter writer;
    
    public static void main(String[] args) throws Exception{
        int tokensOnLine = 20;
        LinkedHashMap<String, Integer> inputFiles = new LinkedHashMap<String, Integer>(); 
        inputFiles.put("src/test/resources/demo_tests.txt", 0);
        inputFiles.put("src/test/resources/Siddham-Edition Export tester.txt", 0);
        inputFiles.put("src/test/resources/Siddham-Edition Export tester_beginning.txt", 0);
        inputFiles.put("src/test/resources/tattvasangrahapanjika_raw_deva.txt", 1);
        
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer();
        
        // parse the title file line per line instead of per batch of tokens
        produceTokensByLine("src/test/resources/tripitaka-titles.txt", 0);
        
        Set<String> keys = inputFiles.keySet();
        for (String fileName: keys) {
            String inputStr = new String(Files.readAllBytes(Paths.get(fileName))); 
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
            words = new Slp2RomanFilter(words);
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
    
    private static void produceTokensByLine(String fileName, Integer encoding) throws FileNotFoundException {
        // Opening and reading the file by line is taken from https://stackoverflow.com/a/15651884
        InputStream ins = null; // raw byte-stream
        Reader r = null; // cooked reader
        BufferedReader br = null; // buffered for readLine()
        try {
            SkrtWordTokenizer tok = new SkrtWordTokenizer();
            String outFileName = "./" + fileName.substring(fileName.lastIndexOf('/'), fileName.lastIndexOf('.')) + "_lemmatized.txt";
            writer = new OutputStreamWriter(new FileOutputStream(outFileName), StandardCharsets.UTF_8);
            
            System.out.println("Processing " + fileName + "...");
            String line;
            int lineNum = 0;
            ins = new FileInputStream(fileName);
            r = new InputStreamReader(ins, "UTF-8"); // leave charset out for default
            br = new BufferedReader(r);
            while ((line = br.readLine()) != null) {
                // create the filter pipeline
                Reader input = new StringReader(line);
                CharFilter cs;
                if (encoding == 0) {
                    cs = new Roman2SlpFilter(input);
                } else {
                    cs = new Deva2SlpFilter(input);
                }
                cs = new SiddhamFilter(cs);
                cs = new GeminateNormalizingFilter(cs);
                TokenStream words = tokenize(cs, tok);
                words = new Slp2RomanFilter(words);
                
                // create tokens
                CharTermAttribute TermAttr = words.addAttribute(CharTermAttribute.class); 
                TypeAttribute typeAttr = words.addAttribute(TypeAttribute.class); 
                OffsetAttribute offsetAttr = words.addAttribute(OffsetAttribute.class); 
                 
                int batchEndOffset = 0; 
                int tmp = -1;
                
                StringBuilder tokensLine = new StringBuilder();
                while (words.incrementToken()) {
                    // token string
                    String token = TermAttr.toString(); 
                    if ("word".equals(typeAttr.type())) {
                        token += '✓';
                    } else if ("lemma".equals(typeAttr.type())) {
                        token += '√';
                    } else {
                        token += '❌';
                    } 
                    batchEndOffset = offsetAttr.endOffset();  
                    if (tmp == -1 || tmp != batchEndOffset) {
                        tmp = batchEndOffset;
                        tokensLine.append("¦ ");
                    }
                    tokensLine.append(token + " ");    
                }
                tokensLine.append("¦\n");
                
                writer.append(String.valueOf(lineNum)+ " ");
                writer.append(line+"\n"); 
                writer.append(tokensLine);
                writer.flush();
                words.close();
                lineNum ++;
            }
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage()); // handle exception
        }
        finally {
            if (br != null) { try { br.close(); } catch(Throwable t) { /* ensure close happens */ } }
            if (r != null) { try { r.close(); } catch(Throwable t) { /* ensure close happens */ } }
            if (ins != null) { try { ins.close(); } catch(Throwable t) { /* ensure close happens */ } }
        }
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
                if ("word".equals(typeAttr.type())) {
                    token += '✓';
                } else if ("lemma".equals(typeAttr.type())) {
                    token += '√';
                } else {
                    token += '❌';
                } 
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
            if (batchEndOffset < batchStartOffset) {
                batchEndOffset = batchStartOffset;
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
        
        writer.append(substring+"\n"); 
        writer.append(tokensLine+"\n");         
    }
}
