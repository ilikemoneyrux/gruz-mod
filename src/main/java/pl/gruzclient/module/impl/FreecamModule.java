package pl.gruzclient.module.impl;

import com.mojang.authlib.GameProfile;
import org.lwjgl.glfw.GLFW;
import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Modules;
import pl.gruzclient.module.Setting;

import java.util.UUID;
//#if MC >= 1.21.2
import net.minecraft.client.player.ClientInput;
//#else
import net.minecraft.client.player.Input;
//#endif
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam: kamera lata swobodnie, postać stoi w miejscu. Tylko w świecie jednoosobowym
 * (na serwerach to byłaby nieuczciwa przewaga). W trakcie nie da się bić ani kopać.
 */
public class FreecamModule extends Module {
    public final Setting.Key key = add(new Setting.Key("key", "setting.key", GLFW.GLFW_KEY_F4));
    public final Setting.Number speed = add(new Setting.Number("speed", "setting.flySpeed", 1f, 0.2f, 5f, 0.1f, "x"));

    private RemotePlayer camera;
    //#if MC >= 1.21.2
    private ClientInput savedInput;
    //#else
    private Input savedInput;
    //#endif
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
        if (mc.player == null || mc.level == null || camera.level() != mc.level || !mc.isLocalServer()) { stop(); return; }
        move();
    }

    @Override
    protected void onDisable() { stop(); }

    private void start() {
        if (mc.player == null || mc.level == null) return;
        if (!mc.isLocalServer()) {
            mc.player.displayClientMessage(Component.literal("§d[Gruz] §f" + Lang.t("freecam.onlySp")), true);
            return;
        }
        camera = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "GruzCam"));
        camera.copyPosition(mc.player);
        camera.setYHeadRot(mc.player.getYHeadRot());
        camera.noPhysics = true;
        savedInput = mc.player.input;
        //#if MC >= 1.21.2
        mc.player.input = new ClientInput(); // postać przestaje reagować na klawisze
        //#else
        mc.player.input = new Input(); // postać przestaje reagować na klawisze
        //#endif
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
        if (camera != null) camera.turn(dx, dy);
    }

    private void move() {
        var o = mc.options;
        boolean screen = mc.screen != null;
        double fwd = 0, side = 0, up = 0;
        if (!screen) {
            if (o.keyUp.isDown()) fwd++;
            if (o.keyDown.isDown()) fwd--;
            if (o.keyLeft.isDown()) side++;
            if (o.keyRight.isDown()) side--;
            if (o.keyJump.isDown()) up++;
            if (o.keyShift.isDown()) up--;
        }
        double sp = 0.6 * speed.value * (o.keySprint.isDown() ? 2.5 : 1);
        float yaw = camera.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        Vec3 v = new Vec3(side * cos - fwd * sin, up, fwd * cos + side * sin);
        if (v.lengthSqr() > 1) v = v.normalize();
        v = v.scale(sp);
        camera.xo = camera.getX();
        camera.yo = camera.getY();
        camera.zo = camera.getZ();
        camera.xOld = camera.getX();
        camera.yOld = camera.getY();
        camera.zOld = camera.getZ();
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        camera.setPosRaw(camera.getX() + v.x, camera.getY() + v.y, camera.getZ() + v.z);
    }
}
