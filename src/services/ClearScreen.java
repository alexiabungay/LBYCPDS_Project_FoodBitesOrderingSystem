package services;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

public final class ClearScreen {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final boolean TERMINAL = detectTerminal();
    private static boolean maskingHintShown = false;

    private ClearScreen() { }

    private static boolean detectTerminal() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            Method isTerminal = Console.class.getMethod("isTerminal");
            return (Boolean) isTerminal.invoke(console);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return true;
        }
    }

    public static void clear() {
        if (!TERMINAL) {
            for (int line = 0; line < 50; line++) {
                System.out.println();
            }
            return;
        }
        try {
            if (WINDOWS) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J\033[3J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException exception) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    public static void pause(Scanner scanner) {
        System.out.print("\nPress Enter to continue...");
        System.out.flush();
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}