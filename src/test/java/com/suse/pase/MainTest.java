package com.suse.pase;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suse.pase.cli.Index;
import com.suse.pase.cli.Search;

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

        Index.index(sourcePath, indexPath, 2);
    }

    @org.junit.jupiter.api.Test
    public void searchTest() throws Exception {
        var patchPath = resourcePath.resolve("patches").resolve("CVE-2017-5638.patch");
        var results = Search.search(indexPath, patchPath);
        assertEquals(3, results.size());

        var first = results.get("core/src/main/java/org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest.java").get(0).get(0);
        assertTrue(first.path.endsWith("multipart/JakartaMultiPartRequest.java"));
        assertEquals(7.901535, first.score, 0.01);

        var second = results.get("core/src/main/java/org/apache/struts2/dispatcher/multipart/JakartaStreamMultiPartRequest.java").get(0).get(0);
        assertTrue(second.path.endsWith("multipart/JakartaStreamMultiPartRequest.java"));
        assertEquals(6.463032, second.score, 0.01);

        var third = results.get("core/src/main/java/org/apache/struts2/dispatcher/multipart/MultiPartRequestWrapper.java").get(0).get(0);
        assertTrue(third.path.endsWith("multipart/MultiPartRequestWrapper.java"));
        assertEquals(8.004337, third.score, 0.01);
    }

    @AfterAll
    static void tear() throws IOException {
        FileUtils.deleteDirectory(indexPath.toFile());
    }
}
