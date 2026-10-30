package pl.gruzclient.module.impl;

import pl.gruzclient.i18n.Lang;
import pl.gruzclient.module.TextHudModule;

public class FpsModule extends TextHudModule {
    public FpsModule() { super("fps", true, 0.01f, 0.015f); }
    @Override protected String value(boolean editing) { return String.valueOf(mc.getFps()); }
    @Override protected String label() { return Lang.t("hud.fps"); }
}
