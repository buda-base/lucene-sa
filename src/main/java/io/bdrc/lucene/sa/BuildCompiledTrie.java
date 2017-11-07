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
import io.bdrc.lucene.stemmer.Trie;

public class BuildCompiledTrie {
	
	public static void main(String [] args){
		List<String> inputFiles = Arrays.asList(
				"resources/sanskrit-stemming-data/output/total_output.txt"	// Sanskrit Heritage entries
				);
		String outFile = "src/main/resources/skrt-compiled.trie";
		
		try {
			Trie trie = buildTrie(inputFiles, false); 	// optimize is set to false to avoid StackOverFlow error
			storeTrie(trie, outFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param inputFiles
	 * @param optimize
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static Trie buildTrie(List<String> inputFiles, boolean optimize) throws FileNotFoundException, IOException {
		/* Fill the Trie with the content of all inputFiles*/
		Trie trie = new Trie(true);
		for (String filename: inputFiles) {
			try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
				String line;
				while ((line = br.readLine()) != null) {
					int endOfFormIndex = line.indexOf(',');
					if (endOfFormIndex != -1) {
						trie.add(line.substring(0, endOfFormIndex), line.substring(endOfFormIndex+1));
					} else {
						throw new IllegalArgumentException("The dictionary file is corrupted in the following line.\n" + line);
					}
				}
			}
		}
		
		/* Optimize the Trie*/
		if (optimize) {
			Optimizer opt = new Optimizer();
			trie.reduce(opt);
		}
		return trie;
	}
	
	/**
	 * 
	 * @param trie
	 * @param outFilename
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void storeTrie(Trie trie, String outFilename) throws FileNotFoundException, IOException {
		OutputStream output = new DataOutputStream(new FileOutputStream(outFilename));
		trie.store((DataOutput) output);
	}	
}
