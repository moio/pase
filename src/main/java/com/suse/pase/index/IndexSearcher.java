package com.suse.pase.index;

import static com.suse.pase.index.IndexCommons.PATH_FIELD;
import static com.suse.pase.index.IndexCommons.SOURCE_FIELD;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import com.suse.pase.FileQuery;
import com.suse.pase.QueryResult;
import com.suse.pase.index.IndexCommons.SourceAnalyzer;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Encapsulates Lucene details about searching indexes */
public class IndexSearcher implements AutoCloseable {

    /** Maximum number of results per file */
    private final int HIT_LIMIT = 50;

    /**
     * Document frequency cutoff percentage, this is used to filter queries with over-popular results.
     *
     * We define a "cutoff score" as the score of a file with n lines, all of which are present in 10% of the indexed
     * files or more.
     *
     * Any result scoring worse than that will be cut off.
     */
    private static final double P = 0.1;

    private static Logger LOG = Logger.getLogger(IndexSearcher.class.getName());
    private final DirectoryReader reader;
    private final org.apache.lucene.search.IndexSearcher searcher;
    private final boolean explain;
    private final double minTermScore;

    public IndexSearcher(Path path, boolean explain) throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(path));
        this.searcher = new org.apache.lucene.search.IndexSearcher(reader);
        // what we really want here is IDF, without any term frequency part
        // setting k1 = 0 (and b to any value) simplifies the formula to a pure IDF
        // https://en.wikipedia.org/wiki/Okapi_BM25
        this.searcher.setSimilarity(new BM25Similarity(0,0));
        this.explain = explain;

        // Calculate the score for a term present in P% of the documents
        // N is the total number of documents in the index
        var N = searcher.getIndexReader().getDocCount(PATH_FIELD);
        // now, per definition of BM25 with k1=0 we have
        // IDF=ln(((N-n) + 0.5) / (N + 0.5) +1)
        this.minTermScore = Math.log((N - P * N + 0.5)/ (P * N + 0.5) + 1);
    }

    /** Searches the index for a file
     * @return a map from filename (as specified in the patch) to list of results
     */
    public Map<String, List<QueryResult>> search(List<FileQuery> fileQueries) {
        return fileQueries.stream()
                .collect(toMap(
                        fq -> fq.getFile(),
                        fq -> searchImpl(fq.getChunks())
                ));
    }

    /** Searches the index for a patch (list of chunks */
    private List<QueryResult> searchImpl(List<String> chunks) {
        try {
            var tokens = tokenize(chunks);
            var termCount = tokens.stream().mapToInt(Collection::size).sum();

            var query = buildQuery(tokens);

            var results = searcher.search(query, HIT_LIMIT);

            return stream(results.scoreDocs)
                    .map(scoreDoc -> {
                        try {
                            var doc = searcher.doc(scoreDoc.doc, singleton(PATH_FIELD));
                            var path = doc.getField(PATH_FIELD).stringValue();
                            if (explain) {
                                LOG.info("Score explanation for: " + path + " (score: " + scoreDoc.score + ")");
                                LOG.info(searcher.explain(query, scoreDoc.doc).toString());
                            }
                            return new QueryResult(path, scoreDoc.score);
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(r -> r.score > minTermScore * termCount)
                    .collect(toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<List<String>> tokenize(List<String> chunks) {
        return chunks.stream()
                .map(this::tokenize)
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String chunk) {
        var analyzer = new SourceAnalyzer();

        var tokens = new LinkedList<String>();
        try {
            try (var tokenStream  = analyzer.tokenStream(SOURCE_FIELD, chunk)){
                tokenStream.reset();  // required
                while (tokenStream.incrementToken()) {
                    tokens.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
                }
            }
        }
        catch (IOException e) {
            // cannot really happen, as it's all in-memory operations
        }
        return tokens;
    }

    private Query buildQuery(List<List<String>> chunks) {
        // a query for a file SHOULD contain every chunk
        return (Query) chunks.stream()
                .map(this::buildChunkQuery)
                .reduce(new BooleanQuery.Builder(), (builder, query1) -> builder.add(query1, SHOULD), (b1, b2) -> b2)
                .build();
    }

    private Query buildChunkQuery(List<String> tokens) {
        // a query for a chunk MUST contain every line (token)
        return tokens.stream()
                .map(t -> new Term(SOURCE_FIELD, t))
                .reduce(new PhraseQuery.Builder(), (builder, term) -> builder.add(term), (b1,b2) -> b2)
                .build();
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
