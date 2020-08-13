import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class SourceCodeTokenizer extends CharTokenizer {
    public SourceCodeTokenizer() {
        super(DEFAULT_TOKEN_ATTRIBUTE_FACTORY, 1024);
    }

    @Override
    protected boolean isTokenChar(int cp) {
        return Character.getType(cp) != Character.LINE_SEPARATOR;
    }
}

class SourceAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new SourceCodeTokenizer());
    }
}

class PathAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // U+002F / SOLIDUS
        Tokenizer tok = CharTokenizer.fromSeparatorCharPredicate(cp -> cp == 0x2F);
        return new TokenStreamComponents(tok);
    }
}

public class Main {
    public static void main(String[] args) throws IOException {

        var sourcePath = ".";
        var indexPath = "index";
        var buffer = 256;

        var analyzer = new PerFieldAnalyzerWrapper(
            new StandardAnalyzer(), // fallback
            Map.of(
                    "path", new PathAnalyzer(),
                    "source", new SourceAnalyzer()
            ));

        var dir = FSDirectory.open(Paths.get(indexPath));

        var config = new IndexWriterConfig(analyzer)
                .setOpenMode(CREATE_OR_APPEND)
                .setRAMBufferSizeMB(buffer);

        try (var writer = new IndexWriter(dir, config)) {
            visitFilesIn(Path.of(sourcePath), path ->{
                indexFile(writer, path);
            });
        }
    }

    static void visitFilesIn(Path path, Consumer<Path> consumer) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                consumer.accept(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Indexes a single document */
    static void indexFile(IndexWriter writer, Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            // make a new, empty document
            Document doc = new Document();

            // Add the path of the file as a field named "path"
            // This is indexed (i.e. searchable), but not tokenized
            Field pathField = new StringField("path", file.toString(), Field.Store.YES);
            doc.add(pathField);

            // Add the contents of the file to a field named "source"
            doc.add(new TextField("source", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            // Create or update if existing
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
