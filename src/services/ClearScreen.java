package services;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

public class ClearScreen {
    public static void clear() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    public static void pause(Scanner scanner) {
        System.out.print("\nPress Enter to continue");
        scanner.nextLine();
    }

    public static String readPassword(String prompt, Scanner scanner) {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("%s", prompt);
            System.out.println();
            if (password == null) {
                System.exit(0);
                return "";
            }
            String text = new String(password);
            Arrays.fill(password, '\0');
            return text;
        }

        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.exit(0);
        }
        return scanner.nextLine();
    }
}
