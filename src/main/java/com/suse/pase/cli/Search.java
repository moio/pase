package com.suse.pase.cli;

import static com.suse.pase.query.QueryFactory.*;
import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Option;

import com.suse.pase.query.QueryResult;
import com.suse.pase.index.IndexSearcher;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "search", description = "Search for a patch")
public class Search implements Callable<Integer> {
    @Option(names = { "-e", "--explain" }, paramLabel = "EXPLAIN", defaultValue = "false", description = "Log debug information about scores")
    boolean explain;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    SearchMode mode;

    static class SearchMode {
        @Option(names = { "-c", "--by-content" }, paramLabel = "BY_CONTENT", defaultValue = "false", description = "Search for files similar to the patch itself, rather than files where the patch can be applied")
        boolean byContent;

        @Option(names = { "-a", "--applied-patch" }, paramLabel = "APPLIED", defaultValue = "false", description = "Search for files matching the applied patch")
        boolean appliedPatch;
    }

    @Parameters(index = "0", paramLabel = "INDEX_PATH", description = "directory with a pase index")
    Path indexPath;

    @Parameters(index = "1", paramLabel = "PATCH_PATH", description = "patch file to search")
    Path patchPath;


    @Override
    public Integer call() throws Exception {
        boolean byContent = false;
        boolean appliedPatch = false;
        if (mode != null) {
            byContent = mode.byContent;
            appliedPatch = mode.appliedPatch
        }
        printResults(search(indexPath, patchPath, explain, byContent, appliedPatch));
        return 0;
    }

    public static Map<String, List<QueryResult>> search(Path indexPath, Path patchPath, boolean explain, boolean byContent, boolean appliedPatch) throws Exception {
        var pathString = patchPath.toString();
        try (var searcher = new IndexSearcher(indexPath, explain); var fis = new FileInputStream(pathString)) {
            if (byContent) {
                return Map.of(pathString, searcher.search(buildByContentQuery(fis)));
            }
            else if (appliedPatch) {
                return searcher.search(buildAppliedPatchTargetQuery(fis));
            } else {
                return searcher.search(buildPatchTargetQuery(fis));
            }
        }
    }

    private void printResults(Map<String, List<QueryResult>> results) {
        results.keySet().stream()
                .sorted()
                .forEach(path -> {
                    System.out.println(path + ":");

                    var fileResults = results.get(path);
                    if (fileResults.isEmpty()) {
                        System.out.println("   (no results found)");
                    }
                    else {
                        fileResults.forEach(result -> {
                            System.out.printf("    - %s (score: %d)\n", result.path , (long)Math.round(result.score));
                        });
                    }
                });
    }
}
