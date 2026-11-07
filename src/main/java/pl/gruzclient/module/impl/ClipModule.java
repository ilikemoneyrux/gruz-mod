package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;
import pl.gruzclient.obs.ObsClient;

/**
 * Klipy zamiast replay: jeden klawisz każe OBS zapisać ostatnie sekundy gry (Bufor powtórek).
 * Opcjonalnie klip zapisuje się sam chwilę po każdym killu.
 */
public class ClipModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", InputConstants.KEY_F8));
    public final Setting.Bool onKill = add(new Setting.Bool("onKill", "setting.clipOnKill", false));
    public final Setting.Number port = add(new Setting.Number("port", "setting.obsPort", 4455f, 1024f, 65535f, 1f, ""));
    public final Setting.Text password = add(new Setting.Text("password", "setting.obsPassword", "").secret());
    public final Setting.Action test = add(new Setting.Action("test", "setting.obsTest", () -> save(true)));

    private boolean wasDown;
    private long killClipAt = 0;

    public ClipModule() { super("clip", Category.UTILITY, false); }

    @Override
    public void tick() {
        boolean down = Modules.keyDown(key.code);
        if (down && !wasDown) save(false);
        wasDown = down;
        if (killClipAt > 0 && System.currentTimeMillis() >= killClipAt) {
            killClipAt = 0;
            save(false);
        }
    }

    public void onKill() {
        if (isEnabled() && onKill.value) killClipAt = System.currentTimeMillis() + 3000; // 3 s po killu
    }

    private void save(boolean test) {
        ObsClient.saveReplay(Math.round(port.value), password.value, test, result -> mc.execute(() -> {
            if (mc.player != null) Compat.chat(mc.player, Component.literal("§d[Gruz] §f" + Lang.t("clip." + result)), true);
            else if (mc.screen != null) pl.gruzclient.GruzClient.LOG.info("OBS: {}", result);
        }));
    }
}
