package io.bdrc.lucene.sa;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import io.bdrc.lucene.stemmer.Optimizer;
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
	
	static boolean optimize = false;	// change to true to optimize the Trie
	static String outFile = "src/main/resources/skrt-compiled-trie.dump";
	public static List<String> inputFiles = Arrays.asList(
			"resources/sanskrit-stemming-data/output/total_output.txt",	// all Sanskrit Heritage entries
			"resources/trie-exceptions.txt"	// things like -tva
			);
	
	public static void main(String [] args){
		
		try {
			Trie trie = buildTrie(inputFiles);
			
			if (optimize) {
				trie = optimizeTrie(trie, new Optimizer());		
				storeTrie(trie, "src/main/resources/skrt-compiled-trie_optimized.dump");	
			} else {
				storeTrie(trie, outFile);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * used in {@link SkrtWordTokenizer} constructors
	 * @param inputFiles
	 * @return builds the Trie, then stores it to a file (no optimization)
	 */
	public static void compileTrie() throws FileNotFoundException, IOException {
		Trie trie = buildTrie(inputFiles);
		storeTrie(trie, outFile);
	}
	
	/** 
	 * 
	 * @param inputFiles  the list of files to feed the Trie with
	 * @return the non-optimized Trie
	 */
	public static Trie buildTrie(List<String> inputFiles) throws FileNotFoundException, IOException {
		long one = System.currentTimeMillis();
		/* Fill the Trie with the content of all inputFiles*/
		Trie trie = new Trie(true);
		for (String filename: inputFiles) {
			try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
				String line;
				while ((line = br.readLine()) != null) {
					final int spaceIndex = line.indexOf(',');
					if (spaceIndex == -1) {
						throw new IllegalArgumentException("The dictionary file is corrupted in the following line.\n" + line);
					} else {
						trie.add(line.substring(0, spaceIndex), line.substring(spaceIndex+1));
					}
				}
			}
		}
		long two = System.currentTimeMillis();
		System.out.println("Building the Trie from the raw text file took: " + (two - one) / 1000 + "s.");
		return trie;
	}
	
	/**
	 *  
	 * Optimizer  - optimisation time: 10mn ; compiled Trie size: 10mo
	 * Optimizer2 - optimisation time: 12mn ; compiled Trie size: 12mo
	 * 
	 * @param trie
	 * @param optimizer
	 * @return
	 */
	public static Trie optimizeTrie(Trie trie, Reduce optimizer) {
		long three = System.currentTimeMillis();
		trie = optimizer.optimize(trie);
		long four = System.currentTimeMillis();
		System.out.println("Optimizing the Trie took: " + (four - three) / 1000 + "s.");
		return trie;
	}
	
	/**
	 * 
	 * @param trie  the trie to store in binary format
	 * @param outFilename  the path+filename of the output file
	 */
	public static void storeTrie(Trie trie, String outFilename) throws FileNotFoundException, IOException {
		OutputStream output = new DataOutputStream(new FileOutputStream(outFilename));
		trie.store((DataOutput) output);
	}	
}
