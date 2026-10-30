package pl.gruzclient.module.impl;

import net.minecraft.client.multiplayer.PlayerInfo;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.TextHudModule;

public class PingModule extends TextHudModule {
    public PingModule() { super("ping", false, 0.01f, 0.105f); }

    @Override
    protected String value(boolean editing) {
        if (mc.player == null || mc.getConnection() == null) return editing ? "42" : "0";
        if (mc.isLocalServer()) return "0";
        PlayerInfo e = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return e == null ? "?" : String.valueOf(e.getLatency());
    }

    @Override protected String label() { return Lang.t("hud.ping"); }
}
