package dev.ironautospell;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.util.stream.Collectors;
import static dev.ironautospell.IronAutoSpell.tr;

public final class ComboScreen extends Screen {
    private final Screen parent;
    private final ComboConfig config;
    private EditBox name, sequence;
    private int page, left, top, panelWidth, gridRows, footer;
    private Component error = Component.empty();
    public ComboScreen(Screen parent) {
        super(tr("title"));
        this.parent = parent;
        this.config = IronAutoSpell.INSTANCE.config;
    }
    private Button button(int x, int y, int w, Component label, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run()).bounds(x, y, w, 20).build());
    }
    @Override protected void init() {
        panelWidth = Math.min(width - 24, 640);
        left = (width - panelWidth) / 2;
        top = 10;
        footer = height - 30;
        gridRows = Math.max(1, Math.min((footer - 145) / 30, 4));
        int half = panelWidth / 2;
        button(left + 10, top + 27, 28, Component.literal("<"), () -> switchProfile(-1));
        button(left + 42, top + 27, 28, Component.literal(">"), () -> switchProfile(1));
        button(left + panelWidth - 144, top + 27, 64, tr("new"), () -> {
            if (!capture() || config.profiles.size() >= 32) return;
            config.profiles.add(new ComboConfig.Profile());
            config.selected = config.profiles.size() - 1; page = 0; rebuildWidgets();
        });
        button(left + panelWidth - 74, top + 27, 64, tr("delete"), () -> {
            if (config.profiles.size() <= 1) return;
            config.profiles.remove(config.selected); config.selected = 0; page = 0; rebuildWidgets();
        });
        name = new EditBox(font, left + 10, top + 66, half - 20, 20, tr("name"));
        name.setMaxLength(40); name.setValue(config.current().name); addRenderableWidget(name);
        sequence = new EditBox(font, left + half + 10, top + 66, half - 20, 20, tr("sequence"));
        sequence.setMaxLength(512);
        sequence.setValue(config.current().slots.stream().map(Object::toString).collect(Collectors.joining(", ")));
        addRenderableWidget(sequence);
        var container = BookAccess.container();
        int count = container == null ? 0 : container.getMaxSpellCount();
        int perPage = gridRows * 4;
        page = Math.max(0, Math.min(page, Math.max(0, (count - 1) / perPage)));
        int cellWidth = (half - 20) / 4;
        for (int offset = 0; offset < perPage && page * perPage + offset < count; offset++) {
            int slot = page * perPage + offset + 1;
            var d = BookAccess.spell(slot);
            var b = button(left + 10 + offset % 4 * cellWidth, top + 102 + offset / 4 * 30,
                    cellWidth - 3, Component.literal(Integer.toString(slot)), () -> append(slot));
            b.setTooltip(Tooltip.create(d.getSpell().getDisplayName(minecraft.player)));
            b.active = BookAccess.resolve(slot, d.getSpell().getSpellId()) >= 0;
        }
        int pageY = top + 105 + gridRows * 30;
        button(left + 10, pageY, 28, Component.literal("<"), () -> changePage(-1));
        button(left + half - 40, pageY, 28, Component.literal(">"), () -> changePage(1));
        var policyButton = button(left + half + 10, footer - 50, half - 20, tr("policy." + config.current().cooldown.name()), () -> {
            if (!capture()) return;
            var values = SequenceEngine.Policy.values();
            config.current().cooldown = values[(config.current().cooldown.ordinal() + 1) % values.length]; rebuildWidgets();
        });
        policyButton.setTooltip(Tooltip.create(tr("policy_hint")));
        int gap = 6, bw = (panelWidth - 20 - gap * 3) / 4;
        button(left + 10, footer, bw, tr("save"), () -> { if (save()) error = tr("saved"); });
        button(left + 10 + (bw + gap), footer, bw, tr("run"), () -> {
            if (!save()) return;
            minecraft.setScreen(null); IronAutoSpell.INSTANCE.start();
        });
        button(left + 10 + (bw + gap) * 2, footer, bw, tr("clear"), () -> sequence.setValue(""));
        button(left + 10 + (bw + gap) * 3, footer, bw, tr("close"), this::onClose);
    }
    private void append(int slot) {
        try {
            var slots = ComboConfig.parseSlots(sequence.getValue());
            if (slots.size() >= 64) throw new IllegalArgumentException();
            slots.add(slot); sequence.setValue(slots.stream().map(Object::toString).collect(Collectors.joining(", ")));
            error = Component.empty();
        } catch (IllegalArgumentException e) { error = tr("invalid_sequence"); }
    }
    private boolean capture() {
        try {
            var slots = ComboConfig.parseSlots(sequence.getValue());
            config.current().slots = slots;
            config.current().name = name.getValue().isBlank() ? "Combo" : name.getValue();
            error = Component.empty(); return true;
        } catch (IllegalArgumentException e) { error = tr("invalid_sequence"); return false; }
    }
    private boolean save() {
        if (!capture()) return false;
        try { config.save(); return true; }
        catch (IOException e) { IronAutoSpell.LOGGER.error("Cannot save combos", e); error = tr("save_error"); return false; }
    }
    private void switchProfile(int direction) {
        if (!capture()) return;
        config.selected = Math.floorMod(config.selected + direction, config.profiles.size());
        page = 0; rebuildWidgets();
    }
    private void changePage(int delta) {
        if (!capture()) return;
        page += delta; rebuildWidgets();
    }
    @Override public void onClose() { if (save()) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    // Screen.render calls renderBackground before widgets; our opaque panel already supplies the background.
    @Override public void renderBackground(GuiGraphics g) {}
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF00B1020);
        g.fill(left, top + 22, left + panelWidth, height - 7, 0xFF172137);
        g.drawCenteredString(font, title, width / 2, top, 0xFFD9E7FF);
        int half = panelWidth / 2;
        g.drawString(font, tr("profile", config.selected + 1, config.profiles.size()), left + 80, top + 33, 0xFFB5C9E7);
        g.drawString(font, tr("name"), left + 10, top + 55, 0xFF9EB3D5);
        g.drawString(font, tr("sequence"), left + half + 10, top + 55, 0xFF9EB3D5);
        g.drawString(font, tr("click_slots"), left + 10, top + 90, 0xFF9EB3D5);
        g.drawString(font, tr("preview"), left + half + 10, top + 90, 0xFF9EB3D5);
        var c = BookAccess.container();
        if (c == null) g.drawWordWrap(font, tr("no_book"), left + 12, top + 112, half - 24, 0xFFFFC58A);
        else {
            int perPage = gridRows * 4;
            g.drawCenteredString(font, Component.literal((page + 1) + " / " + Math.max(1, (c.getMaxSpellCount() + perPage - 1) / perPage)),
                    left + half / 2, top + 141 + (gridRows - 1) * 30, 0xFFB5C9E7);
        }
        try {
            var slots = ComboConfig.parseSlots(sequence.getValue());
            int lines = Math.max(1, (footer - 65 - (top + 104)) / 12);
            for (int i = 0; i < Math.min(slots.size(), lines); i++) {
                var d = BookAccess.spell(slots.get(i));
                String label = (i + 1) + ". [" + slots.get(i) + "] " + d.getSpell().getDisplayName(minecraft.player).getString() + " " + d.getLevel();
                g.drawString(font, font.plainSubstrByWidth(label, half - 24), left + half + 10, top + 104 + i * 12, 0xFFE0EAF8);
            }
        } catch (IllegalArgumentException ignored) {}
        g.drawString(font, font.plainSubstrByWidth(error.getString(), panelWidth - 20), left + 10, footer - 15, 0xFFFFC58A);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
