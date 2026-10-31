package pl.gruzclient.compat;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Przekształcenia 2D w GUI. Do 1.21.5 GuiGraphics.pose() to PoseStack (3D),
 * od 1.21.6 to Matrix3x2fStack (2D) – ten wrapper ukrywa różnicę.
 */
public final class Pose2D {
    //#if MC >= 1.21.6
    private final org.joml.Matrix3x2fStack m;
    //#else
    private final com.mojang.blaze3d.vertex.PoseStack m;
    //#endif

    private Pose2D(GuiGraphics c) { this.m = c.pose(); }

    public static Pose2D of(GuiGraphics c) { return new Pose2D(c); }

    public Pose2D push() {
        //#if MC >= 1.21.6
        m.pushMatrix();
        //#else
        m.pushPose();
        //#endif
        return this;
    }

    public Pose2D pop() {
        //#if MC >= 1.21.6
        m.popMatrix();
        //#else
        m.popPose();
        //#endif
        return this;
    }

    public Pose2D translate(float x, float y) {
        //#if MC >= 1.21.6
        m.translate(x, y);
        //#else
        m.translate(x, y, 0f);
        //#endif
        return this;
    }

    public Pose2D scale(float sx, float sy) {
        //#if MC >= 1.21.6
        m.scale(sx, sy);
        //#else
        m.scale(sx, sy, 1f);
        //#endif
        return this;
    }

    public Pose2D rotateDeg(float deg) { return rotate((float) Math.toRadians(deg)); }

    public Pose2D rotate(float rad) {
        //#if MC >= 1.21.6
        m.rotate(rad);
        //#else
        m.mulPose(com.mojang.math.Axis.ZP.rotation(rad));
        //#endif
        return this;
    }
}
