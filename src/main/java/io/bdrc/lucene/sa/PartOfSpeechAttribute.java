package io.bdrc.lucene.sa;

import org.apache.lucene.util.Attribute;

/**
 * 
 * Copied and adapted from https://lucene.apache.org/core/6_4_1/core/org/apache/lucene/analysis/package-summary.html
 * in the section "Adding a custom Attribute".
 * 
 * @author drupchen
 *
 */
public interface PartOfSpeechAttribute extends Attribute {    
    public static enum PartOfSpeech {
      // added to match the Sanskrit Heritage tags as found in Heritage Resources
      Indeclinable, // encoded as 0 in the Trie
      Noun,         // encoded as 1 
      Pronoun,      // encoded as 2
      Verb,         // encoded as 3
      Preverb,      // encoded as 4
      
      // remaining default tags 
      Adjective, 
      Adverb, 
      Preposition, 
      Conjunction, 
      Article, 
      Unknown
    }
  
    public void setPartOfSpeech(PartOfSpeech pos);
  
    public PartOfSpeech getPartOfSpeech();
}
