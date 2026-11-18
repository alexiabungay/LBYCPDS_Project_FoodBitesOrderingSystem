package services;

import java.util.Scanner;

public class ClearScreen {
    private static volatile boolean masking;

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
        System.out.print(prompt + " ");
        masking = true;
        Thread eraser = new Thread(() -> {
            while (masking) {
                System.out.print("\b*");
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        eraser.start();
        String password = scanner.nextLine();
        masking = false;
        try {
            eraser.join();
        } catch (InterruptedException e) {
        }
        System.out.println();
        return password;
    }
}