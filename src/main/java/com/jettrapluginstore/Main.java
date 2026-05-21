package com.jettrapluginstore;

import com.jettrapluginstore.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "jettrapluginstore", mixinStandardHelpOptions = true, version = "1.0",
        description = "Jettra Plugin Store CLI. Convert, install, and manage Jettra plugins.",
        subcommands = {
                PreparePluginCommand.class,
                CreatePluginCommand.class,
                ListPluginsCommand.class,
                InstallPluginCommand.class,
                RemovePluginCommand.class
        })
public class Main implements Callable<Integer> {

    @Option(names = {"-c", "--command"}, required = false, description = "Command to run (legacy format support). If provided, it overrides the subcommand.")
    private String legacyCommand;

    public static void main(String... args) {
        // Support for -c <command> syntax requested by the user
        if (args.length > 0 && ("-c".equals(args[0]) || "--command".equals(args[0]))) {
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            int exitCode = new CommandLine(new Main()).execute(newArgs);
            System.exit(exitCode);
        } else {
            int exitCode = new CommandLine(new Main()).execute(args);
            System.exit(exitCode);
        }
    }

    @Override
    public Integer call() throws Exception {
        // If a command was passed via -c but not handled by rewriting args
        if (legacyCommand != null) {
            System.out.println("Executing legacy command: " + legacyCommand);
            // We handled this in main() by stripping -c, so this shouldn't be reached if -c is the first arg.
        } else {
            new CommandLine(this).usage(System.out);
        }
        return 0;
    }
}
