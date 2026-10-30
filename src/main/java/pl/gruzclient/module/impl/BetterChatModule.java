package pl.gruzclient.module.impl;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Lepszy czat: zwijanie powtórzeń "(x3)", godziny wiadomości, dłuższa historia,
 * kopiowanie prawym przyciskiem myszy i wyszukiwanie (Ctrl+F na otwartym czacie).
 */
public class BetterChatModule extends Module {
    public final Setting.Bool stack = add(new Setting.Bool("stack", "setting.stackMessages", true));
    public final Setting.Bool timestamps = add(new Setting.Bool("time", "setting.timestamps", false));
    public final Setting.Bool longHistory = add(new Setting.Bool("history", "setting.longHistory", true));
    public final Setting.Bool copy = add(new Setting.Bool("copy", "setting.rightClickCopy", true));

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    /** Aktualna fraza wyszukiwania (pusta = brak filtra) */
    public static String filter = "";

    public BetterChatModule() { super("betterchat", Category.UTILITY, true); }

    public int historyLimit() { return isEnabled() && longHistory.value ? 1000 : 100; }

    /** Dokleja godzinę na początku wiadomości */
    public Text decorate(Text message) {
        if (!isEnabled() || !timestamps.value) return message;
        MutableText t = Text.literal("[" + LocalTime.now().format(TIME) + "] ").formatted(Formatting.DARK_GRAY);
        return t.append(message);
    }

    /** Licznik powtórzeń doklejany na końcu */
    public static Text withCount(Text message, int count) {
        return message.copy().append(Text.literal(" (x" + count + ")").formatted(Formatting.GRAY));
    }

    public boolean stacking() { return isEnabled() && stack.value; }
    public boolean copying() { return isEnabled() && copy.value; }

    public static boolean matches(Text message) {
        return filter.isEmpty() || message.getString().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }
}
