package forge.cli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
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

    @Option(names = {"--check"}, description = "Perform a validation check on the log file structure.")
    private boolean check = false;
    private String compare;

    @Option(names = {"--level", "-l"}, description = "Filter logs by severity level (e.g. INFO, WARN, ERROR, DEBUG)")
    private String filterLevel;

    @Option(names = {"--exclude", "-x"}, description = "Exclude logs by a specific severity level.")
    private String filterExclude;

    @Option(names = {"--grep"}, description = "Filter logs containing a specific phrase or keyword.")
    private String filterGrep;

    @Option(names = {"--since", "-t"}, description = "Filter logs after a specific timestamp. Formats: 'yyyy-MM-dd' or 'yyyy-MM-ddTHH:mm:ss'")
    private String since;

    @Option(names = {"--until", "-u"}, description = "Filter logs up to a specific timestamp. Formats: 'yyyy-MM-dd' or 'yyyy-MM-ddTHH:mm:ss'.")
    private String until;

    @Option(names = {"--output", "--out"}, description = "Path to the file where the filtered logs will be saved.")
    private String output;

    @Option(names = {"--stats"}, description = "DESCRIPTION STATS")
    private boolean stats = false;

    @Override
    public void run() {
        if (filePath != null)
            System.out.println("Selected file: " + filePath); // Debug
        if (compare != null) {
            System.out.println("Selected compare: " + compare); // Debug
            compareData(filePath, compare);
        }
        if (stats) {
            statsLogs(filePath);
            return;
        }
        List<String> filteredData = filterLogs(filePath);
        if (output != null && !filteredData.isEmpty()) {
            saveOutput(filteredData, output);
        }
    }

//    public static void readData(File file) {
//        try (Scanner myReader = new Scanner(file)) {
//            while (myReader.hasNextLine()) {
//                String line = myReader.nextLine();
//                System.out.println(line);
//            }
//        } catch (FileNotFoundException e) {
//            System.err.println("File not found!");
//            e.printStackTrace();
//        }
//    }

    public static void compareData(File file, String compare) {
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
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e1) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            } catch (Exception e2) {
                System.err.println("Error: invalid time format: " + dateStr + "'. Use 'yyyy-MM-ddTHH:mm:ss' or 'yyyy-MM-dd'.");
                return null;
            }
        }
    }

    private List<String> filterLogs(File file) {
        List<String> filteredData = new ArrayList<>();
        LocalDateTime sinceDateTime = parseDate(since);
        LocalDateTime untilDateTime = parseDate(until);

        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = LOG_PATTERN.matcher(line);
                if (!m.matches()) continue;
                String thread = m.group(3);
                String level = m.group(4);
                String message = m.group(5);
                LocalDateTime logDateTime = LocalDateTime.parse(m.group(1) + " " + m.group(2), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

                if (sinceDateTime != null && logDateTime.isBefore(sinceDateTime)) continue;
                if (untilDateTime != null && logDateTime.isAfter(untilDateTime)) continue;
                if (filterExclude != null && level.equalsIgnoreCase(filterExclude)) continue;
                if (filterLevel != null && !level.equalsIgnoreCase(filterLevel)) continue;
                if (filterGrep != null && !line.contains(filterGrep)) continue;

                String color = switch (level.toUpperCase()) {
                    case "ERROR", "FATAL" -> "red";
                    case "WARN" -> "yellow";
                    case "DEBUG", "TRACE" -> "faint,italic";
                    default -> "green";
                };
                System.out.println(picocli.CommandLine.Help.Ansi.AUTO.string("@|" + color + " " + line + "|@"));
                filteredData.add(line);
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

    public static void statsLogs(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {

            String line;
            int count = 0;
            int info_counter = 0;
            int err_counter = 0;
            int warn_counter = 0;
            int debug_counter = 0;

            while ((line = reader.readLine()) != null) {
                count++;
                Matcher m = LOG_PATTERN.matcher(line);
                if (!m.matches()) continue;
                String thread = m.group(3);
                String level = m.group(4);
                if (level.equals("INFO")) info_counter++;
                if (level.equals("ERROR")) err_counter++;
                if (level.equals("WARN")) warn_counter++;
                if (level.equals("DEBUG")) debug_counter++;
                String message = m.group(5);
            }

            System.out.println("Total lines: " + count);
            System.out.println("------------");
            System.out.println("INFO: " + info_counter);
            System.out.println("ERROR: " + err_counter);
            System.out.println("WARN: " + warn_counter);
            System.out.println("DEBUG: " + debug_counter);
            System.out.println("------------");
            System.out.println("First entry: ");
            System.out.println("Last entry: ");

            } catch (IOException e) {
                System.err.println("Error: cannot read file: " + file.getName());
            }
        }
}
