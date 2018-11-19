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
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.Test;

public class TestOffsets {

    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private void assertTokenStream(TokenStream tokenStream, List<String> expected, String origString) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
            String log = "";
            while (tokenStream.incrementToken()) {
                int start = offsetAttribute.startOffset();
                int end = offsetAttribute.endOffset();
                String origSub = origString.substring(offsetAttribute.startOffset(), offsetAttribute.endOffset());
                log += charTermAttribute.toString() + " -> " +  "start:" + start + ", end:" + end + ", substring:'" + origSub + "'\n";
                termList.add(start + ":" + end);
            }
            System.out.println(log);
            System.out.println("1 " + String.join(" ", expected));
            System.out.println("2 " + String.join(" ", termList) + "\n");
            assertThat(termList, is(expected));
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
    
    @Test
    public void testFromSLP() throws IOException
    {
        System.out.println("Increment Position");
        String input = "SrIjYAna Darma boDi loke loke";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("0:3", "2:5", "5:8", "5:8", "5:8", "9:14", "9:14", "15:19", "15:19", "20:24", "20:24", "26:29", "26:29", "26:29");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(); 
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(words, expected, input);
    }

    @Test
    public void testDemoWords() throws IOException
    {
        System.out.println("bug9");
        String input = "sattvasya paramārtha nāma" +  
                "bodhicaryāvatāra bodhisattvacaryāvatāra - "
                + "Śāntideva - "
                + "mañjuśrī nāma saṃgīti - "
                + "mañjuśrījñānasattvasya paramārtha nāma saṃgīti - "
                + "Nāmasaṃgīti - "
                + "bodhicaryāvatāra";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("0:9", "10:16", "15:20", "15:20", "21:25", "25:30", "25:30", "30:35", "34:41", "42:53", "53:58", "57:64", "67:76", 
                "79:87", "89:92", "89:92", "89:92", "93:100", "103:111", "113:117", "113:117", "113:116", "116:125", "127:131", "127:132", "131:136", "131:136", 
                "137:141", "142:149", "152:156", "156:163", "166:171", "166:171", "171:176", "175:182");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer(); 
        Reader readerF = new Roman2SlpFilter(reader);
        TokenStream words = tokenize(readerF, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        assertTokenStream(words, expected, input);
        reader.close();
    }
    
    public static void testIASTOffsets(String input, List<String> expected, boolean wordmode) throws IOException {
        Reader reader = new StringReader(input);
        System.out.println("0 " + input);
        Tokenizer tokenizer;
        if (wordmode) {
            tokenizer = new SkrtWordTokenizer(); 
        } else {
            tokenizer = new SkrtSyllableTokenizer();
        }
        Reader readerF = new Roman2SlpFilter(reader);
        TokenStream words = tokenize(readerF, tokenizer);
        words = new PrepositionMergingFilter(words);
        //words = new Slp2RomanFilter(words);
        assertTokenStream(words, expected, input);
        reader.close();
    }
    
    @Test
    public void bug2Offset() throws IOException
    {
        System.out.println("bug2Offset");
        testIASTOffsets("śrījñāna", Arrays.asList("0:3", "3:6", "6:8"), false);
        testIASTOffsets("śrījñāna", Arrays.asList("0:3", "2:5", "5:8", "5:8", "5:8"), true);
        testIASTOffsets("parāpakārarakṣā", Arrays.asList("0:4", "0:4", "3:8", "3:11", "4:11", "11:15", "11:15", "11:15", "11:15"), true);
        testIASTOffsets("pratimāmānalakṣaṇa", Arrays.asList("0:8", "8:12", "13:17"), true);
        testIASTOffsets("madhyamakavatara", Arrays.asList("0:10", "10:15"), true);
        testIASTOffsets("rasāyanaśāstroddhṛti", Arrays.asList("0:6", "0:6", "6:10", "10:14", "10:14", "15:20", "15:20", "15:15", "15:20", "15:20"), true);
        testIASTOffsets("kosalālaṃkāratattvasaṃgrahatīka", Arrays.asList("0:7", "7:9", "9:13", "13:19", "19:27", "27:30"), true);
        testIASTOffsets("śrītrailokyavijayamaṇḍalopāyikā-āryatattvasaṃgrahatantroddhṛtā", Arrays.asList("0:3", "4:7", "4:12", "12:19", "14:19", "19:23", "23:27", "23:27", "26:29", "26:31", "34:38", "34:38", "34:38", "38:39", "39:42", "41:45", "42:50", "50:56", "50:56", "57:62", "57:62", "57:62", "57:62"), true);
    }
    
}
