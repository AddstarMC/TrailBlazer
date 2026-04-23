package au.com.addstar.trailblazer.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static Component mm(String input) {
        return MINI_MESSAGE.deserialize(input);
    }

    public static String humanizeEnumName(String rawName) {
        return Arrays.stream(rawName.toLowerCase().split("_"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    public static String humanizeEnum(Enum<?> e) {
        return humanizeEnumName(e.name());
    }

    public static String formatDurationShort(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}