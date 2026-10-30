package pl.gruzclient.screen;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static pl.gruzclient.render.GruzDraw.*;

/** Tło: kryształki powoli unoszące się do góry, migoczące, z poświatą. */
public class CrystalField {
    private static final class Mote {
        float x, y, vy, vx, rot, vr, size, tw, depth;
    }

    private final List<Mote> motes = new ArrayList<>();
    private final Random rnd = new Random();

    public CrystalField(int count) {
        for (int i = 0; i < count; i++) {
            Mote m = new Mote();
            m.x = rnd.nextFloat();
            m.y = rnd.nextFloat();
            m.depth = rnd.nextFloat();
            m.vy = 0.008f + rnd.nextFloat() * 0.02f;
            m.vx = (rnd.nextFloat() - 0.5f) * 0.006f;
            m.rot = rnd.nextFloat() * 6.28f;
            m.vr = (rnd.nextFloat() - 0.5f) * 0.5f;
            m.size = 3f + rnd.nextFloat() * 7f;
            m.tw = rnd.nextFloat() * 6.28f;
            motes.add(m);
        }
    }

    public void render(DrawContext c, int w, int h, float dt, float alpha) {
        for (Mote m : motes) {
            m.y -= m.vy * dt * (0.4f + m.depth);
            m.x += m.vx * dt;
            m.rot += m.vr * dt;
            m.tw += dt * 1.8f;
            if (m.y < -0.05f) { m.y = 1.05f; m.x = rnd.nextFloat(); }
            float a = alpha * (0.25f + 0.55f * m.depth) * (0.6f + 0.4f * (float) Math.sin(m.tw));
            float s = m.size * (0.6f + m.depth * 0.8f);
            float px = m.x * w, py = m.y * h;
            sprite(c, GLOW, 256, px - s * 2f, py - s * 2f, s * 4f, s * 4f, a * 0.35f, true, 0f);
            sprite(c, CRYSTAL, 32, px - s / 2f, py - s, s, s * 2f, a, false, m.rot * 0.3f);
        }
    }
}
