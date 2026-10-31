package org.spongepowered.asm.mixin;
public @interface Shadow { String[] aliases() default {}; String prefix() default ""; boolean remap() default true; boolean silent() default false; }
