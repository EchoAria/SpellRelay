package dev.ironautospell;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class BookAccess {
    public static ItemStack book() {
        var player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;
        var stack = Utils.getPlayerSpellbookStack(player);
        return stack == null ? ItemStack.EMPTY : stack;
    }
    public static ISpellContainer container() {
        var stack = book();
        return ISpellContainer.isSpellContainer(stack) ? ISpellContainer.get(stack) : null;
    }
    public static SpellData spell(int oneBasedSlot) {
        var c = container();
        if (c == null || oneBasedSlot < 1 || oneBasedSlot > c.getMaxSpellCount()) return SpellData.EMPTY;
        var data = c.getSpellAtIndex(oneBasedSlot - 1);
        return data == null ? SpellData.EMPTY : data;
    }
    /** Immutable description of the book, not the mutable currently selected wheel entry. */
    public static String signature() {
        var c = container();
        if (c == null) return "";
        StringBuilder s = new StringBuilder(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(book().getItem()).toString());
        for (int i = 1; i <= c.getMaxSpellCount(); i++) {
            var d = spell(i);
            s.append('|').append(d.getSpell().getSpellId()).append(':').append(d.getLevel()).append(':').append(d.isLocked());
        }
        return s.toString();
    }
    public static int resolve(int slot, String spellId) {
        var player = Minecraft.getInstance().player;
        var c = container();
        if (player == null || c == null) return -1;
        var raw = spell(slot);
        if (raw == SpellData.EMPTY || raw.isLocked() || !raw.getSpell().getSpellId().equals(spellId)) return -1;
        var active = c.getActiveSpells();
        int local = -1;
        for (int i = 0; i < active.size(); i++) if (active.get(i).index() == slot - 1) { local = i; break; }
        if (local < 0) return -1;
        var all = new SpellSelectionManager(player).getAllSpells();
        for (int i = 0; i < all.size(); i++) {
            var option = all.get(i);
            if (option.slot.equals(Curios.SPELLBOOK_SLOT) && option.slotIndex == local
                    && option.spellData.getSpell().getSpellId().equals(spellId)
                    && option.spellData.getLevel() == raw.getLevel()) return i;
        }
        // Do not silently cast a merged spell supplied by another equipment slot.
        return -1;
    }
    private BookAccess() {}
}
