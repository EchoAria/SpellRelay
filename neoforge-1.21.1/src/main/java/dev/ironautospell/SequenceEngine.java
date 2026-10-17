package dev.ironautospell;

import java.util.List;

/** One outstanding native cast at a time. No Minecraft dependencies: the real state machine is unit tested. */
public final class SequenceEngine {
    public enum Phase { IDLE, READY, STARTING, CASTING, GAP }
    public enum Policy { SKIP, WAIT, STOP }
    public enum Gate { READY, COOLDOWN, NO_MANA, INVALID }
    public record Step(int bookSlot, String spellId) {}
    public interface Driver {
        Gate check(Step step);
        void send(Step step);
        void status(String key, Object... args);
    }
    private final Driver driver;
    private List<Step> steps = List.of();
    private Phase phase = Phase.IDLE;
    private Policy policy = Policy.SKIP;
    private int cursor, gapTicks;
    private long deadline;
    private int completed, skipped;
    public SequenceEngine(Driver driver) { this.driver = driver; }
    public boolean active() { return phase != Phase.IDLE; }
    public Phase phase() { return phase; }
    public int cursor() { return cursor; }
    public int total() { return steps.size(); }
    public int completed() { return completed; }
    public int skipped() { return skipped; }
    public Step current() { return cursor < steps.size() ? steps.get(cursor) : null; }
    public void start(List<Step> steps, Policy policy) {
        if (active()) throw new IllegalStateException("Already running");
        if (steps.isEmpty()) throw new IllegalArgumentException("Empty sequence");
        this.steps = List.copyOf(steps);
        this.policy = policy;
        cursor = completed = skipped = 0;
        phase = Phase.READY;
    }
    public void stop(String reason) {
        if (!active()) return;
        phase = Phase.IDLE;
        driver.status(reason);
    }
    public void tick(long now) {
        if (!active()) return;
        if (phase == Phase.STARTING || phase == Phase.CASTING) {
            if (now >= deadline) stop(phase == Phase.STARTING ? "timeout_start" : "timeout_cast");
            return;
        }
        if (phase == Phase.GAP) {
            if (--gapTicks <= 0) phase = Phase.READY;
            return;
        }
        Gate gate = driver.check(current());
        if (gate == Gate.INVALID) { stop("invalid"); return; }
        if (gate == Gate.NO_MANA) { stop("no_mana"); return; }
        if (gate == Gate.COOLDOWN) {
            switch (policy) {
                case SKIP -> { skipped++; next(); }
                case STOP -> stop("cooldown_stop");
                case WAIT -> driver.status("cooldown_wait", cursor + 1, steps.size());
            }
            return;
        }
        // Set state before sending, including when a test/local callback is synchronous.
        phase = Phase.STARTING;
        deadline = now + 30_000;
        driver.status("starting", cursor + 1, steps.size(), current().bookSlot());
        driver.send(current());
    }
    public void started(String spellId, int castTicks, long now) {
        if (!active()) return;
        if (phase != Phase.STARTING || !current().spellId().equals(spellId)) {
            stop("manual_cast");
            return;
        }
        phase = Phase.CASTING;
        deadline = now + Math.max(60_000L, Math.max(0L, castTicks) * 250L + 30_000L);
        driver.status("casting", cursor + 1, steps.size(), current().bookSlot());
    }
    public void finished(String spellId, boolean cancelled) {
        // Never advance on a finish packet belonging to an earlier cast or another player.
        if (phase != Phase.CASTING || !current().spellId().equals(spellId)) return;
        if (cancelled) { stop("interrupted"); return; }
        completed++;
        next();
    }
    private void next() {
        cursor++;
        if (cursor >= steps.size()) {
            phase = Phase.IDLE;
            driver.status("done", completed, skipped);
        } else {
            phase = Phase.GAP;
            gapTicks = 2;
        }
    }
}
