package forge.cli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.*;

@Command(name = "logs", description = "View, filter and analyze application log files.")
public class LogsCommand implements Runnable {
    static Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(.+?)\\] (INFO|WARN|ERROR|DEBUG) (.+)$"
    );
        @Option(names = {"--file"}, required = true, description = "The path to the log file to be processed.")
        private File filePath;

        @Option(names = {"--check"}, description = "Perform a validation check on the log file structure")
        private boolean check = false;
        private String compare;

        @Option(names = {"--level", "-l"}, description = "Filter logs by severity level (e.g. INFO, WARN, ERROR, DEBUG)")
        private String filterLevel;

        @Option(names = {"--exclude", "-x"}, description = "DESCRIPTION EXCLUDE")
        private String filterExclude;

        @Option(names = {"--grep"}, description = "DESCRIPTION GREP")
        private String filterGrep;

        @Option(names = {"--since", "-t"}, description = "Filter logs after a specific timestamp. Formats: 'yyyy-MM-dd' or 'yyyy-MM-ddTHH:mm:ss'")
        private String since;

        @Option(names = {"--until", "-u"}, description = "DESCRIPTION UNTIL")
        private String until;

        @Option(names = {"--output", "--out"}, description = "DESCRIPTION OUTPUT")
        private String output;

        @Override
        public void run() {
            if (filePath != null)
                System.out.println("Selected file: " + filePath); // Debug
            if (compare != null) {
                System.out.println("Selected compare: " + compare); // Debug
                compareData(filePath, compare);
            }
            if (output != null) {
                List<String> filteredData = filterLogs(filePath, filterLevel, since);
                saveOutput(filteredData, output);
            }
            else if (filterLevel != null || since != null) {
                // Debug
                System.out.println("Selected filter level: " + filterLevel);
                filterLogs(filePath, filterLevel, since);
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
                    System.err.println("File not found!");
                    e.printStackTrace();
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

        public static List<String> filterLogs(File file, String filterLevel, String since) {
            List<String> filteredData = new ArrayList<>();
            LocalDateTime sinceDateTime = null;
            if (since != null) {
                try {
                        // Date + Hour
                    sinceDateTime = LocalDateTime.parse(since, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                } catch (Exception e1) {
                    try {
                        // Date
                        LocalDate date = LocalDate.parse(since, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        sinceDateTime = date.atStartOfDay();
                    }
                         catch (Exception e2) {
                            System.err.println("Error: invalid time format. Use 'yyyy-MM-ddTHH:mm:ss' or 'yyyy-MM-dd'.'");
                            return filteredData;
                        }
                    }
                }
            List<String> VALID_LEVELS = List.of("INFO", "WARN", "ERROR", "DEBUG");
            if (filterLevel != null && !VALID_LEVELS.contains(filterLevel.toUpperCase())) {
                System.err.println("Invalid filter level: " + filterLevel);
                System.err.println("Valid levels: " + VALID_LEVELS);
                return filteredData;
            }
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = LOG_PATTERN.matcher(line);
                    if (m.matches()) {
                        String thread  = m.group(3);
                        String level   = m.group(4);
                        if (sinceDateTime != null) {
                            LocalDateTime logDateTime = LocalDateTime.parse(m.group(1) + " " + m.group(2), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                            if (logDateTime.isBefore(sinceDateTime)) continue;
                        }
                        if (filterLevel == null || level.equals(filterLevel.toUpperCase())) {
                            String color = switch (level.toUpperCase()) {
                                case "ERROR" -> "red";
                                case "WARN" -> "yellow";
                                case "DEBUG" -> "faint,italic";
                                default -> "green";
                            };
                            System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|" + color + " " + line + "|@"));
                            filteredData.add(line);
                        }
                        String message = m.group(5);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error: cannot read file: " + file.getName());
            }
            return filteredData;
        }

        public static void saveOutput(List<String> filteredData, String fileName) {
            try (FileOutputStream fos = new FileOutputStream(fileName);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                String data = String.join(System.lineSeparator(), filteredData);
                byte[] bytes = data.getBytes();
                bos.write(bytes);
                // Debug
                System.out.println("Data written to file sucessfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}

