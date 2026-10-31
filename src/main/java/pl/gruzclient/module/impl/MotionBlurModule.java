package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import pl.gruzclient.GruzClient;
import pl.gruzclient.module.Module;
import pl.gruzclient.module.Setting;

/**
 * Motion blur: każda klatka jest mieszana z poprzednią (shader post-processingu
 * assets/gruzclient/shaders/post/motion_blur.json).
 */
public class MotionBlurModule extends Module {
    public final Setting.Number strength = add(new Setting.Number("strength", "setting.strength", 50f, 10f, 90f, 5f, "%"));

    private static final ResourceLocation SHADER = Compat.id(GruzClient.MOD_ID, "shaders/post/motion_blur.json");
    private PostChain processor;
    private int width = -1, height = -1;
    private boolean failed = false;

    public MotionBlurModule() { super("motionblur", Category.VISUAL, false); }

    /** Wywoływane w GameRenderer po narysowaniu świata */
    public void render(float tickDelta) {
        //#if MC < 1.21.2
        if (!isEnabled() || failed || mc.level == null) return;
        try {
            if (processor == null) {
                processor = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), SHADER);
                width = -1;
            }
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            if (w != width || h != height) {
                processor.resize(w, h);
                width = w;
                height = h;
            }
            //#if MC >= 1.21
            processor.setUniform("BlendFactor", strength.value / 100f);
            //#endif
            processor.process(tickDelta);
        } catch (Exception e) {
            failed = true; // shader nie działa na tym sprzęcie / wersji – wyłączamy bez crasha
            GruzClient.LOG.warn("Motion blur wyłączony: {}", e.toString());
            close();
        }
        //#endif
        // TODO: od 1.21.2 post-processing działa inaczej (FrameGraph) – motion blur jeszcze nieprzeniesiony
    }

    private void close() {
        //#if MC < 1.21.2
        if (processor != null) { processor.close(); processor = null; }
        //#endif
    }

    @Override
    protected void onDisable() { close(); failed = false; }
}
