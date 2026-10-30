package pl.gruzclient.module;

import com.google.gson.JsonObject;
import pl.gruzclient.i18n.Lang;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Bazowy moduł Gruz Client. */
public abstract class Module {
    public enum Category { HUD, PVP, VISUAL, UTILITY, COSMETICS }

    public final String id;
    public final Category category;
    private boolean enabled;
    protected final List<Setting> settings = new ArrayList<>();
    protected final Minecraft mc = Minecraft.getInstance();

    protected Module(String id, Category category, boolean enabledByDefault) {
        this.id = id;
        this.category = category;
        this.enabled = enabledByDefault;
    }

    public String name() { return Lang.t("module." + id); }
    public String description() { return Lang.t("module." + id + ".desc"); }
    public List<Setting> settings() { return settings; }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}
    /** Co klatkę logiki gry (20 razy na sekundę) */
    public void tick() {}

    public void load(JsonObject o) {
        if (o.has("enabled")) enabled = o.get("enabled").getAsBoolean();
        for (Setting s : settings) s.load(o);
    }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        for (Setting s : settings) s.save(o);
        return o;
    }
}
