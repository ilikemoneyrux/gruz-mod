package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;

/** Toggle Sprint – korzysta z wbudowanej opcji Minecrafta, włącza/wyłącza ją razem z modułem. */
public class ToggleSprintModule extends Module {
    public ToggleSprintModule() { super("togglesprint", Category.UTILITY, false); }

    @Override
    protected void onEnable() { mc.options.toggleSprint().set(true); mc.options.save(); }

    @Override
    protected void onDisable() { mc.options.toggleSprint().set(false); mc.options.save(); }
}
