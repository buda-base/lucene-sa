package io.bdrc.lucene.sa;

import org.apache.lucene.util.Attribute;

/**
 * 
 * Copied and adapted from https://lucene.apache.org/core/6_4_1/core/org/apache/lucene/analysis/package-summary.html
 * in the section "Adding a custom Attribute".
 * 
 * <p>
 * 
 * The 
 * 
 * <p>
 * 
 * Here, "Preposition" is used as a cover name for verbal prefixes or preverbs and prepositions.
 * <br>
 * As found in <a href="https://en.wikisource.org/wiki/Page%3ASanskrit_Grammar_by_Whitney_p1.djvu/442">Whitney</a>, Sanskrit does not have
 * prepositions in the modern sense.
 * <p>
 * Prepositions can be either be integrated inside the verb or used separately. See {@link SanskritAnalyzer#SanskritAnalyzer(boolean, int, String, boolean)}.
 * 
 * @author drupchen
 *
 */
public interface PartOfSpeechAttribute extends Attribute {    
    public static enum PartOfSpeech {
      // tags found in Heritage Resources
      Indeclinable, // encoded as 0 in the Trie
      Noun,         // encoded as 1 
      Pronoun,      // encoded as 2
      Verb,         // encoded as 3
      Preposition,  // encoded as 4
      // note the Trie also contains -1 as value for multi-token lemmas.
      
      // remaining default tags 
      Adjective, 
      Adverb,  
      Conjunction, 
      Article, 
      Unknown
    }
  
    public void setPartOfSpeech(PartOfSpeech pos);
  
    public PartOfSpeech getPartOfSpeech();
}
