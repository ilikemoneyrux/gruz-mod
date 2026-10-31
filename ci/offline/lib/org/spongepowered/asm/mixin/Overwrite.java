package org.spongepowered.asm.mixin;
public @interface Overwrite { String[] aliases() default {}; String prefix() default ""; boolean remap() default true; boolean silent() default false; }
