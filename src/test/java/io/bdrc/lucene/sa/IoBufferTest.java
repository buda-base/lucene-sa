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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IoBufferTest {
	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}
	
	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
		List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString());
			}
			System.out.println("1 " + String.join(" ", expected));
			System.out.println("2 " + String.join(" ", termList) + "\n");
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}
	
	static private String repeatChar(char c, int times) {
		char[] array = new char[times]; 
		Arrays.fill(array, c);
		return String.valueOf(array);
	}

	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}
	
    @Test
    public void testIOBufferSize1() throws Exception {
    	String input = repeatChar('a', 6144);
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList(input);
    	SkrtWordTokenizer skrtWordTokenizer = new SkrtWordTokenizer("src/test/resources/tries/IOBuffer_1.txt");
    	skrtWordTokenizer.IO_BUFFER_SIZE = 6144;
    	skrtWordTokenizer.MAX_WORD_LEN = 6144;
		TokenStream syllables = tokenize(reader, skrtWordTokenizer);
		assertTokenStream(syllables, expected);
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}
