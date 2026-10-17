package dev.ironautospell;

import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import java.util.*;

public final class IronAutoSpell implements SequenceEngine.Driver {
    public static final String ID = "ironautospell";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static IronAutoSpell INSTANCE;
    public final ComboConfig config;
    public final SequenceEngine engine = new SequenceEngine(this);
    private final KeyMapping editor = new KeyMapping("key.ironautospell.editor", GLFW.GLFW_KEY_F8, "key.categories.ironautospell");
    private final KeyMapping toggle = new KeyMapping("key.ironautospell.toggle", GLFW.GLFW_KEY_F9, "key.categories.ironautospell");
    private Component status = Component.empty();
    private long statusUntil;
    private String bookSignature;
    public static void init() { new IronAutoSpell(); }
    private IronAutoSpell() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        INSTANCE = this;
        config = ComboConfig.load();
        bus.addListener(this::keys);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::hud);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ComboScreen(parent)));
    }
    public static Component tr(String key, Object... args) { return Component.translatable("ironautospell." + key, args); }
    private void keys(RegisterKeyMappingsEvent e) { e.register(editor); e.register(toggle); }
    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        while (editor.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                engine.stop("stopped");
                mc.setScreen(new ComboScreen(null));
            }
        }
        while (toggle.consumeClick()) if (mc.screen == null && mc.player != null) {
            if (engine.active()) engine.stop("stopped"); else start();
        }
        if (!engine.active()) return;
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) { engine.stop("left_world"); return; }
        if (mc.screen != null) { engine.stop("screen_open"); return; }
        if (!Objects.equals(bookSignature, BookAccess.signature())) { engine.stop("book_changed"); return; }
        // Manual inputs take over without sending a competing quick-cast packet.
        if (mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) { engine.stop("manual_cast"); return; }
        engine.tick(now());
    }
    public boolean start() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || engine.active()) return false;
        if (ClientMagicData.isCasting()) { status("busy"); return false; }
        var profile = config.current();
        if (profile.slots.isEmpty()) { status("empty"); return false; }
        List<SequenceEngine.Step> steps = new ArrayList<>();
        for (int slot : profile.slots) {
            var data = BookAccess.spell(slot);
            String id = data.getSpell().getSpellId();
            if (BookAccess.resolve(slot, id) < 0) { status("bad_slot", slot); return false; }
            if (data.getSpell().getCastType() == CastType.CONTINUOUS) { status("continuous", slot); return false; }
            steps.add(new SequenceEngine.Step(slot, id));
        }
        bookSignature = BookAccess.signature();
        engine.start(steps, profile.cooldown);
        return true;
    }
    @Override public SequenceEngine.Gate check(SequenceEngine.Step step) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || ClientMagicData.isCasting() || BookAccess.resolve(step.bookSlot(), step.spellId()) < 0)
            return SequenceEngine.Gate.INVALID;
        var spell = BookAccess.spell(step.bookSlot()).getSpell();
        if (ClientMagicData.getRecasts().hasRecastForSpell(step.spellId())) {
            // Recast can dismiss an active summon or toggle a buff off. Do not do it implicitly.
            return SequenceEngine.Gate.INVALID;
        }
        if (ClientMagicData.getCooldowns().isOnCooldown(spell)) return SequenceEngine.Gate.COOLDOWN;
        int level = spell.getLevelFor(BookAccess.spell(step.bookSlot()).getLevel(), mc.player);
        if (!mc.player.isCreative() && ClientMagicData.getPlayerMana() < spell.getManaCost(level)) return SequenceEngine.Gate.NO_MANA;
        return SequenceEngine.Gate.READY;
    }
    @Override public void send(SequenceEngine.Step step) {
        int index = BookAccess.resolve(step.bookSlot(), step.spellId());
        if (index < 0) { engine.stop("invalid"); return; }
        LOGGER.info("IronAutoSpell dispatch book slot {} / wheel {} / {}", step.bookSlot(), index, step.spellId());
        PacketDistributor.sendToServer(new QuickCastPacket(index));
    }
    @Override public void status(String key, Object... args) {
        status = tr("status." + key, args);
        statusUntil = now() + 10_000;
        if (!engine.active()) {
            LOGGER.info("IronAutoSpell: {}", status.getString());
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(status, false);
        }
    }
    public Component statusText() { return status; }
    private void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || (!engine.active() && now() > statusUntil)) return;
        var g = event.getGuiGraphics();
        int x = 8, y = 9;
        g.fill(x - 4, y - 4, x + Math.min(mc.font.width(status), g.guiWidth() - 24) + 6, y + 14, 0xB0101728);
        g.drawString(mc.font, status, x, y, 0xFFD9E8FF);
    }
    public static long now() { return System.nanoTime() / 1_000_000; }
    public static void onStarted(String id, int ticks) {
        if (INSTANCE != null) INSTANCE.engine.started(id, ticks, now());
    }
    public static void onFinished(UUID caster, String id, boolean cancelled) {
        var player = Minecraft.getInstance().player;
        if (INSTANCE != null && player != null && player.getUUID().equals(caster)) {
            LOGGER.debug("IronAutoSpell finish {} cancelled={}", id, cancelled);
            INSTANCE.engine.finished(id, cancelled);
        }
    }
}
