package com.suse.pase.directory;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.eclipse.packagedrone.utils.rpm.parse.RpmInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/** Walks an archive, allowing a consumer to read any file inside of it */
public class ArchiveWalker {

    private static final List<String> TAR_BZIP2_EXTENSIONS = List.of(".tar.bz2", ".tb2", ".tbz", ".tbz2", ".tz2");
    private static final List<String> TAR_GZIP_EXTENSIONS = List.of(".tar.gz", ".taz", ".tgz");

    private static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static Logger LOG = Logger.getLogger(ArchiveWalker.class.getName());

    private final Path path;
    private final InputStream stream;

    public ArchiveWalker(Path path, InputStream stream){
        this.path = path;
        this.stream = stream;
    }

    /** Calls the consumer for all files in the archive. The consumer receives each file's path and a stream of its bytes. */
    public void walkArchiveFiles(BiConsumer<Path, BufferedInputStream> consumer) {
        getArchiveInputStream().ifPresent(ais -> {
                try {
                    ArchiveEntry entry;
                    while ((entry = ais.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            var path = this.path.resolve(entry.getName());
                            var stream = new BufferedInputStream(ais, BUFFER_SIZE);
                            consumer.accept(path, stream);
                        }
                    }
                }
                catch (IOException e) {
                    LOG.warning("Could not decompress entry from archive (unexpected format?): " + path);
                }
            });
    }

    public Optional<ArchiveInputStream> getArchiveInputStream() {
        try {
            if (path.toString().endsWith(".rpm")) {
                var rpmStream = new RpmInputStream(stream);
                rpmStream.getPayloadHeader(); // inits cpio stream
                var stream = rpmStream.getCpioStream();
                return of(stream);
            }
            if (TAR_BZIP2_EXTENSIONS.stream().anyMatch(ext -> path.toString().endsWith(ext))) {
                var compressorStream = new CompressorStreamFactory().createCompressorInputStream("bzip2", stream);
                var archiveStream = new ArchiveStreamFactory().createArchiveInputStream("tar", compressorStream);
                return of(archiveStream);
            }
            if (TAR_GZIP_EXTENSIONS.stream().anyMatch(ext -> path.toString().endsWith(ext))) {
                var compressorStream = new CompressorStreamFactory().createCompressorInputStream("gz", stream);
                var archiveStream = new ArchiveStreamFactory().createArchiveInputStream("tar", compressorStream);
                return of(archiveStream);
            }
            if (path.toString().endsWith(".tar.xz")) {
                var compressorStream = new CompressorStreamFactory().createCompressorInputStream("xz", stream);
                var archiveStream = new ArchiveStreamFactory().createArchiveInputStream("tar", compressorStream);
                return of(archiveStream);
            }
            if (path.toString().endsWith(".zip")) {
                return of(new ArchiveStreamFactory().createArchiveInputStream("zip", stream));
            }
            if (path.toString().endsWith(".obscpio")) {
                return of(new ArchiveStreamFactory().createArchiveInputStream("cpio", stream));
            }
        }
        catch (Exception e) {
            LOG.warning("Could not decompress archive (unexpected format?): " + path);
        }
        return empty();
    }
}
