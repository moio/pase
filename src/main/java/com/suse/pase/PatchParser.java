package com.suse.pase;

import static com.github.difflib.patch.DeltaType.CHANGE;
import static com.github.difflib.patch.DeltaType.DELETE;
import static com.github.difflib.patch.DeltaType.EQUAL;
import static java.lang.String.join;
import static java.util.EnumSet.of;
import static java.util.stream.Collectors.toList;

import com.github.difflib.unifieddiff.UnifiedDiffReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatchParser {
    // Mute logging from java-diff-util
    static {
        Logger.getLogger(UnifiedDiffReader.class.getName()).setLevel(Level.SEVERE);
    }

    /** Parses a patch from an input stream and returns queries (one per file) **/
    public static List<FileQuery> parsePatch(InputStream is) throws IOException {
        var ud = UnifiedDiffReader.parseUnifiedDiff(is);

        return ud.getFiles().stream()
            .filter(f -> !f.getFromFile().equals("/dev/null"))
            .map(file ->{
                var path = file.getFromFile();
                var chunks = file.getPatch().getDeltas().stream()
                        .filter(d -> of(DELETE, EQUAL, CHANGE).contains(d.getType()))
                        .map(d -> join("\n", d.getSource().getLines()))
                        .collect(toList());
                return new FileQuery(path, chunks);
            })
            .collect(toList());
    }
}
