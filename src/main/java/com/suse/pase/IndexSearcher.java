package com.suse.pase;

import static com.suse.pase.IndexCommons.PATH_FIELD;
import static com.suse.pase.IndexCommons.SOURCE_FIELD;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import com.suse.pase.IndexCommons.SourceAnalyzer;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class IndexSearcher implements AutoCloseable {
    private final DirectoryReader reader;
    private final org.apache.lucene.search.IndexSearcher searcher;
    private final int HIT_LIMIT = 5;

    public IndexSearcher(Path path) throws IOException {
        reader = DirectoryReader.open(FSDirectory.open(path));
        searcher = new org.apache.lucene.search.IndexSearcher(reader);
    }

    /** Searches the index for a file */
    public List<QueryResult> search(FileQuery fileQuery) {
        return fileQuery.getChunks().stream()
                .map(this::search)
                .flatMap(List::stream)
                .collect(toList());
    }

    /** Searches the index for a chunk */
    private List<QueryResult> search(String chunk) {
        try {
            var analyzer=new SourceAnalyzer();

            var tokens = new LinkedList<String>();
            try (var tokenStream  = analyzer.tokenStream(SOURCE_FIELD, chunk)){
                tokenStream.reset();  // required
                while (tokenStream.incrementToken()) {
                    tokens.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
                }
            }

            var query = tokens.stream()
                    .map(t -> new Term(SOURCE_FIELD, t))
                    .reduce(new PhraseQuery.Builder(), (builder, term) -> builder.add(term), (b1,b2) -> b2)
                    .build();

            var results = searcher.search(query, HIT_LIMIT);

            return stream(results.scoreDocs)
                    .map(scoreDoc -> {
                        try {
                            var doc = searcher.doc(scoreDoc.doc, singleton(PATH_FIELD));
                            var path = doc.getField(PATH_FIELD).stringValue();
                            return new QueryResult(path, scoreDoc.score);
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
