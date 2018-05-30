package io.bdrc.lucene.sa;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.bdrc.lucene.stemmer.Reduce;
import io.bdrc.lucene.stemmer.Trie;

public class BuildCompiledTrie {
	/**
	 * Builds a Trie from all the entries in a list of files
	 * Dumps it in a binary file
	 * 
	 * !!! Ensure to have enough Stack memory 
	 * ( -Xss40m seems to be enough for all the inflected forms of Sanskrit Heritage)
	 * 
	 */
	
	static String outFile = "src/main/resources/skrt-compiled-trie.dump";
	public static String inputFile = "resources/sanskrit-stemming-data/output/trie_content.txt";
	
	public static void main(String [] args) throws IOException{
			Trie trie = compileTrie();
			storeTrie(trie, outFile);
	}
	
	/**
	 * used in {@link SkrtWordTokenizer} constructors
	 * 
	 * builds the Trie
	 * 
	 * @throws FileNotFoundException  input or output file not found
	 * @throws IOException  input can't be read or output can't be written
	 */
	public static Trie compileTrie() throws IOException {
		Trie trie = new Reduce().optimize(buildTrie(inputFile));
		return trie;
	}
	
	/** 
	 * 
	 * @param inputFiles  the list of files to feed the Trie with
	 * @return the non-optimized Trie
	 * @throws FileNotFoundException  input file not found
	 * @throws IOException  output file can't be written
	 */
	public static Trie buildTrie(String filename) throws IOException {
		System.out.println("\tBuilding the Trie from the raw text file… It will take some time!");
	    long one = System.currentTimeMillis();
		/* Fill the Trie with the content of all inputFiles*/
		Trie trie = new Trie(true);
		BufferedReader br = CommonHelpers.getFileContent(filename);
		String line;
		while ((line = br.readLine()) != null) {
            final int sepIndex = line.indexOf(',');
            if (sepIndex == -1) {
                throw new IllegalArgumentException("The dictionary file is corrupted in the following line.\n" + line);
            } else {
                trie.add(line.substring(0, sepIndex), line.substring(sepIndex+1));
            }
        }
		long two = System.currentTimeMillis();
		String msg = "\tTime: " + (two - one) / 1000 + "s.";
		System.out.println(msg);
		CommonHelpers.logger.info(msg);
		return trie;
	}
    
    public static void storeTrie(Trie trie, String outFilename) throws IOException {
        try {
            OutputStream output = new DataOutputStream(new FileOutputStream(outFilename));
            trie.store((DataOutput) output);
        } catch (FileNotFoundException e) {
            CommonHelpers.logger.info("could not find file {}", outFilename);
            return;
        }
    }
}
