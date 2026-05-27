package forge.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


@Command(name = "logs", description = "DESCRIPTION LOG COMMAND")
public class LogsCommand implements Runnable {

        @Option(names = {"--file"}, description = "DESCRIPTION FILE INPUT")
        private File filePath;

        @Option(names = {"--check"}, description = "DESCRIPTION CHECK")
        private boolean check = false;
        private String compare;

        @Override
        public void run() {
            System.out.println("Hello from logs!");
            if(filePath != null) {
            System.out.println("Selected file: " + filePath);
            readData(filePath);
            }
            if(compare != null) {
                System.out.println("Selected compare: " + compare);
                compareData(filePath, compare);
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
}

