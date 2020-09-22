package com.suse.pase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;

import java.io.IOException;

public class IndexCommons {
    public static final String SOURCE_FIELD = "source";
    public static final String PATH_FIELD = "path";


    /**
     * Divides text by line (treats the whole line as a term), and hashes each line into a compact representation.
     */
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
            var source = new SourceCodeTokenizer();
            var filter = new HashingFilter(source);
            return new TokenStreamComponents(source, filter);
        }

        /**
         * Divides text by line (treats the whole line as a term).
         */
        private class HashingFilter extends TokenFilter {
            private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

            public HashingFilter(TokenStream in) {
                super(in);
            }

            private final char[] CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

            @Override
            public final boolean incrementToken() throws IOException {
                if (input.incrementToken()) {
                    var buffer = termAtt.buffer();
                    int length = termAtt.length();

                    // Compute String.hashCode() on the first length chars of buffer
                    int hashCode = 1;
                    for (var i = 0; i < length; ++i) {
                        hashCode = 31 * hashCode + buffer[i];
                    }

                    // Store hashCode in hexadecimal in buffer
                    termAtt.resizeBuffer(8);
                    termAtt.setLength(8);
                    for (var nibble = 0; nibble < 8; nibble++) {
                        buffer[7 - nibble] = CHARS[(hashCode >> (nibble * 4)) & 0xF];
                    }
                    return true;
                }
                else
                    return false;
            }
        }
    }
}
