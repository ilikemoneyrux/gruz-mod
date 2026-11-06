package pl.gruzclient.screen;

import pl.gruzclient.cosmetics.CosmeticSet;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Setting;
import pl.gruzclient.module.impl.CosmeticsModule;

import static pl.gruzclient.render.GruzDraw.*;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Szafa w grze (od 0.5.2): przebieranie bez wychodzenia z gry. Gra się nie pauzuje, kamera obraca się
 * przodem do Ciebie, więc widzisz zmianę od razu. Zablokowane rzeczy można przymierzyć – tylko Ty je widzisz,
 * po zamknięciu wracają Twoje zapisane kosmetyki (inni gracze nie widzą przymierzania).
 */
public class WardrobeScreen extends pl.gruzclient.compat.GruzScreen {
    private static final String[] SLOTS = {"cape", "head", "wings", "back", "trail", "pet", "name"};
    private static final int PANEL_W = 236, ROW_H = 17;

    private final CosmeticsModule module;
    private final long openedAt = System.currentTimeMillis();
    private final CameraType before;
    private int slot = 0;
    private int scroll = 0;
    private int hovered = -1, hoveredTab = -1;
    private boolean hoverClose = false;
    /** przymierzana zablokowana rzecz w danym slocie (-1 = brak) */
    private int tryIndex = -1;

    public WardrobeScreen(CosmeticsModule module) {
        super(Component.literal("Wardrobe"));
        this.module = module;
        this.before = minecraft().options.getCameraType();
        minecraft().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }

    private static net.minecraft.client.Minecraft minecraft() { return net.minecraft.client.Minecraft.getInstance(); }

    private Setting.Choice choice(int s) {
        return switch (s) {
            case 0 -> module.cape;
            case 1 -> module.head;
            case 2 -> module.wings;
            case 3 -> module.back;
            case 4 -> module.trail;
            case 5 -> module.pet;
            default -> module.name;
        };
    }

    private static String[] values(int s) {
        return switch (s) {
            case 0 -> CosmeticSet.CAPES;
            case 1 -> CosmeticSet.HEADS;
            case 2 -> CosmeticSet.WINGS;
            case 3 -> CosmeticSet.BACKS;
            case 4 -> CosmeticSet.TRAILS;
            case 5 -> CosmeticSet.PETS;
            default -> CosmeticSet.NAMES;
        };
    }

    private boolean locked(int s, int i) { return !CosmeticSet.unlocked(SLOTS[s], values(s)[i]); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics c, int mouseX, int mouseY, float delta) {
        // bez przyciemniania całego ekranu – postać ma być dobrze widać
    }

    private int listTop() { return 70; }
    private int visibleRows() { return Math.max(4, (this.height - listTop() - 56) / ROW_H); }

    @Override
    public void render(GuiGraphics c, int mouseX, int mouseY, float delta) {
        float a = outExpo(clamp01((System.currentTimeMillis() - openedAt) / 240f));
        int x0 = 10 - (int) ((1f - a) * 40), y0 = 10, w = PANEL_W, h = this.height - 20;
        c.fill(x0, y0, x0 + w, y0 + h, argb(0.86f * a, SLAB));
        c.renderOutline(x0, y0, w, h, argb(a, SEAM));
        c.fill(x0, y0, x0 + 3, y0 + h, argb(a, NEON));
        if (!textVisible(a)) return;
        c.drawString(this.font, Lang.t("wardrobe.title"), x0 + 12, y0 + 10, argb(a, TEXT), false);
        c.drawString(this.font, Lang.t("wardrobe.sub"), x0 + 12, y0 + 22, argb(a, MUTE), false);

        // zakładki slotów
        hoveredTab = -1;
        int tx = x0 + 12, ty = y0 + 38;
        for (int s = 0; s < SLOTS.length; s++) {
            String label = Lang.t("wardrobe.slot." + SLOTS[s]);
            int tw = this.font.width(label) + 8;
            if (tx + tw > x0 + w - 8) { tx = x0 + 12; ty += 14; }
            boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + 12;
            if (hov) hoveredTab = s;
            c.fill(tx, ty, tx + tw, ty + 12, argb(a, s == slot ? NEON_DEEP : hov ? 0x22143A : 0x161020));
            c.drawString(this.font, label, tx + 4, ty + 2, argb(a, s == slot ? TEXT : MUTE), false);
            tx += tw + 3;
        }

        // lista
        String[] vals = values(slot);
        Setting.Choice ch = choice(slot);
        int top = Math.max(listTop(), ty + 20), rows = Math.min(vals.length, (this.height - top - 56) / ROW_H);
        scroll = Math.max(0, Math.min(scroll, vals.length - rows));
        hovered = -1;
        for (int r = 0; r < rows; r++) {
            int i = r + scroll, ry = top + r * ROW_H;
            boolean worn = ch.index == i && tryIndex < 0, trying = tryIndex == i, lock = locked(slot, i);
            boolean hov = mouseX >= x0 + 8 && mouseX < x0 + w - 8 && mouseY >= ry && mouseY < ry + ROW_H - 2;
            if (hov) hovered = i;
            c.fill(x0 + 8, ry, x0 + w - 8, ry + ROW_H - 2, argb(a, worn ? 0x1E3A2A : trying ? 0x2A1E3A : hov ? 0x1A1426 : 0x120E1A));
            if (worn || trying) c.fill(x0 + 8, ry, x0 + 10, ry + ROW_H - 2, argb(a, worn ? 0x7DFFB2 : NEON));
            String name = Lang.t(ch.keys[i]);
            c.drawString(this.font, name, x0 + 14, ry + 4, argb(a, lock ? MUTE : TEXT), false);
            String tag = worn ? Lang.t("wardrobe.worn") : trying ? Lang.t("wardrobe.trying") : lock ? Lang.t("wardrobe.locked") : "";
            if (!tag.isEmpty()) c.drawString(this.font, tag, x0 + w - 12 - this.font.width(tag), ry + 4, argb(a, worn ? 0x7DFFB2 : lock ? 0xFF8AB0 : NEON_HOT), false);
        }
        if (scroll > 0) c.drawString(this.font, "▲", x0 + w / 2, top - 10, argb(a, MUTE), false);
        if (scroll + rows < vals.length) c.drawString(this.font, "▼", x0 + w / 2, top + rows * ROW_H, argb(a, MUTE), false);

        // stopka
        int fy = y0 + h - 40;
        String hint = tryIndex >= 0 ? Lang.t("wardrobe.tryHint") : Lang.t("wardrobe.hint");
        drawWrapped(c, hint, x0 + 12, fy, w - 24, argb(a, MUTE));
        String close = Lang.t("wardrobe.close");
        int cw = this.font.width(close) + 16, cx = x0 + w - 12 - cw, cy = y0 + h - 18;
        hoverClose = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + 13;
        c.fill(cx, cy, cx + cw, cy + 13, argb(a, hoverClose ? NEON : NEON_DEEP));
        c.drawString(this.font, close, cx + 8, cy + 3, argb(a, TEXT), false);
    }

    private void drawWrapped(GuiGraphics c, String text, int x, int y, int maxW, int color) {
        StringBuilder line = new StringBuilder();
        int ly = y;
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(test) > maxW && !line.isEmpty()) {
                c.drawString(this.font, line.toString(), x, ly, color, false);
                ly += 10;
                line = new StringBuilder(word);
            } else line = new StringBuilder(test);
        }
        if (!line.isEmpty()) c.drawString(this.font, line.toString(), x, ly, color, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hoverClose) { onClose(); return true; }
        if (hoveredTab >= 0) { endTry(); slot = hoveredTab; scroll = 0; return true; }
        if (hovered >= 0) {
            if (locked(slot, hovered)) {
                // przymierzanie zablokowanej rzeczy: tylko lokalnie
                tryIndex = tryIndex == hovered ? -1 : hovered;
                module.preview(tryIndex < 0 ? null : SLOTS[slot], tryIndex < 0 ? null : values(slot)[tryIndex]);
            } else {
                endTry();
                choice(slot).index = hovered;
                module.saveChoice();
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void endTry() {
        if (tryIndex >= 0) { tryIndex = -1; module.preview(null, null); }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= (int) Math.signum(v);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == module.wardrobeKey.code) { onClose(); return true; }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void onClose() {
        module.preview(null, null);
        minecraft().options.setCameraType(before);
        super.onClose();
    }
}
