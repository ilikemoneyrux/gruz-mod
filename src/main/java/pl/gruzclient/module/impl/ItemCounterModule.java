package pl.gruzclient.module.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pl.gruzclient.module.HudModule;
import pl.gruzclient.module.Setting;

import java.util.ArrayList;
import java.util.List;

import static pl.gruzclient.render.GruzDraw.*;

/** Ile masz bloków, pereł, strzał i złotych jabłek – ikona + liczba. */
public class ItemCounterModule extends HudModule {
    public final Setting.Bool blocks = add(new Setting.Bool("blocks", "setting.countBlocks", true));
    public final Setting.Bool pearls = add(new Setting.Bool("pearls", "setting.countPearls", true));
    public final Setting.Bool arrows = add(new Setting.Bool("arrows", "setting.countArrows", true));
    public final Setting.Bool gapples = add(new Setting.Bool("gapples", "setting.countGapples", true));
    public final Setting.Bool hideEmpty = add(new Setting.Bool("hideEmpty", "setting.hideEmpty", true));

    public ItemCounterModule() { super("items", false, 0.88f, 0.30f); }

    private record Row(ItemStack icon, int count) {}

    @Override
    protected int[] draw(DrawContext c, boolean editing) {
        int nBlocks = 0, nPearls = 0, nArrows = 0, nGapples = 0;
        ItemStack blockIcon = new ItemStack(Items.WHITE_WOOL);
        if (mc.player != null) {
            var inv = mc.player.getInventory();
            List<ItemStack> all = new ArrayList<>(inv.main);
            all.addAll(inv.offHand);
            int bestBlocks = 0;
            for (ItemStack s : all) {
                if (s.isEmpty()) continue;
                Item it = s.getItem();
                if (it instanceof BlockItem) {
                    nBlocks += s.getCount();
                    if (s.getCount() > bestBlocks) { bestBlocks = s.getCount(); blockIcon = s; }
                } else if (it == Items.ENDER_PEARL) nPearls += s.getCount();
                else if (it == Items.ARROW || it == Items.SPECTRAL_ARROW || it == Items.TIPPED_ARROW) nArrows += s.getCount();
                else if (it == Items.GOLDEN_APPLE || it == Items.ENCHANTED_GOLDEN_APPLE) nGapples += s.getCount();
            }
        }
        if (editing && nBlocks + nPearls + nArrows + nGapples == 0) { nBlocks = 64; nPearls = 2; nArrows = 16; nGapples = 3; }

        List<Row> rows = new ArrayList<>();
        boolean hide = hideEmpty.value && !editing;
        if (blocks.value && !(hide && nBlocks == 0)) rows.add(new Row(blockIcon, nBlocks));
        if (pearls.value && !(hide && nPearls == 0)) rows.add(new Row(new ItemStack(Items.ENDER_PEARL), nPearls));
        if (arrows.value && !(hide && nArrows == 0)) rows.add(new Row(new ItemStack(Items.ARROW), nArrows));
        if (gapples.value && !(hide && nGapples == 0)) rows.add(new Row(new ItemStack(Items.GOLDEN_APPLE), nGapples));
        if (rows.isEmpty()) return new int[]{0, 0};

        var tr = mc.textRenderer;
        int w = 26;
        for (Row r : rows) w = Math.max(w, 26 + tr.getWidth(String.valueOf(r.count())) + 6);
        int h = rows.size() * 18 + 4;
        panel(c, w, h);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = 3 + i * 18;
            c.drawItem(r.icon(), 5, y);
            int col = r.count() == 0 ? 0xFFFF5C8A : textColor();
            c.drawText(tr, String.valueOf(r.count()), 25, y + 5, col, !background.value);
        }
        return new int[]{w, h};
    }
}
