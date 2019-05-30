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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.util.IOUtils;

import io.bdrc.lucene.sixtofour.CharArraySet;
import io.bdrc.lucene.sixtofour.StopFilter;
import io.bdrc.lucene.sixtofour.Dummy;

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
		    InputStream stream;
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
	    CommonHelpers.logger.info("new SanskritAnalyzer('" + mode + "', '" + inputEncoding + "')");
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
	    if ("word".equals(mode)) {
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
     * This constructor is required by eXist (3.4.1 may be higher as well)
     * Apart from the version parameter, it is identical to the constructor by mode and inputEncoding
     *
     * @param version         Apache Lucene version that eXist employs
     * @param mode            `space`, `syl` or `word`
     * @param inputEncoding   `SLP`, `deva` or `roman`
     *
     * @throws IOException the file containing the stoplist can not be read
     */
    public SanskritAnalyzer(org.apache.lucene.util.Version version, String mode, String inputEncoding) throws IOException {
        this(mode, inputEncoding);
    }

    /**
     * This constructor is required by eXist (3.4.1 may be higher as well)
     * Apart from the version parameter, it is identical to the corresponding constructor taking the rest of the params
     *
     * @param version         Apache Lucene version that eXist employs
     * @param mode              `space`, `syl` or `word`
     * @param inputEncoding     `SLP`, `deva` or `roman`
     * @param mergePrepositions concatenates the token containing the preposition with the next one if true.
     * @param filterGeminates   simplify geminates if true, else keep them as-is
     *                              Important: must be true if using SkrtWordTokenizer to not stumble on the spelling variations
     * @param lenient           `index` or `query`
     *
     * @throws IOException  the file containing the stoplist can not be found
     */
    public SanskritAnalyzer(org.apache.lucene.util.Version version, String mode, String inputEncoding, boolean mergePrepositions, boolean filterGeminates, String lenient) throws IOException {
        this(mode, inputEncoding, mergePrepositions, filterGeminates, lenient);
    }

    /** Default constructor - also required for eXist (3.4.1) integration */
    public SanskritAnalyzer() throws IOException {
        this("XYZ", "XYZ");
        CommonHelpers.logger.info("new (default c-tor) SanskritAnalyzer()");
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
		} else if (!"SLP".equals(inputEncoding)) {
		    CommonHelpers.logger.error("wrong value for `mode`");
		    return null;
		}
		
	    if (filterGeminates == true) {
	        reader = new GeminateNormalizingFilter(reader);
	    }
	    
	    // what happens in lenient mode is that first the input is transformed
	    // into SLP then into SLP->Lenient. This is a bit awkward but it should work
	    if (lenient != null) {
	        reader = new LenientCharFilter(reader);
	    }
	    
		return super.initReader(fieldName, reader);
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName, Reader reader) {
		Tokenizer source = null;
		TokenStream filter = null;
		
		if ("word".equals(mode)) {
			try {
				source = new SkrtWordTokenizer();
			} catch (Exception e) {
			    CommonHelpers.logger.error("cannot initialize SkrtWordTokenizer", e);
                return null;
            }
		} else if ("syl".equals(mode)) {
			source = new SkrtSyllableTokenizer(this.lenient != null);
		} else if ("space".equals(mode)) {
		    source = new WhitespaceTokenizer(Dummy.READER);
		} else {
		    throw new IllegalArgumentException("Illegal argument 'mode' value: " + mode);
		}
		
		if (skrtStopWords != null) {  // a stop list was parsed
			filter = new StopFilter(source, skrtStopWords);
		} else {
			filter = (TokenStream) source;
		}
		
		if (mergePrepositions) {
		    filter = new PrepositionMergingFilter(filter);
		}
		
		if ("index".equals(lenient)) {
		    filter = new Slp2RomanFilter(filter);
		    filter = new LenientTokenFilter(filter);
		}

		// This is required for Lucene versions prior to 5.0.0
		// as Lucene 5 (and later) Analyzer.tokenStream() *always* invokes components.setReader(r)
		// no matter if it is a reused or a new components object,
		// whereas Lucene up to 4.10.4 invokes it only on reused components object.
		if (org.apache.lucene.util.Version.LATEST.major < 5) {
		    try {
		        source.setReader(reader);
		    } catch (IOException e) {
		        throw new RuntimeException("Unexpected: ", e);
		    }
		}

		return new TokenStreamComponents(source, filter);
	}
}
