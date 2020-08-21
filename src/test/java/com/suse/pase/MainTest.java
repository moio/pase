package com.suse.pase;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An integration tests that indexes and then searches in a code directory.
 */
public class MainTest {
    private static Path resourcePath;
    private static Path indexPath;

    @BeforeAll
    static void setup() throws Exception {
        // get path to test/resources/sources
        resourcePath = Path.of(ClassLoader.getSystemResource(".").toURI().getPath());
        var sourcePath = resourcePath.resolve("sources");
        // get path in random temporary location
        indexPath = createTempDirectory(MainTest.class.getCanonicalName());

        Main.index(sourcePath.toString(), indexPath.toString());
    }

    @org.junit.jupiter.api.Test
    public void searchTest() throws Exception {
        var patchPath = resourcePath.resolve("patches").resolve("CVE-2017-5638.patch");
        var results = Main.search(indexPath.toString(), patchPath.toString());
        assertEquals(3, results.size());
    }

    @AfterAll
    static void tear() throws IOException {
        FileUtils.deleteDirectory(indexPath.toFile());
    }
}
