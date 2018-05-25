package io.bdrc.lucene.sa;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** Default implementation of {@link PartOfSpeechAttribute}. */
public class PartOfSpeechAttributeImpl extends AttributeImpl implements PartOfSpeechAttribute, Cloneable {
    
    private PartOfSpeech pos = PartOfSpeech.Unknown;
    
    public PartOfSpeechAttributeImpl() {}
    
    /** Initialize this attribute with <code>pos</code> */
    public PartOfSpeechAttributeImpl(PartOfSpeech pos) {
        super();
        this.pos = pos;
    }
    
    @Override
    public void setPartOfSpeech(PartOfSpeech pos) {
        this.pos = pos;
    }

    @Override
    public PartOfSpeech getPartOfSpeech() {
        return pos;
    }

    @Override
    public void clear() {
        pos = PartOfSpeech.Unknown;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        ((PartOfSpeechAttribute) target).setPartOfSpeech(PartOfSpeech.Unknown);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }
      
      if (other instanceof PartOfSpeechAttributeImpl) {
        final PartOfSpeechAttributeImpl o = (PartOfSpeechAttributeImpl) other;
        return (this.pos == null ? o.pos == null : this.pos.equals(o.pos));
      }
      
      return false;
    }
    
    @Override
    public void reflectWith(AttributeReflector reflector) {
        // inspired by https://github.com/apache/lucene-solr/blob/branch_6_4/lucene/core/src/java/org/apache/lucene/util/AttributeImpl.java#L78
        reflector.reflect(PartOfSpeechAttribute.class, "pos", pos);
    }
}
