package pl.gruzclient.module.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

import java.util.UUID;

/**
 * Freecam: kamera lata swobodnie, postać stoi w miejscu. Tylko w świecie jednoosobowym
 * (na serwerach to byłaby nieuczciwa przewaga). W trakcie nie da się bić ani kopać.
 */
public class FreecamModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", GLFW.GLFW_KEY_F4));
    public final Setting.Number speed = add(new Setting.Number("speed", "setting.flySpeed", 1f, 0.2f, 5f, 0.1f, "x"));

    private OtherClientPlayerEntity camera;
    private Input savedInput;
    private boolean wasDown;

    public FreecamModule() { super("freecam", Category.UTILITY, true); }

    public boolean isActive() { return camera != null; }

    @Override
    public void tick() {
        boolean down = Modules.keyDown(key.code);
        if (down && !wasDown) {
            if (isActive()) stop(); else start();
        }
        wasDown = down;
        if (!isActive()) return;
        if (mc.player == null || mc.world == null || camera.getWorld() != mc.world || !mc.isInSingleplayer()) { stop(); return; }
        move();
    }

    @Override
    protected void onDisable() { stop(); }

    private void start() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.isInSingleplayer()) {
            mc.player.sendMessage(Text.literal("§d[Gruz] §f" + Lang.t("freecam.onlySp")), true);
            return;
        }
        camera = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "GruzCam"));
        camera.copyPositionAndRotation(mc.player);
        camera.setHeadYaw(mc.player.getHeadYaw());
        camera.noClip = true;
        savedInput = mc.player.input;
        mc.player.input = new Input(); // postać przestaje reagować na klawisze
        mc.setCameraEntity(camera);
    }

    public void stop() {
        if (camera == null) return;
        if (mc.player != null) {
            if (savedInput != null) mc.player.input = savedInput;
            mc.setCameraEntity(mc.player);
        }
        camera = null;
        savedInput = null;
    }

    /** Obrót myszą trafia do kamery zamiast do postaci (EntityMixin) */
    public void look(double dx, double dy) {
        if (camera != null) camera.changeLookDirection(dx, dy);
    }

    private void move() {
        var o = mc.options;
        boolean screen = mc.currentScreen != null;
        double fwd = 0, side = 0, up = 0;
        if (!screen) {
            if (o.forwardKey.isPressed()) fwd++;
            if (o.backKey.isPressed()) fwd--;
            if (o.leftKey.isPressed()) side++;
            if (o.rightKey.isPressed()) side--;
            if (o.jumpKey.isPressed()) up++;
            if (o.sneakKey.isPressed()) up--;
        }
        double sp = 0.6 * speed.value * (o.sprintKey.isPressed() ? 2.5 : 1);
        float yaw = camera.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double sin = MathHelper.sin(yaw), cos = MathHelper.cos(yaw);
        Vec3d v = new Vec3d(side * cos - fwd * sin, up, fwd * cos + side * sin);
        if (v.lengthSquared() > 1) v = v.normalize();
        v = v.multiply(sp);
        camera.prevX = camera.getX();
        camera.prevY = camera.getY();
        camera.prevZ = camera.getZ();
        camera.lastRenderX = camera.getX();
        camera.lastRenderY = camera.getY();
        camera.lastRenderZ = camera.getZ();
        camera.prevYaw = camera.getYaw();
        camera.prevPitch = camera.getPitch();
        camera.setPos(camera.getX() + v.x, camera.getY() + v.y, camera.getZ() + v.z);
    }
}
