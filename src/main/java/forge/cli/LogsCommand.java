package forge.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.regex.*;

@Command(name = "logs", description = "DESCRIPTION LOG COMMAND")
public class LogsCommand implements Runnable {
    static Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(.+?)\\] (INFO|WARN|ERROR|DEBUG) (.+)$"
    );

        @Option(names = {"--file"}, description = "DESCRIPTION FILE INPUT")
        private File filePath;

        @Option(names = {"--check"}, description = "DESCRIPTION CHECK")
        private boolean check = false;
        private String compare;

        @Option(names = {"--level"}, description = "DESCRIPTION LEVEL")
        private String filterLevel;

        @Option(names = {"--stats"}, description = "DESCRIPTION STATS")
        private boolean stats;

        @Override
        public void run() {
            System.out.println("Hello from logs!");
            if(filePath != null) {
            System.out.println("Selected file: " + filePath);
            // readData(filePath);
            }
            if(compare != null) {
                System.out.println("Selected compare: " + compare);
                compareData(filePath, compare);
            }
            if(filterLevel != null) {
                System.out.println("Selected filter level: " + filterLevel);
                filterLogs(filePath, filterLevel);
            }
        }

        public static void readData (File file) {
            try (Scanner myReader = new Scanner(file)) {
                while (myReader.hasNextLine()) {
                    String line = myReader.nextLine();
                    System.out.println(line);
                }
            }
                catch (FileNotFoundException e) {
                    System.out.println("File not found!");
                    // e.printStackTrace();
                }
            }

        public static void compareData (File file, String compare) {
            boolean check = false;
            int lineNumber = 0;

            try (Scanner myReader = new Scanner(file)) {
                while (myReader.hasNextLine()) {
                    String line = myReader.nextLine();
                    if (line.contains(compare)) {
                        check = true;
                        System.out.println("Found comparison " + compare + "in line: " + line);
                    }
                    lineNumber++;
                }
            }
                catch (FileNotFoundException e) {
                    System.out.println("File not found!");
            }
        }

        public static void filterLogs(File file, String filterLevel) {
            List<String> VALID_LEVELS = List.of("INFO", "WARN", "ERROR", "DEBUG");
            if (!VALID_LEVELS.contains(filterLevel.toUpperCase())) {
                System.out.println("Invalid filter level: " + filterLevel);
                System.out.println("Valid levels: " + VALID_LEVELS);
                return;
            }
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = LOG_PATTERN.matcher(line);
                    if (m.matches()) {
                        String date    = m.group(1); // "2024-03-01"
                        String time    = m.group(2); // "08:01:23.345"
                        String thread  = m.group(3); // "http-thread-3"
                        String level   = m.group(4); // "ERROR"
                        if (level.equals(filterLevel.toUpperCase())) {
                            System.out.println(line);
                        }
                        String message = m.group(5); // "com.forge.api.AuthController - Account locked"
                    }
                }
            } catch (IOException e) {
                System.out.println("Error: cannot read file: " + file.getName());
            }
        }
}

