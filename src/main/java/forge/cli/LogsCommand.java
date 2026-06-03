package forge.cli;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

@Command(name = "logs", description = "View, filter and analyze application log files.")
public class LogsCommand implements Runnable {
    static Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(.+?)\\] (INFO|WARN|ERROR|DEBUG) (.+)$"
    );

    @Option(names = {"--file", "-f"}, required = true, description = "The path to the log file to be processed.")
    private File filePath;

    @Option(names = {"--level", "-l"}, description = "Filter logs by severity level (e.g. INFO, WARN, ERROR, DEBUG)")
    private String filterLevel;

    @Option(names = {"--exclude", "-x"}, description = "Exclude logs by a specific severity level.")
    private String filterExclude;

    @Option(names = {"--grep", "-gr"}, description = "Filter logs containing a specific phrase or keyword.")
    private String filterGrep;

    @Option(names = {"--since", "-t"}, description = "Filter logs after a specific timestamp. Formats: 'yyyy-MM-dd' or 'yyyy-MM-ddTHH:mm:ss'")
    private String since;

    @Option(names = {"--until", "-u"}, description = "Filter logs up to a specific timestamp. Formats: 'yyyy-MM-dd' or 'yyyy-MM-ddTHH:mm:ss'.")
    private String until;

    @Option(names = {"--output", "-out"}, description = "Path to the file where the filtered logs will be saved.")
    private String output;

    @Option(names = {"--stats"}, description = "Generate and display a summary report of log severities and timestamps.")
    private boolean stats;

    @Override
    public void run() {
        if (stats) {
            List<String> statsList = statsLogs(filePath);
            if (output != null) {
                saveOutput(statsList, output);
            } else {
                System.out.println("--------");
                for (String s : statsList) {
                    System.out.println(s);
                }
                System.out.println("--------");
            }
        return;
    }
        List<String> filteredData = filterLogs(filePath);
        if (output != null && !filteredData.isEmpty()) {
            saveOutput(filteredData, output);
        }
    }

//    public static void compareData(File file, String compare) {
//        boolean check = false;
//        int lineNumber = 0;
//
//        try (Scanner myReader = new Scanner(file)) {
//            while (myReader.hasNextLine()) {
//                String line = myReader.nextLine();
//                if (line.contains(compare)) {
//                    check = true;
//                    System.out.println("Found comparison " + compare + "in line: " + line);
//                }
//                lineNumber++;
//            }
//        } catch (FileNotFoundException e) {
//            System.out.println("File not found!");
//        }
//    }

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
                System.out.println(CommandLine.Help.Ansi.AUTO.string("@|" + color + " " + line + "|@"));
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

    public List<String> statsLogs(File file) {
        List<String> statsData = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            String firstLine = null;
            String lastLine = null;
            int count = 0;
            int info_counter = 0;
            int err_counter = 0;
            int warn_counter = 0;
            int debug_counter = 0;

            while ((line = reader.readLine()) != null) {
                count++;
                Matcher m = LOG_PATTERN.matcher(line);
                if (!m.matches()) continue;
                String timestamp = m.group(1) + " " + m.group(2);
                if (firstLine == null) firstLine = timestamp;
                lastLine = timestamp;
                LocalDateTime logDateTime = LocalDateTime.parse(m.group(1) + " " + m.group(2), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                String thread = m.group(3);
                String level = m.group(4);
                String message = m.group(5);

                switch (level.toUpperCase()) {
                    case "INFO" -> info_counter++;
                    case "WARN" -> warn_counter++;
                    case "DEBUG" -> debug_counter++;
                    case "ERROR" -> err_counter++;
                }

            }
            statsData.add(
                      "Total lines: " + count + "\n"
                    + "INFO: " + info_counter + "\n"
                    + "WARN: " + warn_counter + "\n"
                    + "DEBUG: " + debug_counter + "\n"
                    + "ERROR: " + err_counter + "\n"
                    + "First entry: " + firstLine + "\n"
                    + "Last entry: " + lastLine
            );

            } catch (IOException e) {
                System.err.println("Error: cannot read file: " + file.getName());
            }
        return statsData;
        }
}
