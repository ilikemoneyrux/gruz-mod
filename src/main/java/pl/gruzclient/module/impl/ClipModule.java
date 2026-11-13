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
 * Klipy: jeden klawisz zapisuje ostatnie sekundy gry – wbudowanym buforem Gruz (Replay, plik AVI w gruz-clips/)
 * albo przez OBS (Bufor powtórek).
 * Opcjonalnie klip zapisuje się sam chwilę po każdym killu.
 */
public class ClipModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", InputConstants.KEY_F8));
    public final Setting.Choice source = add(new Setting.Choice("source", "setting.clipSource", 0, "clip.src.gruz", "clip.src.obs"));
    public final Setting.Choice length = add(new Setting.Choice("length", "setting.clipLength", 1, "clip.len.15", "clip.len.30", "clip.len.60"));
    public final Setting.Action folder = add(new Setting.Action("folder", "setting.clipFolder", this::openFolder).button("clip.openFolder"));
    public final Setting.Bool onKill = add(new Setting.Bool("onKill", "setting.clipOnKill", false));
    public final Setting.Number port = add(new Setting.Number("port", "setting.obsPort", 4455f, 1024f, 65535f, 1f, ""));
    public final Setting.Text password = add(new Setting.Text("password", "setting.obsPassword", "").secret());
    public final Setting.Action test = add(new Setting.Action("test", "setting.obsTest", () -> save(true)));

    private boolean wasDown;
    private long killClipAt = 0;

    public ClipModule() { super("clip", Category.UTILITY, false); }

    @Override
    public void tick() {
        if (builtIn()) pl.gruzclient.replay.Replay.tick(mc, seconds());
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

    private boolean builtIn() { return isEnabled() && source.index == 0; }
    private int seconds() { return new int[]{15, 30, 60}[length.index]; }

    @Override
    protected void onDisable() { pl.gruzclient.replay.Replay.clear(); }

    private void openFolder() {
        java.io.File dir = new java.io.File(mc.gameDirectory, "gruz-clips");
        dir.mkdirs();
        //#if MC >= 26.3
        com.mojang.blaze3d.Blaze3D.openPath(dir.toPath());
        //#else
        net.minecraft.Util.getPlatform().openFile(dir);
        //#endif
    }

    private void save(boolean test) {
        if (!test && source.index == 0) {
            if (!builtIn()) return;
            pl.gruzclient.replay.Replay.save(mc, name -> {
                if (mc.player != null) Compat.chat(mc.player, Component.literal("§d[Gruz] §f" + (name == null
                        ? Lang.t("clip.empty") : Lang.t("clip.savedGruz").replace("{name}", name))), false);
            });
            return;
        }
        ObsClient.saveReplay(Math.round(port.value), password.value, test, result -> mc.execute(() -> {
            if (mc.player != null) Compat.chat(mc.player, Component.literal("§d[Gruz] §f" + Lang.t("clip." + result)), true);
            else if (mc.screen != null) pl.gruzclient.GruzClient.LOG.info("OBS: {}", result);
        }));
    }
}
