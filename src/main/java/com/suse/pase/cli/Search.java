package com.suse.pase.cli;

import static picocli.CommandLine.*;

import com.suse.pase.PatchParser;
import com.suse.pase.QueryResult;
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

    @Parameters(index = "0", paramLabel = "INDEX_PATH", description = "directory with a pase index")
    Path indexPath;

    @Parameters(index = "1", paramLabel = "PATCH_PATH", description = "patch file to search")
    Path patchPath;


    @Override
    public Integer call() throws Exception {
        printResults(search(indexPath, patchPath, explain));
        return 0;
    }

    public static Map<String, List<List<QueryResult>>> search(Path indexPath, Path patchPath, boolean explain) throws Exception {
        try (var searcher = new IndexSearcher(indexPath, explain); var fis = new FileInputStream(patchPath.toString())) {
            return searcher.search(PatchParser.parsePatch(fis));
        }
    }

    private void printResults(Map<String, List<List<QueryResult>>> results) {
        results.keySet().stream()
                .sorted()
                .forEach(path -> {
                    System.out.println(path + ":");

                    var fileResults = results.get(path);
                    if (fileResults.stream().allMatch(chunkResult -> chunkResult.isEmpty())) {
                        System.out.println("   (no results found)");
                    }
                    else {
                        for (int i = 0; i < fileResults.size(); i++) {
                            System.out.printf("  - chunk #%d:\n", i+1);

                            var chunkResults = fileResults.get(i);

                            if (chunkResults.isEmpty()) {
                                System.out.println("      (no results found)");
                            }
                            else {
                                chunkResults.stream().forEach(result -> {
                                    System.out.printf("    - %s (score: %d)\n", result.path , (long)Math.round(result.score));
                                });
                            }
                        }
                    }
                });
    }
}
