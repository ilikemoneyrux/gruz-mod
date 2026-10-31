package org.spongepowered.asm.mixin.injection;
public @interface At { String value(); String target() default ""; String id() default ""; int ordinal() default -1; int opcode() default -1; String[] args() default {}; boolean remap() default true; Shift shift() default Shift.NONE; int by() default 0; Slice slice() default @Slice; enum Shift { NONE, BEFORE, AFTER, BY } }
