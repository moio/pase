package com.suse.pase.cli;

import com.suse.pase.index.IndexWriter;
import com.suse.pase.directory.DirectoryIndexer;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "index", description = "Index a directory of sources")
public class Index implements Callable<Integer> {
    @Option(names = { "-r", "--recursion-limit" }, paramLabel = "RECURSION_LIMIT", defaultValue = "2", description = "the number of nested archives to unpack")
    int recursionLimit;
    @Parameters(index = "0", paramLabel = "SOURCE_PATH", description = "directory to index")
    Path sourcePath;
    @Parameters(index = "1", paramLabel = "INDEX_PATH", description = "directory where to create the index")
    Path indexPath;

    @Override
    public Integer call() throws Exception {
        index(sourcePath, indexPath, recursionLimit);
        return 0;
    }

    public static void index(Path sourcePath, Path indexPath, int recursionLimit) throws Exception {
        try (var writer = new IndexWriter(indexPath)) {
            new DirectoryIndexer(sourcePath, recursionLimit, writer).index();
        }
    }
}
