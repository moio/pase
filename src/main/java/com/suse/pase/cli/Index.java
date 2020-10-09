package com.suse.pase.cli;

import com.suse.pase.IndexWriter;
import com.suse.pase.TextFileWalker;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "index", description = "Index a directory of sources")
public class Index implements Callable<Integer> {
    @Parameters(index = "0", paramLabel = "SOURCE_PATH", description = "directory to index")
    Path sourcePath;
    @Parameters(index = "1", paramLabel = "INDEX_PATH", description = "directory where to create the index")
    Path indexPath;

    @Override
    public Integer call() throws Exception {
        index(sourcePath, indexPath);
        return 0;
    }

    public static void index(Path sourcePath, Path indexPath) throws Exception {
        try (var writer = new IndexWriter(indexPath)) {
            new TextFileWalker(sourcePath).withTextFilesIn((path, stream) -> {
                writer.add(path.toString(), stream);
            });
        }
    }
}
