package pl.gruzclient.module.impl;

import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import pl.gruzclient.GruzClient;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/**
 * Motion blur: każda klatka jest mieszana z poprzednią (shader post-processingu
 * assets/gruzclient/shaders/post/motion_blur.json).
 */
public class MotionBlurModule extends Module {
    public final Setting.Number strength = add(new Setting.Number("strength", "setting.strength", 50f, 10f, 90f, 5f, "%"));

    private static final Identifier SHADER = Identifier.of(GruzClient.MOD_ID, "shaders/post/motion_blur.json");
    private PostEffectProcessor processor;
    private int width = -1, height = -1;
    private boolean failed = false;

    public MotionBlurModule() { super("motionblur", Category.VISUAL, false); }

    /** Wywoływane w GameRenderer po narysowaniu świata */
    public void render(float tickDelta) {
        if (!isEnabled() || failed || mc.world == null) return;
        try {
            if (processor == null) {
                processor = new PostEffectProcessor(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), SHADER);
                width = -1;
            }
            int w = mc.getWindow().getFramebufferWidth(), h = mc.getWindow().getFramebufferHeight();
            if (w != width || h != height) {
                processor.setupDimensions(w, h);
                width = w;
                height = h;
            }
            processor.setUniforms("BlendFactor", strength.value / 100f);
            processor.render(tickDelta);
        } catch (Exception e) {
            failed = true; // shader nie działa na tym sprzęcie / wersji – wyłączamy bez crasha
            GruzClient.LOG.warn("Motion blur wyłączony: {}", e.toString());
            close();
        }
    }

    private void close() {
        if (processor != null) { processor.close(); processor = null; }
    }

    @Override
    protected void onDisable() { close(); failed = false; }
}
