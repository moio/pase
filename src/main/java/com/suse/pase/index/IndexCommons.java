package com.suse.pase.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharTokenizer;

public class IndexCommons {
    public static final String SOURCE_FIELD = "source";
    public static final String PATH_FIELD = "path";
    public static final String PATH_FINGERPRINT_FIELD = "path_fingerprint";
    public static final String LAST_UPDATED_FIELD = "last_updated";


    /** Divides text by line (treats the whole line as a term). */
    public static class SourceAnalyzer extends Analyzer {
        private class SourceCodeTokenizer extends CharTokenizer {
            public SourceCodeTokenizer() {
                super(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, 1024);
            }

            @Override
            protected boolean isTokenChar(int cp) {
                return cp != 0x0D && cp != 0x0A;
            }
        }

        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            return new TokenStreamComponents(new SourceCodeTokenizer());
        }
    }
}
