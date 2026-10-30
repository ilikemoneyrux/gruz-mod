package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import java.util.ArrayList;
import java.util.List;

import static pl.gruzclient.render.GruzDraw.*;

/** Zbroja (od hełmu do butów) + przedmiot w ręce, z wytrzymałością. */
public class ArmorHudModule extends HudModule {
    public final Setting.Bool durability = add(new Setting.Bool("durability", "setting.durability", true));

    public ArmorHudModule() { super("armor", true, 0.88f, 0.70f); }

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        List<ItemStack> items = new ArrayList<>();
        if (mc.player != null) {
            var armor = mc.player.getInventory().armor;
            for (int i = armor.size() - 1; i >= 0; i--) if (!armor.get(i).isEmpty()) items.add(armor.get(i));
            if (!mc.player.getMainHandStack().isEmpty()) items.add(mc.player.getMainHandStack());
        }
        if (items.isEmpty() && editing) {
            items.add(new ItemStack(Items.DIAMOND_HELMET));
            items.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            items.add(new ItemStack(Items.DIAMOND_LEGGINGS));
            items.add(new ItemStack(Items.DIAMOND_BOOTS));
        }
        var tr = mc.textRenderer;
        int w = 0;
        for (ItemStack s : items) w = Math.max(w, 20 + (durability.value ? tr.getWidth(label(s)) + 4 : 0));
        w = Math.max(w + 6, 26);
        int h = Math.max(items.size() * 18 + 4, 22);
        panel(c, w, h);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            int y = 3 + i * 18;
            c.drawItem(s, 5, y);
            c.drawItemInSlot(tr, s, 5, y);
            if (durability.value) {
                String l = label(s);
                int col = textColor();
                if (s.isDamageable()) {
                    float left = 1f - (float) s.getDamage() / s.getMaxDamage();
                    if (left < 0.25f) col = 0xFFFF5C8A;
                    else if (left < 0.5f) col = 0xFFFFD35C;
                }
                c.drawText(tr, l, 25, y + 5, col, !background.value);
            }
        }
        return new int[]{w, h};
    }

    private static String label(ItemStack s) {
        if (s.isDamageable()) return String.valueOf(s.getMaxDamage() - s.getDamage());
        return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
    }
}
