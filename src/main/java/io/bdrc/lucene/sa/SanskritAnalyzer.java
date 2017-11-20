/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the 
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.bdrc.lucene.sa;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.IOUtils;

/**
 * An Analyzer that uses {@link TibSyllableTokenizer} and filters with StopFilter
 * 
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceAnalyzer.java
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 **/
public final class SanskritAnalyzer extends Analyzer {	
	boolean segmentInWords = false; 
	int inputEncoding = 0;
	CharArraySet skrtStopWords;
	@SuppressWarnings("unused")
	private CharArraySet srktStopSet;
	
	/**
	 * Creates a new {@link SanskritAnalyzer}
	 * 
	 * @param segmentInWords if the segmentation is on words instead of syllables
	 * @param inputEncoding `0` for SLP, `1` for devanagari, `2` for romanized sanskrit
	 * @param stopFilename formatting: 
	 * 								- in SLP encoding
	 * 								- 1 word per line 
	 * 								- empty lines (with and without comments), spaces and tabs are allowed 
	 * 								- comments start with `#`
	 * 								- lines can end with a comment
	 * @throws FileNotFoundException  the file containing the stoplist can not be read
	 * @throws IOException  the file containing the stoplist can not be found
	 */
	public SanskritAnalyzer(boolean segmentInWords, int inputEncoding, String stopFilename) throws FileNotFoundException, IOException {
		this.segmentInWords = segmentInWords;
		this.inputEncoding = inputEncoding;
		if (stopFilename != null) {
			this.srktStopSet = StopFilter.makeStopSet(getWordList(stopFilename, "#"));
		} else {
			this.skrtStopWords = null;
		}
	}
	
	/**
	 * Creates a new {@link SanskritAnalyzer} with the default values
	 * 
	 * Uses the list of stopwords defined here:
	 *  {@link https://gist.github.com/Akhilesh28/b012159a10a642ed5c34e551db76f236}
	 * 
	 * @throws IOException the file containing the stoplist can not be read
	 * @throws FileNotFoundException  the file containing the stoplist can not be found 
	 */
	public SanskritAnalyzer() throws FileNotFoundException, IOException {
		this(true, 0, "src/main/resources/skrt-stopwords.txt");
	}
	
	/**
	 * @param reader Reader containing the list of stopwords
	 * @param comment The string representing a comment.
	 * @return result the {@link ArrayList} to fill with the reader's words
	 */
	public static ArrayList<String> getWordList(String filename, String comment) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			String word = null;
			while ((word = br.readLine()) != null) {
				word = word.replace("\t", "");
				if (word.contains(comment)) {
					if (!word.startsWith(comment)) {
						word = word.substring(0, word.indexOf(comment));
						word = word.trim();
						if (!word.isEmpty()) result.add(word);
					}
				} else {
					word = word.trim();
					if (!word.isEmpty()) result.add(word);
				}
			}
		}
		finally {
			IOUtils.close(br);
		}
		return result;
	}
	
	@Override
	protected Reader initReader(String fieldName, Reader reader) {
		if (this.inputEncoding == 0) {
			return super.initReader(fieldName, reader);
		} else {
			if (this.inputEncoding == 1) {
				reader = new Deva2SlpFilter(reader);
				return super.initReader(fieldName, reader);
			} else if (this.inputEncoding == 2) {
				reader = new Roman2SlpFilter(reader);
				return super.initReader(fieldName, reader);
			} else {
				throw new IllegalArgumentException("options for input encoding:\n0 for SLP, 1 for devanagari, 2 for romanized");
			}
		}
	}
	
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = null;
		TokenStream filter = null;
		
		if (segmentInWords) {
			try {
				source = new SkrtWordTokenizer();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			source = new SkrtSyllableTokenizer();
		}		
		
		if (skrtStopWords != null) {  // a stop list was parsed
			filter = new StopFilter(source, skrtStopWords);
		} else {
			filter = (TokenStream) source;
		}
		return new TokenStreamComponents(source, filter);
	}
}