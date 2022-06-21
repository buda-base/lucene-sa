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

public class TestSyllableTokenizer {
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
        try {
            final List<String> termList = new ArrayList<String>();
            final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            final OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
                System.out.println(charTermAttribute.toString()+" "+offsetAttribute.startOffset()+"-"+offsetAttribute.endOffset());
            }
            System.out.println("1 " + String.join(" ", expected));
            System.out.println("2 " + String.join(" ", termList) + "\n");
            assertThat(termList, is(expected));
        } catch (IOException e) {
            assertTrue(false);
        }
    }
    
    @Test
    public void simpleCase() throws IOException
    {
        System.out.println("simple case");
        String input = "nAma-saNgIti";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("nA", "ma", "sa", "NgI", "ti");
        System.out.println("0 " + input);
        SkrtSyllableTokenizer skrtSylTokenizer = new SkrtSyllableTokenizer();
        TokenStream words = tokenize(reader, skrtSylTokenizer);
        assertTokenStream(words, expected);
    }

    @Test
    public void withTrans() throws IOException
    {
        System.out.println("lenient case");
        String input = "nāma-saṅgīti";
        Reader reader = new StringReader(input);
        reader = new Roman2SlpFilter(reader);
        reader = new LenientCharFilter(reader);
        List<String> expected = Arrays.asList("na", "ma", "sa", "ngi", "ti");
        System.out.println("0 " + input);
        SkrtSyllableTokenizer skrtSylTokenizer = new SkrtSyllableTokenizer();
        TokenStream words = tokenize(reader, skrtSylTokenizer);
        assertTokenStream(words, expected);
    }

    @Test
    public void withTransCase() throws IOException
    {
        System.out.println("lenient case");
        String input = "Nāma-SaṅgīLi";
        Reader reader = new StringReader(input);
        reader = new Roman2SlpFilter(reader);
        reader = new LenientCharFilter(reader);
        List<String> expected = Arrays.asList("na", "ma", "sa", "ngi", "li");
        System.out.println("0 " + input);
        SkrtSyllableTokenizer skrtSylTokenizer = new SkrtSyllableTokenizer();
        TokenStream words = tokenize(reader, skrtSylTokenizer);
        assertTokenStream(words, expected);
    }
    
    @Test
    public void withAnalyzerIndex() throws IOException
    {
        System.out.println("analyzer index mode");
        String input = "nāma-saṅgīti";
        Reader reader = new StringReader(input);
        SanskritAnalyzer sa = new SanskritAnalyzer("syl", "roman", false, true, "index");
        List<String> expected = Arrays.asList("na", "ma", "sa", "ngi", "ti");
        System.out.println("0 " + input);
        TokenStream words = sa.tokenStream("", reader);
        words.reset();
        assertTokenStream(words, expected);
        sa.close();
    }

    public void assertLenient(String input, List<String> expected, String lenientMode) throws IOException {
        Reader reader = new StringReader(input);
        SanskritAnalyzer sa = new SanskritAnalyzer("syl", "roman", false, true, lenientMode);
        System.out.println("0 " + input);
        TokenStream words = sa.tokenStream("", reader);
        words.reset();
        assertTokenStream(words, expected);
        sa.close();
    }
    
    @Test
    public void testRnorm() throws IOException
    {
        assertLenient("Maitreyapraṇidhanarāja", Arrays.asList("mai", "trie", "ya", "pria", "ni", "da", "na", "ria", "ja"), "index");
        assertLenient("ṛtāvan kṛṣṇa śrījñāna", Arrays.asList("ri", "ta", "ban", "kri", "sna", "sri", "jna", "na"), "index");
        assertLenient("rtavan krshna shrjnana", Arrays.asList("ri", "ta", "ban", "kri", "sna", "sri", "jna", "na"), "query");
        assertLenient("ritavan krishna shrijnana", Arrays.asList("ri", "ta", "ban", "kri", "sna", "sri", "jna", "na"), "query");
    }
    
    @Test
    public void withAnalyzerQuery() throws IOException
    {
        System.out.println("analyzer query mode");
        SanskritAnalyzer sa = new SanskritAnalyzer("syl", "roman", false, false, "query");
        String input = "nama sangiti";
        Reader reader = new StringReader(input);
        List<String> expected = Arrays.asList("na", "ma", "sa", "ngi", "ti");
        System.out.println("0 " + input);
        TokenStream words = sa.tokenStream("", reader);
        words.reset();
        assertTokenStream(words, expected);
        sa.close();
    }
}
