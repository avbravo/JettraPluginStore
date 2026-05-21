package com.jettrapluginstore;

import com.jettrapluginstore.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (args.length == 0) {
            startInteractiveShell();
            System.exit(0);
        } else {
            int exitCode = executeCommand(args);
            System.exit(exitCode);
        }
    }

    private static int executeCommand(String[] args) {
        // Support for -c <command> syntax requested by the user
        if (args.length > 0 && ("-c".equals(args[0]) || "--command".equals(args[0]))) {
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);
            return new CommandLine(new Main()).execute(newArgs);
        } else {
            return new CommandLine(new Main()).execute(args);
        }
    }

    private static void startInteractiveShell() {
        System.out.println("Jettra Plugin Store Interactive Shell");
        System.out.println("Type 'exit' or 'quit' to close the shell.");
        Scanner scanner = new Scanner(System.in);

        java.io.File credFile = new java.io.File("jettraappstore.md");
        if (!credFile.exists()) {
            System.out.println("\n[Configuración Inicial] No se encontraron credenciales de GitHub.");
            System.out.print("Ingrese su usuario de GitHub: ");
            String user = scanner.nextLine().trim();
            System.out.print("Ingrese su Token de Acceso Personal (PAT) de GitHub: ");
            String pat = scanner.nextLine().trim();
            System.out.print("Cree una frase clave (passphrase) para encriptar estas credenciales localmente: ");
            String secret = scanner.nextLine().trim();
            com.jettrapluginstore.config.CredentialsManager.saveCredentials(user, pat, secret);
            System.out.println("Credenciales guardadas y encriptadas exitosamente.\n");
        }

        while (true) {
            System.out.print("jettrapluginstore> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                break;
            }

            List<String> argList = new ArrayList<>();
            Matcher m = Pattern.compile("([^\"\\s]\\S*|\".+?\")\\s*").matcher(line);
            while (m.find()) {
                argList.add(m.group(1).replace("\"", ""));
            }

            String[] cmdArgs = argList.toArray(new String[0]);
            executeCommand(cmdArgs);
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
