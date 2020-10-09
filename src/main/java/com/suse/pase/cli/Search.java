package com.suse.pase.cli;

import static java.util.stream.Collectors.toList;

import com.suse.pase.IndexSearcher;
import com.suse.pase.PatchParser;
import com.suse.pase.QueryResult;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "search", description = "Search for a patch")
public class Search implements Callable<Integer> {
    @Parameters(index = "0", paramLabel = "INDEX_PATH", description = "directory with a pase index")
    Path indexPath;

    @Parameters(index = "1", paramLabel = "PATCH_PATH", description = "patch file to search")
    Path patchPath;

    @Override
    public Integer call() throws Exception {
        printResults(search(indexPath, patchPath));
        return 0;
    }

    public static List<QueryResult> search(Path indexPath, Path patchPath) throws Exception {
        try (var searcher = new IndexSearcher(indexPath); var fis = new FileInputStream(patchPath.toString())) {
            return PatchParser.parsePatch(fis).stream()
                    .map(searcher::search)
                    .flatMap(List::stream)
                    .collect(toList());
        }
    }

    private void printResults(List<QueryResult> results) {
        for (var result:results) {
            System.out.println(result);
        }
    }
}
