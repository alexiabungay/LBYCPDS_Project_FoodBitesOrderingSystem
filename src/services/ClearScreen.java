package services;

import java.io.Console;
import java.util.Scanner;

public class ClearScreen {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    private ClearScreen() { }

    private static boolean isTerminal() {
        Console console = System.console();
        return console != null && console.isTerminal();
    }

    public static void clear() {
        if (!isTerminal()) {
            for (int line = 0; line < 50; line++) {
                System.out.println();
            }
            return;
        }
        try {
            if (WINDOWS) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception exception) {
        }
    }

    public static void pause(Scanner scanner) {
        System.out.print("\nPress Enter to continue");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    public static String readPassword(String prompt, Scanner scanner) {
        if (isTerminal()) {
            char[] password = System.console().readPassword(prompt);
            if (password != null) {
                return new String(password);
            }
        }
        System.out.print(prompt);
        return scanner.nextLine();
    }
}