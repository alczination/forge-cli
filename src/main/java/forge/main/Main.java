package forge.main;
import forge.cli.LogsCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(subcommands = {Main.toolCommand.class, Main.helpCommand.class, LogsCommand.class})
public class Main implements Runnable {
    @Override
    public void run() {
        // Default (if no params given)
        System.out.println("Give command line arguments");
    }

    @Command(name = "tool", mixinStandardHelpOptions = true, description = "DESCRIPTION TOOL COMMAND")
    static class toolCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Hello from tool!");
        }
    }

    // Help Command
    @Command(name = "help", description = "DESCRIPTION HELP COMMAND")
    static class helpCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Help command");
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}