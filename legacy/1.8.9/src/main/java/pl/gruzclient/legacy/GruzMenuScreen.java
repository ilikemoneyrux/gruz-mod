package pl.gruzclient.legacy;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Menu modułów (prawy Shift): przycisk na każdy moduł, kliknięcie włącza/wyłącza i zapisuje. */
public class GruzMenuScreen extends Screen {
    private static final String[] IDS = {"fps", "cps", "coords", "keystrokes", "armor", "potions", "toggleSprint", "fullbright", "zoom", "freelook", "network", "capes", "friendAlerts"};
    private static final String[] EN = {"FPS", "CPS", "Coordinates", "Keystrokes", "Armor HUD", "Potion HUD", "Toggle Sprint", "Fullbright", "Zoom (C)", "Perspective (Left Alt)", "Gruz network", "Gruz capes", "Friend alerts"};
    private static final String[] PL = {"FPS", "CPS", "Koordynaty", "Klawisze", "Zbroja", "Mikstury", "Toggle Sprint", "Pełna jasność", "Zoom (C)", "Perspektywa (lewy Alt)", "Sieć Gruz", "Peleryny Gruz", "Powiadomienia o znajomych"};

    private static boolean get(int i) {
        try { return Mods.Config.class.getField(IDS[i]).getBoolean(Mods.cfg); } catch (Exception e) { return false; }
    }

    private static void flip(int i) {
        try { Mods.Config.class.getField(IDS[i]).setBoolean(Mods.cfg, !get(i)); } catch (Exception ignored) { }
    }

    private String label(int i) {
        String on = Mods.pl() ? (get(i) ? "WŁ." : "WYŁ.") : (get(i) ? "ON" : "OFF");
        return (Mods.pl() ? PL[i] : EN[i]) + ": " + (get(i) ? "§a" : "§c") + on;
    }

    @Override
    public void init() {
        buttons.clear();
        int cols = 2, bw = 150, bh = 20, gap = 4;
        int x0 = width / 2 - (cols * bw + gap) / 2, y0 = height / 2 - ((IDS.length + 1) / 2) * (bh + gap) / 2;
        for (int i = 0; i < IDS.length; i++) {
            buttons.add(new ButtonWidget(i, x0 + (i % cols) * (bw + gap), y0 + (i / cols) * (bh + gap), bw, bh, label(i)));
        }
        buttons.add(new ButtonWidget(100, width / 2 - 75, y0 + ((IDS.length + 1) / 2) * (bh + gap) + 8, 150, 20, Mods.pl() ? "Gotowe" : "Done"));
    }

    @Override
    protected void buttonClicked(ButtonWidget b) {
        if (b.id == 100) { client.setScreen(null); return; }
        flip(b.id);
        b.message = label(b.id);
        Mods.save();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        renderBackground();
        drawCenteredString(textRenderer, "§d§lGRUZ CLIENT §7· 1.8.9", width / 2, 20, 0xFFFFFF);
        super.render(mouseX, mouseY, delta);
    }

    @Override
    public void removed() { Mods.save(); }
}
