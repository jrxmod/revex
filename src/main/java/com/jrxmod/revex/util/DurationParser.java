package com.jrxmod.revex.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdw])");

    // Parse duration string like "15m", "1h", "7d" into milliseconds. Returns -1 for invalid input.
    public static long parseToMs(String input) {
        if (input == null || input.isEmpty()) return -1;

        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase().trim());
        if (!matcher.matches()) return -1;

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "s" -> value * 1000L;
            case "m" -> value * 60_000L;
            case "h" -> value * 3_600_000L;
            case "d" -> value * 86_400_000L;
            case "w" -> value * 604_800_000L;
            default -> -1;
        };
    }

    // Format milliseconds into human-readable string like "2h 15m"
    public static String format(long ms) {
        if (ms <= 0) return "expired";

        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
