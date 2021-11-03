package com.suse.pase.query;

import static com.github.difflib.patch.DeltaType.*;
import static java.util.stream.Collectors.toList;

import com.github.difflib.patch.DeltaType;
import com.github.difflib.unifieddiff.UnifiedDiffReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class QueryFactory {
    // Mute logging from java-diff-util
    static {
        Logger.getLogger(UnifiedDiffReader.class.getName()).setLevel(Level.SEVERE);
    }

    /** Parses a patch from an input stream and returns queries (one per file). The target argument controls
     * whether to search for the unapplied (target=false) patch sources or for the applied (target=true) ones. **/
    public static List<PatchQuery> buildPatchQuery(InputStream is, boolean target) throws IOException {
        var ud = UnifiedDiffReader.parseUnifiedDiff(is);
        EnumSet<DeltaType> dt = target ? EnumSet.of(INSERT, EQUAL, CHANGE) : EnumSet.of(DELETE, EQUAL, CHANGE);
        return ud.getFiles().stream()
            .filter(f -> !f.getFromFile().equals("/dev/null"))
            .map(file ->{
                var path = file.getFromFile();
                var chunks = file.getPatch().getDeltas().stream()
                        .filter(d -> dt.contains(d.getType()))
                        .map(d -> (target ? d.getTarget() : d.getSource()).getLines().stream().filter(l -> !l.isEmpty()).collect(toList()))
                        .collect(toList());
                return new PatchQuery(path, chunks);
            })
            .collect(toList());
    }

    /** Parses a patch from an input stream and returns queries for the unapplied context of it **/
    public static List<PatchQuery> buildUnappliedPatchQuery(InputStream is) throws IOException {
        return buildPatchQuery(is, false);
    }

    /** Parses a patch from an input stream and returns queries for the applied context of it **/
    public static List<PatchQuery> buildAppliedPatchQuery(InputStream is) throws IOException {
        return buildPatchQuery(is, true);
    }

    /** Returns a query object for a file with identical content */
    public static ByContentQuery buildByContentQuery(InputStream is) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(is));
        return new ByContentQuery(reader.lines().collect(Collectors.toList()));
    }
}
