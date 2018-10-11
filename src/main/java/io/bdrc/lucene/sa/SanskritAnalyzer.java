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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.util.IOUtils;

/**
 * An Analyzer that uses {@link SkrtSyllableTokenizer} and {@link SkrtWordTokenizer} and filters with StopFilter
 * 
 * @author Chris Tomlinson
 * @author Hélios Hildt
 **/
public final class SanskritAnalyzer extends Analyzer {	
	String mode = null;
	String inputEncoding = null;
	String lenient = null;
	String defaultStopFile = "skrt-stopwords.txt";
	String stopFilename = null;
	boolean mergePrepositions = true;
	boolean filterGeminates = false;
	
	CharArraySet skrtStopWords = null;
	
	/**
	 * 
	 * @param mode            `space`, `syl` or `word`
     * @param inputEncoding   `SLP`, `deva` or `roman`
     * @param stopFilename    path to the file, empty string (default list) or null (no stopwords)
	 * 
	 * @throws IOException  the file containing the stoplist can not be found
	 */
	public SanskritAnalyzer(String mode, String inputEncoding, String stopFilename) throws IOException {
		this.mode = mode;
		this.inputEncoding = inputEncoding;
		if (stopFilename != null) {
		    InputStream stream = null;
		    if (stopFilename.isEmpty()) {
		        stream = CommonHelpers.getResourceOrFile(defaultStopFile);
		    } else {
		        stream = CommonHelpers.getResourceOrFile(stopFilename);
		    }
            this.skrtStopWords = StopFilter.makeStopSet(getWordList(stream, "#"));
		}
	}
	
	/**
     * 
     * Uses the list of stopwords defined here:
     * <a href="https://gist.github.com/Akhilesh28/b012159a10a642ed5c34e551db76f236">gist.github.com/Akhilesh28</a>
     * 
     * @param mode            `space`, `syl` or `word`
     * @param inputEncoding   `SLP`, `deva` or `roman`
     * 
     * @throws IOException the file containing the stoplist can not be read 
     */
	public SanskritAnalyzer(String mode, String inputEncoding) throws IOException {
	    this(mode, inputEncoding, "");
	}
	
	/**
	 * 
	 * Allows to change the default value(true) of mergePrepositions.
	 * 
	 * <p>
	 * 
	 * Prepositions can either be merged or kept as separate tokens.
	 * Eventually, we will want to have a more refined treatment of the prepositions to account for cases where they should be standalone tokens.
	 *  
	 *  <p>
	 *  
	 * "(...) in the classical language the usage is mainly restricted to prati, anu, and ā.", 
	 * (1125.b. of <a href="https://en.wikisource.org/wiki/Page%3ASanskrit_Grammar_by_Whitney_p1.djvu/442">Whitney</a>)
	 * 
     * @param mode              `space`, `syl` or `word`
     * @param inputEncoding     `SLP`, `deva` or `roman`
     * @param mergePrepositions concatenates the token containing the preposition with the next one if true.
     * @param filterGeminates   simplify geminates if true, else keep them as-is (default behavior)
     *                              Important: must be true if using SkrtWordTokenizer to not stumble on the spelling variations
     *                              
     * @throws IOException  the file containing the stoplist can not be found
     */
	public SanskritAnalyzer(String mode, String inputEncoding, boolean mergePrepositions, boolean filterGeminates) throws IOException {
	    this(mode, inputEncoding);
	    this.filterGeminates = filterGeminates;
	    if (mode != null && mode.equals("word")) {
	        this.mergePrepositions = mergePrepositions;
	    } else if (mergePrepositions){
	        CommonHelpers.logger.error("Can only merge prepositions if mode == word");
	        return;
	    }
	}
	
	/**
     * 
     * @param mode              `space`, `syl` or `word`
     * @param inputEncoding     `SLP`, `deva` or `roman`
     * @param mergePrepositions concatenates the token containing the preposition with the next one if true.
     * @param filterGeminates   simplify geminates if true, else keep them as-is
     *                              Important: must be true if using SkrtWordTokenizer to not stumble on the spelling variations  
     * @param lenient           `index` or `query` 
     *                              
     * @throws IOException  the file containing the stoplist can not be found
     */
	public SanskritAnalyzer(String mode, String inputEncoding, boolean mergePrepositions, boolean filterGeminates, String lenient) throws IOException {
	    this(mode, inputEncoding, mergePrepositions, filterGeminates);
	    this.lenient = lenient;
	}
	
	/**
	 * @param inputStream Stream containing the list of stopwords
	 * @param comment The string representing a comment.
	 * @return the {@link ArrayList} of stopwords
	 * @throws IOException  the input file couldn't be read
	 */
	public static ArrayList<String> getWordList(InputStream inputStream, String comment) throws IOException {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(inputStream));
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
	    if ("deva".equals(inputEncoding)) {
		    reader = new Deva2SlpFilter(reader);
		    reader = new VedicFilter(reader);
		} else if ("roman".equals(inputEncoding)) {
		    reader = new Roman2SlpFilter(reader);
		} else if ("SLP".equals(inputEncoding)) {
		    CommonHelpers.logger.error("wrong value for `mode`");
		    return null;
		}
		
	    if (filterGeminates == true) {
	        reader = new GeminateNormalizingFilter(reader);
	    }
	    
	    // what happens in lenient mode is that first the input is transformed
	    // into SLP then into SLP->Lenient. This is a bit awkward but it should work
	    if ("query".equals(lenient)) {
	        reader = new LenientCharFilter(reader);
	    }
	    
		return super.initReader(fieldName, reader);
	}
	
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		Tokenizer source = null;
		TokenStream filter = null;
		
		if (mode != null && mode.equals("word")) {
			try {
				source = new SkrtWordTokenizer();
			} catch (Exception e) {
			    CommonHelpers.logger.error("cannot initialize SkrtWordTokenizer", e);
                return null;
            }
		} else if (mode != null && mode.equals("syl")) {
			source = new SkrtSyllableTokenizer();
		} else if (mode != null && mode.equals("space")) {
		    source = new WhitespaceTokenizer();
		}
		
		if (skrtStopWords != null) {  // a stop list was parsed
			filter = new StopFilter(source, skrtStopWords);
		} else {
			filter = (TokenStream) source;
		}
		
		if (mergePrepositions) {
		    filter = new PrepositionMergingFilter(filter);
		}
		
		if (lenient != null && lenient.equals("index")) {
		    filter = new Slp2RomanFilter(filter);
		    filter = new LenientTokenFilter(filter);
		}
		
		return new TokenStreamComponents(source, filter);
	}
}