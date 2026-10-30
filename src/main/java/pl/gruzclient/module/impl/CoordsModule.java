package pl.gruzclient.module.impl;

import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import static pl.gruzclient.render.GruzDraw.*;

import net.minecraft.client.gui.GuiGraphics;

/** Koordynaty X / Y / Z i kierunek, w którym patrzysz. */
public class CoordsModule extends HudModule {
    public final Setting.Bool showDirection = add(new Setting.Bool("direction", "setting.showDirection", true));

    public CoordsModule() { super("coords", false, 0.01f, 0.15f); }

    @Override
    protected int[] draw(GuiGraphics c, boolean editing) {
        var tr = mc.font;
        int x = 0, y = 64, z = 0;
        String dir = Lang.t("dir.north");
        if (mc.player != null) {
            x = mc.player.getBlockX(); y = mc.player.getBlockY(); z = mc.player.getBlockZ();
            dir = switch (mc.player.getDirection()) {
                case SOUTH -> Lang.t("dir.south");
                case EAST -> Lang.t("dir.east");
                case WEST -> Lang.t("dir.west");
                default -> Lang.t("dir.north");
            };
        }
        String[] lines = showDirection.value
                ? new String[]{"X  " + x, "Y  " + y, "Z  " + z, dir}
                : new String[]{"X  " + x, "Y  " + y, "Z  " + z};
        int w = 0;
        for (String l : lines) w = Math.max(w, tr.width(l));
        w += 10;
        int h = lines.length * 10 + 6;
        panel(c, w, h);
        for (int i = 0; i < lines.length; i++) {
            boolean last = showDirection.value && i == lines.length - 1;
            c.drawString(tr, lines[i], 5, 4 + i * 10, last ? argb(1f, MUTE) : textColor(), !background.value);
        }
        return new int[]{w, h};
    }
}
