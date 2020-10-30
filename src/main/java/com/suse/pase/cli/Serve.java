package com.suse.pase.cli;

import static java.util.stream.Collectors.toList;
import static picocli.CommandLine.Option;
import static spark.Spark.awaitInitialization;
import static spark.Spark.exception;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

import com.github.difflib.unifieddiff.UnifiedDiffParserException;
import com.google.gson.Gson;
import com.suse.pase.PatchParser;
import com.suse.pase.index.IndexSearcher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "serve", description = "Serves the Web interface for searching")
public class Serve implements Callable<Integer> {
    @Option(names = { "-p", "--port" }, paramLabel = "PORT", defaultValue = "4567", description = "TCP port to serve from")
    int port;
    @Parameters(index = "0", paramLabel = "INDEX_PATH", description = "directory with a pase index")
    Path indexPath;

    @Override
    public Integer call() throws Exception {
        serve(port, indexPath);
        return 0;
    }

    public static void serve(int port, Path indexPath) throws Exception {
        Gson gson = new Gson();
        try (var searcher = new IndexSearcher(indexPath)) {
            port(port);
            staticFileLocation("/htdocs");
            post("/search", (req, res) -> {
                // HACK: for testing purposes, allow Javascript from any site to send requests
                res.header("Access-Control-Allow-Origin", "*");

                var patch = req.body();
                var inputStream = new ByteArrayInputStream(patch.getBytes(StandardCharsets.UTF_8));

                var results = PatchParser.parsePatch(inputStream).stream()
                    .map(searcher::search)
                    .flatMap(List::stream)
                    .collect(toList());

                return results;
            }, gson::toJson);

            exception(UnifiedDiffParserException.class, (exception, request, response) -> {
                response.status(400);
                response.body("Unable to parse the patch");
            });

            exception(Exception.class, (exception, request, response) -> {
                exception.printStackTrace();
                throw new RuntimeException(exception);
            });

            awaitInitialization();
            System.out.println("Access the Web UI at http://localhost:" + port);

            // Spark by default runs in separate daemon threads, but we do not
            // want the current thread to die or searcher will be GC'd.
            // Solution: go to sleep
            while(true) {
                Thread.sleep(1000);
            }
        }
    }
}
