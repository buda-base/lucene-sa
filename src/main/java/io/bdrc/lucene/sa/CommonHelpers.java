package io.bdrc.lucene.sa;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Comparator;
import java.util.MissingResourceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonHelpers {
    static final Logger logger = LoggerFactory.getLogger(CommonHelpers.class);
    public static final String baseDir = "src/main/resources/";
    
    public static InputStream getResourceOrFile(final String baseName) {
        InputStream stream = null;
        stream = CommonHelpers.class.getResourceAsStream("/"+baseName);
        if (stream != null) {
            logger.info("found resource /{} through regular classloader", baseName);
            return stream;
        }
        stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/"+baseName);
        if (stream != null) {
            logger.info("found resource /{} through thread context classloader", baseName);
            return stream;
        }
        final String fileBaseName = baseDir+baseName;
        try {
            stream = new FileInputStream(fileBaseName);
            logger.info("found file {}", fileBaseName);
            return stream;
        } catch (FileNotFoundException e) {
            logger.info("could not find file {}", fileBaseName);
            return null;
        }  
    }

    public static final BufferedReader getFileContent (final String baseName) {
        try {
            return new BufferedReader(new FileReader(baseName));
        } catch (FileNotFoundException e) {
            throw new MissingResourceException("cannot find file " + baseName, "", "");
        }
    }
    
    public static class LengthComp implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            final int lenComp = s2.length() - s1.length();
            if (lenComp != 0) {
                return lenComp;
            }
            return s1.compareTo(s2);
        }
    }
}
