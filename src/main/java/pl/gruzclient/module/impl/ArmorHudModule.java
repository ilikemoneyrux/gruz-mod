package pl.gruzclient.module.impl;

import pl.gruzclient.compat.Compat;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static pl.gruzclient.render.GruzDraw.*;

/** Zbroja (od hełmu do butów) + przedmiot w ręce, z wytrzymałością. */
public class ArmorHudModule extends HudModule {
    public final Setting.Bool durability = add(new Setting.Bool("durability", "setting.durability", true));

    public ArmorHudModule() { super("armor", true, 0.88f, 0.70f); }

    @Override
    protected int[] draw(GuiGraphics c, boolean editing) {
        List<ItemStack> items = new ArrayList<>();
        if (mc.player != null) {
            for (var it : Compat.armor(mc.player)) if (!it.isEmpty()) items.add(it);
            if (!mc.player.getMainHandItem().isEmpty()) items.add(mc.player.getMainHandItem());
        }
        if (items.isEmpty() && editing) {
            items.add(Compat.stack(Items.DIAMOND_HELMET));
            items.add(Compat.stack(Items.DIAMOND_CHESTPLATE));
            items.add(Compat.stack(Items.DIAMOND_LEGGINGS));
            items.add(Compat.stack(Items.DIAMOND_BOOTS));
        }
        var tr = mc.font;
        int w = 0;
        for (ItemStack s : items) w = Math.max(w, 20 + (durability.value ? tr.width(label(s)) + 4 : 0));
        w = Math.max(w + 6, 26);
        int h = Math.max(items.size() * 18 + 4, 22);
        panel(c, w, h);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            int y = 3 + i * 18;
            c.renderItem(s, 5, y);
            c.renderItemDecorations(tr, s, 5, y);
            if (durability.value) {
                String l = label(s);
                int col = textColor();
                if (s.isDamageableItem()) {
                    float left = 1f - (float) s.getDamageValue() / s.getMaxDamage();
                    if (left < 0.25f) col = 0xFFFF5C8A;
                    else if (left < 0.5f) col = 0xFFFFD35C;
                }
                c.drawString(tr, l, 25, y + 5, col, !background.value);
            }
        }
        return new int[]{w, h};
    }

    private static String label(ItemStack s) {
        if (s.isDamageableItem()) return String.valueOf(s.getMaxDamage() - s.getDamageValue());
        return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
    }
}
