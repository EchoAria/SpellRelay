package dev.ironautospell.smoke;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;
import java.nio.file.*;
import java.util.*;

public class ServerFixture {
    private UUID pending;
    private int ticks;
    private final List<String> casts = new ArrayList<>();
    public ServerFixture() {
        if (ModList.get().isLoaded("ironautospell")) throw new IllegalStateException("Production mod must NOT be installed on this test server");
        MinecraftForge.EVENT_BUS.addListener(this::login);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::cast);
    }
    private void login(PlayerEvent.PlayerLoggedInEvent e) { pending=e.getEntity().getUUID(); ticks=0; }
    private void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (pending==null || ++ticks<60) return;
        ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(pending);
        if (p==null) return;
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(10000);
        MagicData.getPlayerMagicData(p).setMana(10000);
        MagicData.getPlayerMagicData(p).getPlayerCooldowns().clearCooldowns();
        MagicData.getPlayerMagicData(p).getPlayerCooldowns().syncToPlayer(p);
        var book = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("irons_spellbooks:gold_spell_book")));
        var data=ISpellContainer.get(book).mutableCopy(); data.setMaxSpellCount(8);
        data.addSpellAtIndex(SpellRegistry.getSpell("irons_spellbooks:oakskin"),1,0,false);
        data.addSpellAtIndex(SpellRegistry.getSpell("irons_spellbooks:fortify"),1,2,false);
        data.addSpellAtIndex(SpellRegistry.getSpell("irons_spellbooks:haste"),1,4,false);
        ISpellContainer.set(book,data.toImmutable());
        CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("spellbook").orElseThrow().getStacks().setStackInSlot(0,book);
        pending=null;
        write("server-ready.txt","ironautospell absent; equipped slots 1=oakskin, 3=fortify, 5=haste; other slots empty");
    }
    private void cast(SpellOnCastEvent e) {
        casts.add(e.getSpellId());
        write("server-casts.txt",String.join("\n",casts));
    }
    private static void write(String file,String text) {
        try { Files.writeString(Path.of(file),text); } catch(Exception e) { throw new RuntimeException(e); }
    }
}
