package pl.gruzclient.cosmetics;

import pl.gruzclient.compat.Compat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
//#if MC < 26.2
import net.minecraft.client.renderer.MultiBufferSource;
//#endif
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import pl.gruzclient.GruzClient;
import pl.gruzclient.render.GruzDraw;

/**
 * Kosmetyki 3D na graczu: nakrycia głowy (aureola, korona, cylinder, kapelusz czarownicy, czapka Mikołaja),
 * skrzydła (anioł, demon, kryształ, nietoperz), plecak / jetpack i pety (kryształ, dynia).
 * Kształty liczą HatMesh i WingMesh (te same co w launcherze i na stronie).
 *
 * Ważne: bufor warstwy bierzemy tuż przed rysowaniem i nie przeplatamy dwóch warstw –
 * w ekwipunku pobranie drugiej warstwy zamyka pierwszą ("Not building!").
 */
//#if MC >= 1.21.2
public class CosmeticsFeature extends RenderLayer<net.minecraft.client.renderer.entity.state.PlayerRenderState, PlayerModel> {
//#else
public class CosmeticsFeature extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
//#endif
    private static final ResourceLocation WHITE = Compat.id(GruzClient.MOD_ID, "textures/gui/white.png");
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final float PX = 1f / 16f;

    private RenderType solidLayer, glowLayer, litLayer;
    private Sink solidSink, glowSink, litSink;
    //#if MC >= 1.21.9
    private net.minecraft.client.renderer.SubmitNodeCollector collector;
    //#else
    private MultiBufferSource buffers;
    //#endif

    //#if MC >= 1.21.2
    public CosmeticsFeature(RenderLayerParent<net.minecraft.client.renderer.entity.state.PlayerRenderState, PlayerModel> context) {
    //#else
    public CosmeticsFeature(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
    //#endif
        super(context);
    }

    private Sink solid() { return solidSink.open(); }
    private Sink glow() { return glowSink.open(); }
    /** Zwykłe oświetlenie (ciemnieje w nocy) – czapki, plecaki, skrzydła z piór */
    private Sink lit() { return litSink.open(); }

    /**
     * Cel rysowania jednej warstwy. Do 1.21.8 to po prostu bufor wierzchołków z MultiBufferSource.
     * Od 1.21.9 gra przyjmuje tylko "zlecenia" (SubmitNodeCollector), więc wierzchołki zbieramy
     * (już przekształcone) i na końcu oddajemy jednym submitCustomGeometry.
     */
    private final class Sink {
        private final RenderType layer;
        //#if MC >= 1.21.9
        private float[] data = new float[1024];
        private int n;
        //#else
        private VertexConsumer out;
        //#endif

        Sink(RenderType layer) { this.layer = layer; }

        Sink open() {
            //#if MC < 1.21.9
            out = buffers.getBuffer(layer); // bufor bierzemy tuż przed rysowaniem – nie przeplatamy warstw
            //#endif
            return this;
        }

        void vertex(PoseStack.Pose e, float x, float y, float z, int rgb, int alpha, int light) {
            //#if MC >= 1.21.9
            org.joml.Vector3f p = e.pose().transformPosition(x, y, z, new org.joml.Vector3f());
            org.joml.Vector3f nr = e.normal().transform(new org.joml.Vector3f(0f, -1f, 0f));
            if (n + 12 > data.length) data = java.util.Arrays.copyOf(data, data.length * 2);
            data[n++] = p.x; data[n++] = p.y; data[n++] = p.z;
            data[n++] = (rgb >> 16) & 255; data[n++] = (rgb >> 8) & 255; data[n++] = rgb & 255; data[n++] = alpha;
            data[n++] = Float.intBitsToFloat(light);
            data[n++] = nr.x; data[n++] = nr.y; data[n++] = nr.z;
            data[n++] = 0f;
            //#elif MC >= 1.21
            out.addVertex(e, x, y, z)
                    .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha)
                    .setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(e, 0f, -1f, 0f);
            //#else
            out.vertex(e.pose(), x, y, z)
                    .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha)
                    .uv(0.5f, 0.5f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(e.normal(), 0f, -1f, 0f)
                    .endVertex();
            //#endif
        }

        void flush() {
            //#if MC >= 1.21.9
            if (n == 0) return;
            final float[] d = java.util.Arrays.copyOf(data, n);
            n = 0;
            collector.submitCustomGeometry(new PoseStack(), layer, (pose, out) -> {
                for (int i = 0; i < d.length; i += 12) {
                    out.addVertex(pose, d[i], d[i + 1], d[i + 2])
                            .setColor((int) d[i + 3], (int) d[i + 4], (int) d[i + 5], (int) d[i + 6])
                            .setUv(0.5f, 0.5f)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(Float.floatToRawIntBits(d[i + 7]))
                            .setNormal(pose, d[i + 8], d[i + 9], d[i + 10]);
                }
            });
            //#endif
        }
    }

    //#if MC >= 1.21.9
    @Override
    public void submit(PoseStack m, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, net.minecraft.client.renderer.entity.state.PlayerRenderState state,
                       float headYaw, float headPitch) {
        this.collector = collector;
        if (state.isInvisible) return;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(state.id) instanceof AbstractClientPlayer player)) return;
        float tickDelta = state.ageInTicks - player.tickCount;
    //#elif MC >= 1.21.2
    @Override
    public void render(PoseStack m, MultiBufferSource buffers, int light, net.minecraft.client.renderer.entity.state.PlayerRenderState state,
                       float headYaw, float headPitch) {
        if (state.isInvisible) return;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(state.id) instanceof AbstractClientPlayer player)) return;
        float tickDelta = state.ageInTicks - player.tickCount;
    //#else
    @Override
    public void render(PoseStack m, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
    //#endif
        CosmeticSet set = Cosmetics.of(player);
        if (!set.has3d()) return;
        GruzDraw.tex(WHITE);
        //#if MC < 1.21.9
        this.buffers = buffers;
        //#endif
        this.solidLayer = RenderType.entityTranslucentEmissive(WHITE);
        this.glowLayer = RenderType.eyes(WHITE);
        this.litLayer = RenderType.entityTranslucent(WHITE);
        this.solidSink = new Sink(solidLayer);
        this.glowSink = new Sink(glowLayer);
        this.litSink = new Sink(litLayer);
        try {
            draw(m, light, player, set, tickDelta);
        } finally {
            solidSink.flush();
            glowSink.flush();
            litSink.flush();
        }
    }

    private void draw(PoseStack m, int light, AbstractClientPlayer player, CosmeticSet set, float tickDelta) {
        float t = (player.tickCount + tickDelta) / 20f;
        int color = set.color();

        if (set.hasHead()) {
            m.pushPose();
            this.getParentModel().head.translateAndRotate(m);
            hat(m, light, set.head(), t, color);
            m.popPose();
        }
        if (set.hasWings()) {
            m.pushPose();
            this.getParentModel().body.translateAndRotate(m);
            boolean fast = !player.onGround() || player.isSprinting() || player.isFallFlying();
            wings(m, light, set.wings(), t, fast, color);
            m.popPose();
        }
        if (set.hasBack()) {
            m.pushPose();
            this.getParentModel().body.translateAndRotate(m);
            boolean jet = "jetpack".equals(set.back());
            HatMesh.build(jet ? "jetpack" : "backpack", color);
            mesh(m, lit(), 1f, 255, light);
            if (jet) {
                // płomienie z dysz: długie, gdy lecisz albo biegniesz
                HatMesh.build("flame", color);
                float len = (!player.onGround() || player.isSprinting() ? 5f : 1.2f) * (0.7f + 0.3f * (float) Math.sin(t * 40f));
                for (int s = -1; s <= 1; s += 2) {
                    m.pushPose();
                    m.translate(s * 1.95f * PX, 10.4f * PX, 3.7f * PX);
                    m.scale(1f, len, 1f);
                    mesh(m, glow(), 1f, 200, FULL);
                    m.popPose();
                }
            }
            m.popPose();
        }
        if (set.hasPet()) {
            m.pushPose();
            float a = t * 0.9f;
            m.translate(Math.cos(a) * 0.75, -0.55 + Math.sin(t * 2.1f) * 0.08, Math.sin(a) * 0.75);
            m.mulPose(Axis.YP.rotation(t * 2f));
            if ("pumpkin".equals(set.pet())) {
                HatMesh.build("petpumpkin", color);
                mesh(m, solid(), 1f, 255, FULL);
            } else {
                HatMesh.build("petcrystal", color);
                mesh(m, solid(), 1f, 230, FULL);
                mesh(m, glow(), 1.3f, 70, FULL); // poświata
            }
            m.popPose();
        }
    }

    // ---------------- nakrycia głowy ----------------
    /** Okrągłe bryły zamiast klocków – kształt liczy HatMesh (ten sam co w launcherze i na stronie) */
    private void hat(PoseStack m, int light, String style, float t, int color) {
        boolean emissive = HatMesh.build(style, color);
        if ("halo".equals(style)) {
            // aureola unosi się i powoli obraca
            m.translate(0, Math.sin(t * 1.6f) * 0.025, 0);
            m.mulPose(Axis.YP.rotation(t * 0.6f));
        }
        for (int pass = 0; pass < (emissive ? 2 : 1); pass++) {
            Sink vc = pass == 1 ? glow() : emissive ? solid() : lit();
            int lt = emissive ? FULL : light, alpha = pass == 1 ? 60 : 255;
            // poświata: druga, grubsza obręcz (skalowana od środka aureoli)
            float grow = pass == 1 ? 1.12f : 1f;
            PoseStack.Pose e = m.last();
            float[] p = HatMesh.POS;
            int[] c = HatMesh.COL;
            for (int i = 0; i + 2 < HatMesh.N; i += 3) {
                // trójkąt jako czworokąt z powtórzonym wierzchołkiem, z obu stron
                for (int k = 0; k < 8; k++) {
                    int v = i + (k < 4 ? Math.min(k, 2) : 2 - Math.min(k - 4, 2));
                    vertex(vc, e, p[v * 3] * grow * PX, (-11.2f + (p[v * 3 + 1] + 11.2f) * (pass == 1 ? 1.6f : 1f)) * PX, p[v * 3 + 2] * grow * PX, c[v], alpha, lt);
                }
            }
        }
    }

    /** Skrzydła z piór albo błon – kształt liczy WingMesh (ten sam co w launcherze i na stronie) */
    private void wings(PoseStack m, int light, String style, float t, boolean fast, int color) {
        float speed = fast ? 9f : 2.2f;
        float flap = (float) Math.sin(t * speed) * (fast ? 24f : 9f);
        // końcówka skrzydła zgina się z lekkim opóźnieniem względem machnięcia
        float fold = 0.2f + (float) Math.sin(t * speed - 0.7f) * (fast ? 0.4f : 0.22f);
        boolean emissive = WingMesh.build(style, color, fold);
        // kryształowe świecą: drugi, lekko większy i przezroczysty przebieg daje poświatę
        for (int pass = 0; pass < (emissive ? 2 : 1); pass++) {
            Sink vc = pass == 1 ? glow() : emissive ? solid() : lit();
            int lt = emissive ? FULL : light, alpha = pass == 1 ? 55 : 255;
            float grow = (pass == 1 ? 1.07f : 1f) * PX, zoff = pass == 1 ? 0.5f : 0f;
            for (int side = -1; side <= 1; side += 2) {
                m.pushPose();
                m.translate(side * 1.5f * PX, 2.5f * PX, 2.6f * PX);
                m.mulPose(Axis.YP.rotationDegrees(side * (-(24f + flap))));
                m.mulPose(Axis.ZP.rotationDegrees(side * -8f));
                PoseStack.Pose e = m.last();
                float[] p = WingMesh.POS;
                int[] c = WingMesh.COL;
                for (int i = 0; i + 2 < WingMesh.N; i += 3) {
                    // trójkąt jako czworokąt z powtórzonym wierzchołkiem, z obu stron
                    for (int k = 0; k < 8; k++) {
                        int v = i + (k < 4 ? Math.min(k, 2) : 2 - Math.min(k - 4, 2));
                        vertex(vc, e, p[v * 3] * side * grow, p[v * 3 + 1] * grow, (p[v * 3 + 2] + zoff) * PX, c[v], alpha, lt);
                    }
                }
                m.popPose();
            }
        }
    }

    /** Rysuje ostatnią siatkę z HatMesh (plecy, pety): trójkąt jako czworokąt z powtórzonym wierzchołkiem, z obu stron */
    private void mesh(PoseStack m, Sink vc, float grow, int alpha, int light) {
        PoseStack.Pose e = m.last();
        float[] p = HatMesh.POS;
        int[] c = HatMesh.COL;
        float k = grow * PX;
        for (int i = 0; i + 2 < HatMesh.N; i += 3) {
            for (int q = 0; q < 8; q++) {
                int v = i + (q < 4 ? Math.min(q, 2) : 2 - Math.min(q - 4, 2));
                vertex(vc, e, p[v * 3] * k, p[v * 3 + 1] * k, p[v * 3 + 2] * k, c[v], alpha, light);
            }
        }
    }

    private void vertex(Sink vc, PoseStack.Pose e, float x, float y, float z, int rgb, int alpha, int light) {
        vc.vertex(e, x, y, z, rgb, alpha, light);
    }
}
