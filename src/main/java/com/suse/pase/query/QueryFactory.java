package com.suse.pase.query;

import static com.github.difflib.patch.DeltaType.CHANGE;
import static com.github.difflib.patch.DeltaType.DELETE;
import static com.github.difflib.patch.DeltaType.EQUAL;
import static java.util.EnumSet.of;
import static java.util.stream.Collectors.toList;

import com.github.difflib.unifieddiff.UnifiedDiffReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class QueryFactory {
    // Mute logging from java-diff-util
    static {
        Logger.getLogger(UnifiedDiffReader.class.getName()).setLevel(Level.SEVERE);
    }

    /** Parses a patch from an input stream and returns queries (one per file) **/
    public static List<PatchTargetQuery> buildPatchTargetQuery(InputStream is) throws IOException {
        var ud = UnifiedDiffReader.parseUnifiedDiff(is);

        return ud.getFiles().stream()
            .filter(f -> !f.getFromFile().equals("/dev/null"))
            .map(file ->{
                var path = file.getFromFile();
                var chunks = file.getPatch().getDeltas().stream()
                        .filter(d -> of(DELETE, EQUAL, CHANGE).contains(d.getType()))
                        .map(d -> d.getSource().getLines().stream().filter(l -> !l.isEmpty()).collect(toList()))
                        .collect(toList());
                return new PatchTargetQuery(path, chunks);
            })
            .collect(toList());
    }

    /** Returns a query object for a file with identical content */
    public static ByContentQuery buildByContentQuery(InputStream is) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(is));
        return new ByContentQuery(reader.lines().collect(Collectors.toList()));
    }
}
