package dev.ironautospell.smoke;

import dev.ironautospell.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import java.nio.file.*;
import java.util.*;

public class ClientFixture {
    private int stage, ticks, startupTicks;
    private long deadline=System.nanoTime()+300_000_000_000L;
    private final List<String> evidence=new ArrayList<>();
    public ClientFixture() { MinecraftForge.EVENT_BUS.addListener(this::tick); }
    private void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        try {
            if (stage==99) return;
            if (System.nanoTime()>deadline) throw new AssertionError("Integration test timed out at stage "+stage);
            if (mc.player == null || mc.level == null) {
                if (++startupTicks % 100 == 0) {
                    String screen = mc.screen == null ? "null" : mc.screen.getClass().getSimpleName();
                    Files.writeString(Path.of("startup-screen.txt"), screen);
                    Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), c -> {});
                    if (mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
                        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc,
                                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("127.0.0.1:25579"),
                                new net.minecraft.client.multiplayer.ServerData("Spell Relay test", "127.0.0.1:25579", false), false);
                    } else if (screen.contains("AccessibilityOnboardingScreen")) {
                        mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                    }
                }
                return;
            }
            if (BookAccess.container() == null) return;
            var mod=IronAutoSpell.INSTANCE;
            if(stage==0) {
                if (mc.screen != null) return; // Wait until the world loading screen releases input.
                if(BookAccess.resolve(3,"irons_spellbooks:fortify")<0) return;
                require(BookAccess.resolve(1,"irons_spellbooks:oakskin")==0,"slot1 -> wheel0");
                require(BookAccess.resolve(3,"irons_spellbooks:fortify")==1,"slot3 -> wheel1 despite empty slot2");
                require(BookAccess.resolve(5,"irons_spellbooks:haste")==2,"slot5 -> wheel2");
                mod.config.current().name="Buff sequence / BUFF 连击";
                mod.config.current().slots=new ArrayList<>(List.of(3,1,5)); mod.config.current().cooldown=SequenceEngine.Policy.SKIP;
                mod.config.save(); require(ComboConfig.load().current().slots.equals(List.of(3,1,5)),"config saved and reloaded");
                KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(297)); // F8, through the registered key binding
                stage=1;ticks=0;
            } else if(stage==1 && ++ticks>180) {
                require(mc.screen instanceof ComboScreen,"F8 opened the configuration UI");
                Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),c->{});
                mc.setScreen(null);stage=2;ticks=0;
            } else if(stage==2 && ++ticks>20) {
                KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(298)); // F9
                evidence.add("F9 triggered native combo execution"); stage=3;ticks=0;
            } else if(stage==3 && !mod.engine.active()) {
                require(mod.engine.completed()==3 && mod.engine.skipped()==0,"all 3 real casts acknowledged, none skipped");
                require(mc.player.getActiveEffects().size()>=2,"buff effects synchronized to client");
                evidence.add("Survival cast order: book 3 -> 1 -> 5; native server acknowledgements received");
                require(mod.start(),"cooldown rerun started");stage=4;
            } else if(stage==4 && !mod.engine.active()) {
                require(mod.engine.completed()==0 && mod.engine.skipped()==3,"all cooling spells skipped, none cast");
                mod.config.current().slots=new ArrayList<>(List.of(2));
                require(!mod.start(),"empty physical slot rejected without a cast request");
                mod.config.current().slots=new ArrayList<>(List.of(3,1,5)); mod.config.save();
                stage=5;ticks=0;
            } else if(stage==5 && ++ticks>40) {
                evidence.add("PASS");Files.writeString(Path.of("integration-result.txt"),String.join("\n",evidence));
                stage=99;mc.stop();
            }
            if(stage==1 && ticks==170) mc.getToasts().clear();
        } catch(Throwable failure) {
            try { Files.writeString(Path.of("integration-result.txt"),"FAIL stage="+stage+"\n"+failure); } catch(Exception ignored) {}
            failure.printStackTrace();stage=99;mc.stop();
        }
    }
    private void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); evidence.add(message); }
}
