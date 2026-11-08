package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Lepszy czat: zwijanie powtórzeń "(x3)", godziny wiadomości, dłuższa historia,
 * kopiowanie prawym przyciskiem myszy i wyszukiwanie (Ctrl+F na otwartym czacie).
 */
public class BetterChatModule extends Module {
    public final Setting.Bool stack = add(new Setting.Bool("stack", "setting.stackMessages", true));
    public final Setting.Bool timestamps = add(new Setting.Bool("time", "setting.timestamps", false));
    public final Setting.Bool longHistory = add(new Setting.Bool("history", "setting.longHistory", true));
    public final Setting.Bool copy = add(new Setting.Bool("copy", "setting.rightClickCopy", true));
    /** Znajomi z Gruz: wejście do gry / na Twój serwer (działa też przy wyłączonym module) */
    public final Setting.Bool friendAlerts = add(new Setting.Bool("friendAlerts", "setting.friendAlerts", true));
    /** Screeny: przycisk „Udostępnij” po F2 / od razu wysyłaj każdy screen (link w schowku) */
    public final Setting.Bool shareButton = add(new Setting.Bool("shareButton", "setting.shareButton", true));
    public final Setting.Bool autoShare = add(new Setting.Bool("autoShare", "setting.autoShare", false));

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    /** Aktualna fraza wyszukiwania (pusta = brak filtra) */
    public static String filter = "";

    public BetterChatModule() { super("betterchat", Category.UTILITY, true); }

    public int historyLimit() { return isEnabled() && longHistory.value ? 1000 : 100; }

    /** Dokleja godzinę na początku wiadomości */
    public Component decorate(Component message) {
        if (!isEnabled() || !timestamps.value) return message;
        MutableComponent t = Component.literal("[" + LocalTime.now().format(TIME) + "] ").withStyle(ChatFormatting.DARK_GRAY);
        return t.append(message);
    }

    /** Licznik powtórzeń doklejany na końcu */
    public static Component withCount(Component message, int count) {
        return message.copy().append(Component.literal(" (x" + count + ")").withStyle(ChatFormatting.GRAY));
    }

    public boolean stacking() { return isEnabled() && stack.value; }
    public boolean copying() { return isEnabled() && copy.value; }

    public static boolean matches(Component message) {
        return filter.isEmpty() || message.getString().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }
}
