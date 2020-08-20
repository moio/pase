package com.suse.pase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharTokenizer;

/** Divides text by line (treats the whole line as a term). */
public class SourceAnalyzer extends Analyzer {
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
