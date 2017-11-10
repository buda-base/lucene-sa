package io.bdrc.lucene.sa;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import io.bdrc.lucene.stemmer.Gener;
import io.bdrc.lucene.stemmer.Lift;
import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Optimizer2;
import io.bdrc.lucene.stemmer.Reduce;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

public class TrieOptimization {

	public static void main(String [] args){
		List<String> inputFiles = Arrays.asList(
				"src/test/resources/tries/abab_test.txt"
				);
		
		try {
			Trie trie = BuildCompiledTrie.buildTrie(inputFiles);			
			List<Reduce> optimizers = Arrays.asList(new Reduce(), new Optimizer(), new Optimizer2(), 
					new Lift(true), new Lift(false), new Gener());

			
			for (Reduce opt: optimizers) {
				Trie optimized = BuildCompiledTrie.optimizeTrie(trie, opt);
//				getIdxAndPrintRow(optimized, optimized.getRoot());
				Row root = optimized.getRow(optimized.getRoot());
				System.out.println(root);
			}	
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static int[] getIdxAndPrintRow(Trie trie, int currentIdx) {
		String rowStr = trie.getRow(currentIdx).toString();
		System.out.println(rowStr);
		int[] refs = findRefs(rowStr);
		if (refs.length != 0) {
			for (int ref: refs) {
				getIdxAndPrintRow(trie, ref);
			}
		}
		return refs;
	}
	
	public static int[] findRefs(String row) {
		Pattern r = Pattern.compile("ref\\(([0-9]+)\\)");
		String[] matches = r.split(row);
		int[] refs = new int[matches.length]; 
		for (int i = 0; i<= matches.length; i++) {
			System.out.println(matches[i]);
			refs[i] = Integer.valueOf(matches[i]);
		}
		
		// take 2nd char
		
		return refs;
	}
}
