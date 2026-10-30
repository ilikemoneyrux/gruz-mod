package pl.gruzclient.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import pl.gruzclient.i18n.Lang;

/** Ustawienie modułu. Typy: przełącznik, wybór z listy, liczba, klawisz. */
public abstract class Setting {
    public final String id;
    public final String labelKey;

    protected Setting(String id, String labelKey) {
        this.id = id;
        this.labelKey = labelKey;
    }

    public String label() { return Lang.t(labelKey); }
    public abstract String display();
    public abstract void load(JsonObject o);
    public abstract void save(JsonObject o);

    // ---------------- typy ----------------

    public static class Bool extends Setting {
        public boolean value;
        public Bool(String id, String labelKey, boolean def) { super(id, labelKey); value = def; }
        public void toggle() { value = !value; }
        @Override public String display() { return Lang.t(value ? "mods.on" : "mods.off"); }
        @Override public void load(JsonObject o) { if (o.has(id)) value = o.get(id).getAsBoolean(); }
        @Override public void save(JsonObject o) { o.addProperty(id, value); }
    }

    public static class Choice extends Setting {
        public final String[] keys;
        public int index;
        public Choice(String id, String labelKey, int def, String... keys) { super(id, labelKey); this.keys = keys; index = def; }
        public void next(int dir) { index = Math.floorMod(index + dir, keys.length); }
        @Override public String display() { return Lang.t(keys[index]); }
        @Override public void load(JsonObject o) { if (o.has(id)) index = Math.floorMod(o.get(id).getAsInt(), keys.length); }
        @Override public void save(JsonObject o) { o.addProperty(id, index); }
    }

    public static class Number extends Setting {
        public final float min, max, step;
        public float value;
        public final String suffix;
        public Number(String id, String labelKey, float def, float min, float max, float step, String suffix) {
            super(id, labelKey); value = def; this.min = min; this.max = max; this.step = step; this.suffix = suffix;
        }
        public void set(float v) { value = Math.round(Math.max(min, Math.min(max, v)) / step) * step; }
        public float fraction() { return (value - min) / (max - min); }
        @Override public String display() {
            return (step >= 1 ? String.valueOf(Math.round(value)) : String.format("%.1f", value)) + suffix;
        }
        @Override public void load(JsonObject o) { if (o.has(id)) set(o.get(id).getAsFloat()); }
        @Override public void save(JsonObject o) { o.addProperty(id, value); }
    }

    public static class Key extends Setting {
        public int code;
        public Key(String id, String labelKey, int def) { super(id, labelKey); code = def; }
        @Override public String display() {
            return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
        }
        @Override public void load(JsonObject o) { if (o.has(id)) code = o.get(id).getAsInt(); }
        @Override public void save(JsonObject o) { o.addProperty(id, code); }
    }

    /** Tekst (np. kod celownika) */
    public static class Text extends Setting {
        public String value;
        public boolean secret;
        public Text(String id, String labelKey, String def) { super(id, labelKey); value = def; }
        /** Hasło: w panelu pokazujemy kropki */
        public Text secret() { secret = true; return this; }
        @Override public String display() {
            if (secret) return value.isEmpty() ? "—" : "•".repeat(Math.min(8, value.length()));
            return value.length() > 10 ? value.substring(0, 10) + "…" : value;
        }
        @Override public void load(JsonObject o) { if (o.has(id)) value = o.get(id).getAsString(); }
        @Override public void save(JsonObject o) { o.addProperty(id, value); }
    }

    /** Przycisk w panelu ustawień (np. "Edytuj piksele") */
    public static class Action extends Setting {
        public final Runnable action;
        public Action(String id, String labelKey, Runnable action) { super(id, labelKey); this.action = action; }
        @Override public String display() { return "›"; }
        @Override public void load(JsonObject o) {}
        @Override public void save(JsonObject o) {}
    }

    /** Wybór koloru z palety Gruz */
    public static class Color extends Choice {
        public static final String[] KEYS = {"color.white", "color.neon", "color.lilac", "color.cyan", "color.green", "color.red", "color.yellow"};
        public static final int[] RGB = {0xFFFFFF, pl.gruzclient.render.GruzDraw.NEON, pl.gruzclient.render.GruzDraw.NEON_HOT, 0x5CE1FF, 0x7DFFB2, 0xFF5C8A, 0xFFD35C};
        public Color(String id, String labelKey, int def) { super(id, labelKey, def, KEYS); }
        public int rgb() { return RGB[index]; }
    }
}
