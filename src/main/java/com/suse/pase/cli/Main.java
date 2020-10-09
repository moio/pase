package com.suse.pase.cli;

import static picocli.CommandLine.Command;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@Command(name = "pase", mixinStandardHelpOptions = true, version = "pase 0.1",
        description = "Source code patch search", subcommands = {Index.class, Search.class, Serve.class})
public class Main implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        CommandLine.usage(new Main(), System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
