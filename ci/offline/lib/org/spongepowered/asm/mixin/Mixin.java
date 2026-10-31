package org.spongepowered.asm.mixin;
public @interface Mixin { Class<?>[] value() default {}; String[] targets() default {}; int priority() default 1000; boolean remap() default true; }
