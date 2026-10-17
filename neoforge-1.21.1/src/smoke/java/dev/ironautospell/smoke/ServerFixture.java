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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import java.nio.file.*;
import java.util.*;

@Mod(value="smoketest", dist=Dist.DEDICATED_SERVER)
public class ServerFixture {
    private UUID pending;
    private int ticks;
    private final List<String> casts = new ArrayList<>();
    public ServerFixture() {
        if (ModList.get().isLoaded("ironautospell")) throw new IllegalStateException("Production mod must NOT be installed on this test server");
        NeoForge.EVENT_BUS.addListener(this::login);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::cast);
    }
    private void login(PlayerEvent.PlayerLoggedInEvent e) { pending=e.getEntity().getUUID(); ticks=0; }
    private void tick(ServerTickEvent.Post e) {
        if (pending==null || ++ticks<60) return;
        ServerPlayer p = e.getServer().getPlayerList().getPlayer(pending);
        if (p==null) return;
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(10000);
        MagicData.getPlayerMagicData(p).setMana(10000);
        MagicData.getPlayerMagicData(p).getPlayerCooldowns().clearCooldowns();
        MagicData.getPlayerMagicData(p).getPlayerCooldowns().syncToPlayer(p);
        var book = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_spellbooks:gold_spell_book")));
        var data=ISpellContainer.get(book).mutableCopy(); data.setMaxSpellCount(8);
        data.addSpellAtIndex(SpellRegistry.getSpell("irons_spellbooks:oakskin"),1,0,false);
        data.addSpellAtIndex(SpellRegistry.getSpell("irons_spellbooks:fortify"),1,2,false);
        data.addSpellAtIndex(SpellRegistry.getSpell("irons_spellbooks:haste"),1,4,false);
        ISpellContainer.set(book,data.toImmutable());
        CuriosApi.getCuriosInventory(p).orElseThrow().getStacksHandler("spellbook").orElseThrow().getStacks().setStackInSlot(0,book);
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
