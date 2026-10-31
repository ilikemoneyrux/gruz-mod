package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import pl.gruzclient.cosmetics.Cosmetics;
import pl.gruzclient.hypixel.HypixelStats;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/** Lista graczy (Tab): ping w ms zamiast kresek, logo Gruz przy nickach i statystyki Hypixela. */
public class TabModule extends Module {
    public final Setting.Bool pingMs = add(new Setting.Bool("pingMs", "setting.pingMs", true));
    public final Setting.Bool logo = add(new Setting.Bool("logo", "setting.gruzIcon", true));
    public final Setting.Bool hypixel = add(new Setting.Bool("hypixel", "setting.hypixelStats", true));

    public TabModule() { super("tab", Category.VISUAL, true); }

    @Override
    public void tick() {
        if (hypixel.value) HypixelStats.tick();
    }

    public Component name(PlayerInfo entry, Component name) {
        if (!isEnabled()) return name;
        var id = Compat.profileId(entry.getProfile());
        Component out = pl.gruzclient.cosmetics.NameEffects.apply(name, Cosmetics.ofUuid(id), Compat.profileName(entry.getProfile()));
        if (hypixel.value) {
            HypixelStats.Stats s = HypixelStats.get(id);
            if (s != null) {
                Component tag = s.nick()
                        ? Component.literal("[NICK] ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5C8A)))
                        : Component.literal("[" + s.stars() + "✫] ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(HypixelStats.starColor(s.stars()))));
                out = Component.empty().append(tag).append(out);
                if (!s.nick()) out = out.copy().append(Component.literal(String.format("  %.2f", s.fkdr()))
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(HypixelStats.fkdrColor(s.fkdr())))));
            }
        }
        if (logo.value && (Cosmetics.isGruzUser(id) || (mc.player != null && id.equals(mc.player.getUUID())))) out = NametagModule.withIcon(out);
        return out;
    }

    /** Ping liczbą w kolorze (zielony → czerwony) zamiast ikonki */
    public boolean drawPing(GuiGraphics c, int width, int x, int y, PlayerInfo entry) {
        if (!isEnabled() || !pingMs.value) return false;
        int ms = entry.getLatency();
        String s = ms <= 0 ? "?" : String.valueOf(ms);
        int col = ms <= 0 ? 0x9488A8 : ms < 60 ? 0x7DFFB2 : ms < 120 ? 0xC8FF5C : ms < 200 ? 0xFFD35C : ms < 350 ? 0xFF9A5C : 0xFF5C8A;
        var m = pl.gruzclient.compat.Pose2D.of(c);
        m.push();
        float k = 0.75f;
        float tx = x + width - 1 - mc.font.width(s) * k;
        m.translate(tx, y + 1.5f);
        m.scale(k, k);
        c.drawString(mc.font, s, 0, 0, 0xFF000000 | col, true);
        m.pop();
        return true;
    }
}
