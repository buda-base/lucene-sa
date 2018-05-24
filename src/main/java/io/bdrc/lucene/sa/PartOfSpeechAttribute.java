package io.bdrc.lucene.sa;

import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

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
      Noun, 
      Verb, 
      Preverb, // added for Sanskrit 
      Adjective, 
      Adverb, 
      Pronoun, 
      Preposition, 
      Conjunction, 
      Article, 
      Unknown
    }
  
    public void setPartOfSpeech(PartOfSpeech pos);
  
    public PartOfSpeech getPartOfSpeech();
}


final class PartOfSpeechAttributeImpl extends AttributeImpl implements PartOfSpeechAttribute {

    private PartOfSpeech pos = PartOfSpeech.Unknown;

    public void setPartOfSpeech(PartOfSpeech pos) {
        this.pos = pos;
    }

    public PartOfSpeech getPartOfSpeech() {
        return pos;
    }

    @Override
    public void clear() {
        pos = PartOfSpeech.Unknown;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        ((PartOfSpeechAttribute) target).setPartOfSpeech(pos);
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
        // inspired by https://github.com/apache/lucene-solr/blob/branch_6_4/lucene/core/src/java/org/apache/lucene/util/AttributeImpl.java#L78
        reflector.reflect(PartOfSpeechAttribute.class, "pos", getPartOfSpeech());
    }
}