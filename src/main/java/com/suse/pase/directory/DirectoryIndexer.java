package com.suse.pase.directory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.suse.pase.index.IndexWriter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
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
        new DirectoryWalker(root).walkFiles((path, stream) -> {
            var fingerprint = fingerprint(path);
            if (isText(path, stream)) {
                index.add(path.toString(), fingerprint, of(stream));
            }
            else if (recursionLimit >=1) {
                if (index.add(path.toString(), fingerprint, empty())) {
                    indexArchive(path, fingerprint, stream, recursionLimit);
                }
            }
        });
    }

    private void indexArchive(Path archivePath, String fingerprint, BufferedInputStream archiveStream, int recursionLevel) {
        new ArchiveWalker(archivePath, archiveStream).walkArchiveFiles((path, stream) -> {
            if (isText(path, stream)) {
                index.add(path.toString(), fingerprint, of(stream));
            }
            else if (recursionLevel > 1) {
                indexArchive(path, fingerprint, stream, recursionLevel - 1);
            }
        });
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
