package pl.gruzclient.cosmetics;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import pl.gruzclient.GruzClient;
import pl.gruzclient.render.GruzDraw;

/**
 * Kosmetyki 3D na graczu: nakrycia głowy (aureola, korona, cylinder, kapelusz czarownicy, czapka Mikołaja),
 * skrzydła (anioł, demon, kryształ, nietoperz), plecak / jetpack i pety (kryształ, dynia).
 * Wszystko budowane z prostopadłościanów i czworokątów w stylu Minecrafta.
 *
 * Ważne: bufor warstwy bierzemy tuż przed rysowaniem i nie przeplatamy dwóch warstw –
 * w ekwipunku pobranie drugiej warstwy zamyka pierwszą ("Not building!").
 */
public class CosmeticsFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier WHITE = Identifier.of(GruzClient.MOD_ID, "textures/gui/white.png");
    private static final int FULL = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private static final float PX = 1f / 16f;

    private VertexConsumerProvider buffers;
    private RenderLayer solidLayer, glowLayer, litLayer;

    public CosmeticsFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    private VertexConsumer solid() { return buffers.getBuffer(solidLayer); }
    private VertexConsumer glow() { return buffers.getBuffer(glowLayer); }
    /** Zwykłe oświetlenie (ciemnieje w nocy) – czapki, plecaki, skrzydła z piór */
    private VertexConsumer lit() { return buffers.getBuffer(litLayer); }

    @Override
    public void render(MatrixStack m, VertexConsumerProvider buffers, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        CosmeticSet set = Cosmetics.of(player);
        if (!set.has3d()) return;
        GruzDraw.tex(WHITE);
        this.buffers = buffers;
        this.solidLayer = RenderLayer.getEntityTranslucentEmissive(WHITE);
        this.glowLayer = RenderLayer.getEyes(WHITE);
        this.litLayer = RenderLayer.getEntityTranslucent(WHITE);
        float t = (player.age + tickDelta) / 20f;
        int color = set.color();
        int hot = GruzDraw.mix(color, 0xFFFFFF, 0.5f);

        if (set.hasHead()) {
            m.push();
            this.getContextModel().head.rotate(m);
            switch (set.head()) {
                case "halo" -> halo(m, t, color, hot);
                case "crown" -> crown(m, t, color, hot);
                case "tophat" -> topHat(m, light, color);
                case "witch" -> witchHat(m, light);
                case "santa" -> santaHat(m, light);
                default -> { }
            }
            m.pop();
        }
        if (set.hasWings()) {
            m.push();
            this.getContextModel().body.rotate(m);
            boolean fast = !player.isOnGround() || player.isSprinting() || player.isFallFlying();
            wings(m, light, set.wings(), t, fast, color);
            m.pop();
        }
        if (set.hasBack()) {
            m.push();
            this.getContextModel().body.rotate(m);
            if ("jetpack".equals(set.back())) jetpack(m, light, t, color, !player.isOnGround() || player.isSprinting());
            else backpack(m, light, color);
            m.pop();
        }
        if (set.hasPet()) {
            m.push();
            float a = t * 0.9f;
            m.translate(Math.cos(a) * 0.75, -0.55 + Math.sin(t * 2.1f) * 0.08, Math.sin(a) * 0.75);
            m.multiply(RotationAxis.POSITIVE_Y.rotation(t * 2f));
            if ("pumpkin".equals(set.pet())) pumpkinPet(m, t);
            else {
                crystal(m, solid(), 0.09f, 0.2f, color, hot, 230, FULL);
                crystal(m, glow(), 0.13f, 0.27f, color, color, 70, FULL);
            }
            m.pop();
        }
    }

    // ---------------- nakrycia głowy ----------------
    private void halo(MatrixStack m, float t, int color, int hot) {
        m.translate(0, -0.72 + Math.sin(t * 1.6f) * 0.025, 0);
        m.multiply(RotationAxis.POSITIVE_Y.rotation(t * 0.6f));
        int seg = 24;
        float r0 = 0.2f, r1 = 0.27f;
        VertexConsumer core = solid();
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            int col = GruzDraw.mix(color, hot, (float) Math.sin(a0 * 3 + t * 4) * 0.5f + 0.5f);
            quad(m, core, c0 * r0, 0, s0 * r0, c0 * r1, 0, s0 * r1, c1 * r1, 0, s1 * r1, c1 * r0, 0, s1 * r0, col, 235, FULL);
        }
        VertexConsumer g = glow();
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            quad(m, g, c0 * (r0 - 0.04f), 0, s0 * (r0 - 0.04f), c0 * (r1 + 0.05f), 0, s0 * (r1 + 0.05f),
                    c1 * (r1 + 0.05f), 0, s1 * (r1 + 0.05f), c1 * (r0 - 0.04f), 0, s1 * (r0 - 0.04f), color, 60, FULL);
        }
    }

    private void crown(MatrixStack m, float t, int color, int hot) {
        m.translate(0, -0.55, 0);
        VertexConsumer vc = solid();
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 + t * 0.3;
            m.push();
            m.translate(Math.cos(a) * 0.22, 0, Math.sin(a) * 0.22);
            m.multiply(RotationAxis.POSITIVE_Y.rotation((float) -a));
            crystal(m, vc, 0.05f, 0.16f + (float) Math.sin(t * 2f + i) * 0.015f, color, hot, 235, FULL);
            m.pop();
        }
    }

    private void topHat(MatrixStack m, int light, int color) {
        VertexConsumer vc = lit();
        int black = 0x1A1A20;
        box(m, vc, -5.2f, -8.6f, -5.2f, 5.2f, -8f, 5.2f, black, light);           // rondo
        box(m, vc, -3.6f, -16f, -3.6f, 3.6f, -8.6f, 3.6f, black, light);         // główka
        box(m, vc, -3.75f, -10.4f, -3.75f, 3.75f, -9.2f, 3.75f, color, light);   // wstążka w kolorze motywu
    }

    private void witchHat(MatrixStack m, int light) {
        VertexConsumer vc = lit();
        int hat = 0x2A1440, band = 0xFF7A1A;
        box(m, vc, -6.5f, -8.7f, -6.5f, 6.5f, -8f, 6.5f, hat, light);
        float[][] tiers = {{4.2f, 3.2f, 0f}, {3.2f, 3f, 0.4f}, {2.3f, 3f, 1.1f}, {1.4f, 2.8f, 2.1f}, {0.7f, 2.4f, 3.4f}};
        float y = -8.7f;
        for (int i = 0; i < tiers.length; i++) {
            float w = tiers[i][0], h = tiers[i][1], dz = tiers[i][2];
            box(m, vc, -w, y - h, -w + dz, w, y, w + dz, i == 0 ? hat : GruzDraw.mix(hat, 0x5A2A9A, i * 0.08f), light);
            if (i == 0) box(m, vc, -w - 0.1f, y - 1.2f, -w - 0.1f, w + 0.1f, y - 0.2f, w + 0.1f, band, FULL);
            y -= h;
        }
    }

    private void santaHat(MatrixStack m, int light) {
        VertexConsumer vc = lit();
        int red = 0xC8202A, white = 0xF4F4F8;
        box(m, vc, -4.6f, -9.4f, -4.6f, 4.6f, -7.6f, 4.6f, white, light);
        float[][] tiers = {{4f, 2.6f, 0.3f}, {3.2f, 2.4f, 1f}, {2.4f, 2.2f, 2f}, {1.6f, 2f, 3.2f}};
        float y = -9.4f, lastDz = 0;
        for (float[] tr : tiers) {
            box(m, vc, -tr[0], y - tr[1], -tr[0] + tr[2], tr[0], y, tr[0] + tr[2], red, light);
            y -= tr[1];
            lastDz = tr[2];
        }
        box(m, vc, -1.2f, y - 2.2f, lastDz + 0.4f, 1.2f, y + 0.2f, lastDz + 2.8f, white, light); // pompon
    }

    // ---------------- skrzydła ----------------
    private void wings(MatrixStack m, int light, String style, float t, boolean fast, int color) {
        float speed = fast ? 9f : 2.2f;
        float flap = (float) Math.sin(t * speed) * (fast ? 24f : 9f);
        int base, tip;
        boolean emissive = false;
        switch (style) {
            case "demon" -> { base = 0x3A0710; tip = 0x8A1020; }
            case "bat" -> { base = 0x141018; tip = 0x3A1A5A; }
            case "crystal" -> { base = GruzDraw.mix(color, 0x000000, 0.25f); tip = GruzDraw.mix(color, 0xFFFFFF, 0.55f); emissive = true; }
            default -> { base = 0xE8E6F0; tip = 0xFFFFFF; } // anioł
        }
        int lt = emissive ? FULL : light;
        boolean membrane = "demon".equals(style) || "bat".equals(style);
        for (int side = -1; side <= 1; side += 2) {
            m.push();
            m.translate(side * 1.5f * PX, 2.5f * PX, 2.6f * PX);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (-(28f + flap))));
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -12f));
            VertexConsumer vc = emissive ? solid() : lit();
            if (membrane) {
                // kości + błona
                float[] lens = {13f, 11f, 9f, 6.5f};
                for (int i = 0; i < lens.length; i++) {
                    float y0 = i * 2.4f - 1f;
                    box(m, vc, side > 0 ? 0 : -lens[i], y0, -0.3f, side > 0 ? lens[i] : 0, y0 + 0.6f, 0.3f, tip, lt);
                }
                for (int i = 0; i < lens.length - 1; i++) {
                    float y0 = i * 2.4f - 0.4f, l = lens[i + 1] - 0.5f;
                    box(m, vc, side > 0 ? 0.4f : -l, y0, -0.12f, side > 0 ? l : -0.4f, y0 + 2.4f, 0.12f, base, lt);
                }
                // pazurki na końcach
                box(m, vc, side > 0 ? 12.4f : -13.6f, -2.4f, -0.3f, side > 0 ? 13.6f : -12.4f, -0.9f, 0.3f, 0x0A0A0A, lt);
            } else {
                // pióra
                float[] lens = {14f, 12.5f, 11f, 9f, 7f, 5f};
                for (int i = 0; i < lens.length; i++) {
                    float y0 = i * 1.7f - 1.5f;
                    int col = GruzDraw.mix(base, tip, i / (float) lens.length);
                    box(m, vc, side > 0 ? 0 : -lens[i], y0, -0.35f + i * 0.05f, side > 0 ? lens[i] : 0, y0 + 2.2f, 0.35f + i * 0.05f, col, lt);
                    // końcówka pióra jaśniejsza
                    float tipLen = 2.5f;
                    box(m, vc, side > 0 ? lens[i] - tipLen : -lens[i], y0 + 0.3f, -0.4f, side > 0 ? lens[i] : -lens[i] + tipLen, y0 + 1.9f, 0.4f,
                            emissive ? GruzDraw.mix(tip, 0xFFFFFF, 0.3f) : tip, lt);
                }
            }
            m.pop();
        }
        if (emissive) {
            // poświata kryształowych skrzydeł
            VertexConsumer g = glow();
            for (int side = -1; side <= 1; side += 2) {
                m.push();
                m.translate(side * 1.5f * PX, 2.5f * PX, 2.6f * PX);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (-(28f + flap))));
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -12f));
                box(m, g, side > 0 ? 0 : -15f, -2.5f, -0.6f, side > 0 ? 15f : 0, 9f, 0.6f, color, FULL, 50);
                m.pop();
            }
        }
    }

    // ---------------- plecy ----------------
    private void backpack(MatrixStack m, int light, int color) {
        VertexConsumer vc = lit();
        int leather = 0x6B4A2B, dark = 0x4A311B;
        box(m, vc, -3.6f, 1f, 2.1f, 3.6f, 9.5f, 5.4f, leather, light);
        box(m, vc, -3.7f, 0.8f, 2f, 3.7f, 3.4f, 5.6f, dark, light);           // klapa
        box(m, vc, -2.4f, 5.4f, 5.4f, 2.4f, 8.6f, 6.3f, dark, light);         // kieszeń
        box(m, vc, -0.6f, 2.9f, 5.55f, 0.6f, 3.9f, 5.8f, color, FULL);        // klamra w kolorze motywu
        for (int s = -1; s <= 1; s += 2) {                                     // szelki
            box(m, vc, s * 2.6f - 0.6f, -0.2f, -2.25f, s * 2.6f + 0.6f, 0.4f, 2.2f, dark, light);
            box(m, vc, s * 2.6f - 0.6f, 0f, -2.3f, s * 2.6f + 0.6f, 7f, -2.05f, dark, light);
        }
    }

    private void jetpack(MatrixStack m, int light, float t, int color, boolean active) {
        VertexConsumer vc = lit();
        int metal = 0x8C8F99, dark = 0x4A4D57;
        for (int s = -1; s <= 1; s += 2) {
            float x0 = s > 0 ? 0.4f : -3.4f, x1 = s > 0 ? 3.4f : -0.4f;
            box(m, vc, x0, 1f, 2.1f, x1, 9f, 5.1f, metal, light);
            box(m, vc, x0 + 0.2f, 0.4f, 2.3f, x1 - 0.2f, 1.2f, 4.9f, dark, light);
            box(m, vc, x0 + 0.5f, 9f, 2.6f, x1 - 0.5f, 10.2f, 4.6f, dark, light);
            box(m, vc, x0 + 0.8f, 3.5f, 5.1f, x1 - 0.8f, 4.5f, 5.3f, color, FULL); // świecący pasek
        }
        // płomienie z dysz (gdy lecisz albo biegniesz)
        VertexConsumer g = glow();
        float flick = 0.7f + 0.3f * (float) Math.sin(t * 40f);
        float len = active ? 5f * flick : 1.2f * flick;
        for (int s = -1; s <= 1; s += 2) {
            float cx = s * 1.9f;
            box(m, g, cx - 0.9f, 10.2f, 2.9f, cx + 0.9f, 10.2f + len, 4.3f, 0xFF8A1A, FULL, 200);
            box(m, g, cx - 0.5f, 10.2f, 3.3f, cx + 0.5f, 10.2f + len * 0.6f, 3.9f, 0xFFF1A0, FULL, 230);
        }
    }

    // ---------------- pety ----------------
    private void pumpkinPet(MatrixStack m, float t) {
        VertexConsumer vc = solid();
        float s = 2.8f;
        box(m, vc, -s, -s, -s, s, s, s, 0xE8701A, FULL);
        box(m, vc, -0.5f, -s - 1.4f, -0.5f, 0.5f, -s, 0.5f, 0x4E7A22, FULL);
        int face = GruzDraw.mix(0xFFD35C, 0xFFF1A0, (float) Math.sin(t * 5f) * 0.5f + 0.5f);
        box(m, vc, -2f, -1.4f, -s - 0.05f, -0.8f, -0.2f, -s, face, FULL);
        box(m, vc, 0.8f, -1.4f, -s - 0.05f, 2f, -0.2f, -s, face, FULL);
        box(m, vc, -2f, 0.8f, -s - 0.05f, 2f, 1.6f, -s, face, FULL);
    }

    /** Kryształ (ośmiościan wydłużony w pionie) */
    private void crystal(MatrixStack m, VertexConsumer vc, float w, float h, int color, int tip, int alpha, int light) {
        float[][] ring = {{w, 0, 0}, {0, 0, w}, {-w, 0, 0}, {0, 0, -w}};
        float[] top = {0, -h, 0}, bottom = {0, h * 0.6f, 0};
        for (int i = 0; i < 4; i++) {
            float[] p = ring[i], q = ring[(i + 1) % 4];
            tri(m, vc, top, p, q, tip, color, alpha, light);
            tri(m, vc, bottom, q, p, GruzDraw.mix(color, 0x000000, 0.35f), color, alpha, light);
        }
    }

    // ---------------- geometria ----------------
    /** Prostopadłościan w pikselach modelu (1/16 bloku), ściany lekko cieniowane */
    private void box(MatrixStack m, VertexConsumer vc, float x1, float y1, float z1, float x2, float y2, float z2, int color, int light) {
        box(m, vc, x1, y1, z1, x2, y2, z2, color, light, 255);
    }

    private void box(MatrixStack m, VertexConsumer vc, float x1, float y1, float z1, float x2, float y2, float z2, int color, int light, int alpha) {
        x1 *= PX; y1 *= PX; z1 *= PX; x2 *= PX; y2 *= PX; z2 *= PX;
        int top = GruzDraw.mix(color, 0xFFFFFF, 0.12f), side = color, side2 = GruzDraw.mix(color, 0x000000, 0.12f), bottom = GruzDraw.mix(color, 0x000000, 0.3f);
        quad(m, vc, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, top, alpha, light);      // góra (y w dół)
        quad(m, vc, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, bottom, alpha, light);   // dół
        quad(m, vc, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, side, alpha, light);     // przód
        quad(m, vc, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, side, alpha, light);     // tył
        quad(m, vc, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, side2, alpha, light);    // lewo
        quad(m, vc, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, side2, alpha, light);    // prawo
    }

    private void tri(MatrixStack m, VertexConsumer vc, float[] a, float[] b, float[] c, int ca, int cb, int alpha, int light) {
        MatrixStack.Entry e = m.peek();
        vertex(vc, e, a[0], a[1], a[2], ca, alpha, light);
        vertex(vc, e, b[0], b[1], b[2], cb, alpha, light);
        vertex(vc, e, c[0], c[1], c[2], cb, alpha, light);
        vertex(vc, e, c[0], c[1], c[2], cb, alpha, light);
        vertex(vc, e, a[0], a[1], a[2], ca, alpha, light);
        vertex(vc, e, c[0], c[1], c[2], cb, alpha, light);
        vertex(vc, e, b[0], b[1], b[2], cb, alpha, light);
        vertex(vc, e, b[0], b[1], b[2], cb, alpha, light);
    }

    /** Czworokąt widoczny z obu stron */
    private void quad(MatrixStack m, VertexConsumer vc, float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4, int color, int alpha, int light) {
        MatrixStack.Entry e = m.peek();
        vertex(vc, e, x1, y1, z1, color, alpha, light);
        vertex(vc, e, x2, y2, z2, color, alpha, light);
        vertex(vc, e, x3, y3, z3, color, alpha, light);
        vertex(vc, e, x4, y4, z4, color, alpha, light);
        vertex(vc, e, x4, y4, z4, color, alpha, light);
        vertex(vc, e, x3, y3, z3, color, alpha, light);
        vertex(vc, e, x2, y2, z2, color, alpha, light);
        vertex(vc, e, x1, y1, z1, color, alpha, light);
    }

    private void vertex(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, int rgb, int alpha, int light) {
        vc.vertex(e, x, y, z)
                .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha)
                .texture(0.5f, 0.5f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(e, 0f, -1f, 0f);
    }
}
