package services;

import java.util.ArrayList;
import java.util.List;

public class Csv {
    public static String[] parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
    }
}
