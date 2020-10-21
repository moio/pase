package com.suse.pase.directory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.google.common.base.Stopwatch;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.suse.pase.index.IndexWriter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Walks a directory (recursively), indexing any text file.
 * Also walks any archives found in the directory, up to a specified recursion level of nested archives,
 * and indexes any included text files as well.
 */
public class DirectoryIndexer {
    private static Logger LOG = Logger.getLogger(DirectoryIndexer.class.getName());
    private final Path root;
    private final int recursionLimit;
    private final IndexWriter index;
    private final AtomicInteger processedFiles = new AtomicInteger();
    private final AtomicInteger processedFilesInArchives = new AtomicInteger();
    private final AtomicInteger updatedFiles = new AtomicInteger();
    private final AtomicInteger updatedFilesInArchives = new AtomicInteger();

    public DirectoryIndexer(Path path, int recursionLimit, IndexWriter index) {
        this.root = path;
        this.recursionLimit = recursionLimit;
        this.index = index;
    }

    /**
     * Indexes all text files in the directory, and also any text file in any archive found in the directory,
     * up to the specified recursion level of nested archives.
     */
    public void index() {
        Stopwatch timer = Stopwatch.createStarted();
        new DirectoryWalker(root).walkFiles((path, stream) -> {
            var fingerprint = fingerprint(path);
            if (isText(path, stream)) {
                if(index.add(path.toString(), fingerprint, of(stream))){
                    logAdvancement(false, true);
                };
            }
            else if (recursionLimit >=1) {
                if (index.add(path.toString(), fingerprint, empty())) {
                    indexArchive(path, fingerprint, stream, recursionLimit);
                }
            }
            logAdvancement(false, false);
        });
        var endTime = System.currentTimeMillis();

        LOG.info("Indexing completed!");
        LOG.info("Total processed files: " + processedFiles.get());
        LOG.info("  - of which in archives: " + processedFilesInArchives.get());
        LOG.info("Updated text files in the index: " + updatedFiles.get());
        LOG.info("  - of which in archives: " + updatedFilesInArchives.get());
        LOG.info("Indexing time: " + timer.stop());
    }

    private void indexArchive(Path archivePath, String fingerprint, BufferedInputStream archiveStream, int recursionLevel) {
        new ArchiveWalker(archivePath, archiveStream).walkArchiveFiles((path, stream) -> {
            if (isText(path, stream)) {
                if (index.add(path.toString(), fingerprint, of(stream))) {
                    logAdvancement(true, true);
                }
            }
            else if (recursionLevel > 1) {
                indexArchive(path, fingerprint, stream, recursionLevel - 1);
            }
            logAdvancement(true, false);
        });
    }

    private void logAdvancement(boolean inArchive, boolean updated) {
        if (inArchive && updated) {
            updatedFilesInArchives.incrementAndGet();
        }
        if (inArchive && !updated) {
            processedFilesInArchives.incrementAndGet();
        }
        if (!inArchive && updated) {
            updatedFiles.incrementAndGet();
        }
        if (!inArchive && !updated) {
            var total = processedFiles.incrementAndGet();
            if (total > 0 && total % 1000 == 0) {
                LOG.info("Total files processed so far: " + processedFiles.get());
            }
        }
    }

    /** Returns a string that changes when the file pointed by path changes */
    private String fingerprint(Path path) {
        // we assume contents changed based on filesystem metadata,
        // see rationale in https://apenwarr.ca/log/20181113
        try {
            Hasher hasher = Hashing.sha256().newHasher();
            var attributes = Files.readAttributes(path, PosixFileAttributes.class);

            // mtime
            hasher.putLong(attributes.lastModifiedTime().toMillis());
            // size
            hasher.putLong(attributes.size());
            // inode
            hasher.putString(attributes.fileKey().toString(), UTF_8);
            // mode
            for (var p : PosixFilePermission.values()){
                hasher.putBoolean(attributes.permissions().contains(p));
            }
            // owner
            hasher.putInt(attributes.owner().hashCode());
            // owner group
            hasher.putInt(attributes.group().hashCode());

            return hasher.hash().toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isText(Path path, BufferedInputStream stream) {
        // same heuristic used by diff
        // https://dev.to/sharkdp/what-is-a-binary-file-2cf5

        byte[] buf = new byte[4096];
        stream.mark(buf.length);
        try {
            var count = stream.read(buf, 0, buf.length);
            for (int i = 0; i < count; i++) {
                if (buf[i] == 0) {
                    return false;
                }
            }
            return true;
        }
        catch (IOException e) {
            LOG.warning("I/O error during text file check on " + path + ", skipping");
            return false;
        }
        finally {
            try {
                stream.reset();
            }
            catch (IOException e) {
                // cannot happen in BufferedInputStream with mark >= 0
            }
        }
    }
}
