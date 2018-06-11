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

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.Test;

public class TestAnalyzerConfigs {
    static SkrtWordTokenizer skrtWordTokenizer = fillWordTokenizer();
    
    static private SkrtWordTokenizer fillWordTokenizer() {
        try {
            skrtWordTokenizer = new SkrtWordTokenizer(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return skrtWordTokenizer;
    }
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private List<String> generateTokenStream(TokenStream tokenStream) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
            PartOfSpeechAttribute posAttribute= tokenStream.addAttribute(PartOfSpeechAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString()); 
                System.out.println(charTermAttribute.toString() + ", tokenType: " + typeAttribute.type()+ ", POS: " + posAttribute.getPartOfSpeech());
            }
            return termList;
        } catch (IOException e) {
            assertTrue(false);
            return null;
        }
    }
    
    @Test
    public void bug21indexOutOfBounds() throws IOException
    {
        String input = "bodhicaryāvatāra - "
                + "bodhisattvacaryāvatāra - "
                + "Śāntideva - "
                + "mañjuśrī nāma saṃgīti - "
                + "mañjuśrījñānasattvasya paramārtha nāma saṃgīti - "
                + "Nāmasaṅgīti - "
                + "bodhicaryāvatāra";
        System.out.println("0 " + input);
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> tokens = generateTokenStream(words);
        List<String> expected = Arrays.asList("uz", "vas", "M", "tattva");
        assertThat(tokens, is(expected));
    }
}
