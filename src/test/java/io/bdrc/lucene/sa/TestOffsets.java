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
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Trie;

public class TestOffsets {

    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private SkrtWordTokenizer buildTokenizer(String trieName) throws FileNotFoundException, IOException {
        Trie trie = BuildCompiledTrie.buildTrie(trieName + ".txt");

        return new SkrtWordTokenizer(true);
    }
    
    static private void assertTokenStream(TokenStream tokenStream, List<String> expected, String origString) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute incrAttribute = tokenStream.addAttribute(PositionIncrementAttribute.class);
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
            assertTrue(false);
        }
    }
    
    @Test
    public void testFromSLP() throws IOException
    {
        System.out.println("Increment Position");
        String input = "SrIjYAna Darma boDi loke loke";  // only loka for last token as only absolute final sandhi is applicable at end of input
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("0:3", "2:5", "5:8", "5:8", "3:8", "9:14", "9:14", "15:19", "15:19", "20:24", "20:24", "25:29", "25:29");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/increment_position_test"); 
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        assertTokenStream(words, expected, input);
    }
    
    @Test
    public void testDemoWords() throws IOException
    {
        System.out.println("bug9");
        String input = "sattvasya paramārtha nāma" +  
                "bodhicaryāvatara bodhisattvacaryāvatara - "
                + "Śāntideva - "
                + "mañjuśrī nāma saṃgīti - "
                + "mañjuśrījñānasattvasya paramārtha nāma saṃgīti - "
                + "Nāmasaṃgīti - "
                + "bodhicaryāvatara";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("0:9", "10:16", "15:20", "15:20", "21:25", "25:30", "25:30", "30:35", "34:41", "42:53", "53:58", "57:64", 
                "67:76", "79:87", "88:92", "93:100", "103:111", "113:117", "113:117", "111:116", "116:125", "127:131", "126:132", "131:136", "131:136", 
                "137:141", "142:149", "152:156", "156:163", "166:171", "166:171", "171:176", "175:182");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/demo_test"); 
        reader = new Roman2SlpFilter(reader);
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        assertTokenStream(words, expected, input);
    }
    
    @Test
    public void bug1Offset() throws IOException
    {
        System.out.println("bug9");
        String input = "śrījñāna";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("0:3", "2:5", "5:8", "5:8", "3:8");
        System.out.println("0 " + input);
        
        SkrtWordTokenizer skrtWordTokenizer = buildTokenizer("src/test/resources/tries/demo_test"); 
        reader = new Roman2SlpFilter(reader);
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        assertTokenStream(words, expected, input);
    }
}
