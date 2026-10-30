package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

import java.util.regex.Pattern;

/** Auto GG: po końcu gry (wykrytym z czatu) wysyła "gg". Opcjonalnie też po zabójstwie. */
public class AutoGGModule extends Module {
    public static final String[] MESSAGES = {"gg", "GG", "gg wp", "Dobra gra!", "Good game!"};
    public final Setting.Choice message = add(new Setting.Choice("message", "setting.ggMessage", 0,
            "gg.gg", "gg.GG", "gg.ggwp", "gg.pl", "gg.en"));
    public final Setting.Number delay = add(new Setting.Number("delay", "setting.delay", 1.5f, 0.5f, 5f, 0.5f, " s"));
    public final Setting.Bool onKill = add(new Setting.Bool("onKill", "setting.ggOnKill", false));

    /** Typowe komunikaty końca gry (Hypixel, polskie serwery minigier) */
    private static final Pattern END = Pattern.compile(
            "(1st Killer|Winner:|Winners:|Reward Summary|You won|VICTORY|GAME OVER|Game Over!|has won the game|"
            + "Wygrałeś|Wygrała drużyna|Wygrywa|Zwycięzca|Zwycięstwo|Koniec gry|KONIEC GRY|Gra zakończona|wygrał grę)",
            Pattern.CASE_INSENSITIVE);

    private long sendAt = 0;
    private long lastSent = 0;

    public AutoGGModule() { super("autogg", Category.UTILITY, false); }

    /** Każda wiadomość na czacie (z mixina ChatHud) */
    public void onChat(String text) {
        if (!isEnabled() || mc.player == null || mc.isLocalServer()) return;
        // ignorujemy wiadomości graczy ("<Nick> ..." albo "Nick: ...") – szukamy komunikatów serwera
        if (text.startsWith("<") || text.matches("^\\[?[\\w\\s\\]]{0,24}[\\w]{3,16}: .*")) return;
        if (END.matcher(text).find()) schedule();
    }

    public void onKill() {
        if (isEnabled() && onKill.value) schedule();
    }

    private void schedule() {
        long now = System.currentTimeMillis();
        if (now - lastSent < 10000 || sendAt > now) return; // maks. raz na 10 sekund
        sendAt = now + Math.round(delay.value * 1000);
    }

    @Override
    public void tick() {
        if (sendAt == 0 || System.currentTimeMillis() < sendAt) return;
        sendAt = 0;
        if (mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().sendChat(MESSAGES[message.index]);
        lastSent = System.currentTimeMillis();
    }
}
