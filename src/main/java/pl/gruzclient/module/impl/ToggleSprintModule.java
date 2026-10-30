package pl.gruzclient.module.impl;

import pl.gruzclient.module.Module;

/** Toggle Sprint – korzysta z wbudowanej opcji Minecrafta, włącza/wyłącza ją razem z modułem. */
public class ToggleSprintModule extends Module {
    public ToggleSprintModule() { super("togglesprint", Category.UTILITY, false); }

    @Override
    protected void onEnable() { mc.options.getSprintToggled().setValue(true); mc.options.write(); }

    @Override
    protected void onDisable() { mc.options.getSprintToggled().setValue(false); mc.options.write(); }
}
