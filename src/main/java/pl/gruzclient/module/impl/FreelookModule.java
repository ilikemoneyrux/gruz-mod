package pl.gruzclient.module.impl;

import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

/**
 * Perspective / freelook: kamera krąży wokół postaci, a postać patrzy dalej tam, gdzie patrzyła.
 * Domyślnie lewy Alt (przytrzymaj).
 */
public class FreelookModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", GLFW.GLFW_KEY_LEFT_ALT));
    public final Setting.Choice mode = add(new Setting.Choice("mode", "setting.mode", 0, "setting.mode.hold", "setting.mode.toggle"));

    private boolean active, wasDown;
    private Perspective previous;
    public float yaw, pitch;

    public FreelookModule() { super("freelook", Category.UTILITY, true); }

    @Override
    public void tick() {
        if (mc.player == null) { stop(); return; }
        boolean down = Modules.keyDown(key.code);
        boolean want = mode.index == 0 ? down : (down && !wasDown ? !active : active);
        wasDown = down;
        if (want && !active) start();
        else if (!want && active) stop();
    }

    private void start() {
        active = true;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        previous = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    private void stop() {
        if (!active) return;
        active = false;
        if (previous != null) mc.options.setPerspective(previous);
    }

    public boolean isActive() { return isEnabled() && active; }

    /** Ruch myszą obraca tylko kamerę (te same przeliczniki co w Minecrafcie) */
    public void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }

    @Override
    protected void onDisable() { stop(); }
}
