package com.suse.pase;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 3){
            printHelp();
        }

        var sourcePath = Path.of(args[1]);
        var indexPath = Path.of(args[2]);

        try (var writer = new IndexWriter(indexPath)) {
            DirectoryWalker.forEachTextFileIn(sourcePath, path -> {
                writer.add(path);
            });
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar pase.jar index <source_path> <index_path>");
        System.exit(-1);
    }
}
