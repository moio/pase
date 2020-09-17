package com.suse.pase;

import static java.util.stream.Collectors.toList;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 3){
            printHelp();
        }

        switch (args[0]) {
            case "index": index(args[1], args[2]); break;
            case "search": printResults(search(args[1], args[2])); break;
            default: printHelp();
        }
    }


    private static void printHelp() {
        System.out.println("To index a directory:  java -jar pase.jar index <source_path> <index_path>");
        System.out.println("To search for a patch: java -jar pase.jar search <index_path> <patch_path>");
        System.exit(-1);
    }

    public static void index(String sourcePath, String indexPath) throws Exception {
        try (var writer = new IndexWriter(Path.of(indexPath))) {
            DirectoryWalker.forEachTextFileIn(Path.of(sourcePath), path -> {
                try (InputStream stream = Files.newInputStream(path)) {
                    writer.add(path.toString(), stream);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static List<QueryResult> search(String indexPath, String patchPath) throws Exception {
        try (var searcher = new IndexSearcher(Path.of(indexPath)); var fis = new FileInputStream(patchPath)) {
            return PatchParser.parsePatch(fis).stream()
                    .map(searcher::search)
                    .flatMap(List::stream)
                    .collect(toList());
        }
    }
    private static void printResults(List<QueryResult> results) {
        for (var result:results) {
            System.out.println(result);
        }
    }
}
