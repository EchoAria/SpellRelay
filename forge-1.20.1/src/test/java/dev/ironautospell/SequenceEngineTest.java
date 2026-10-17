package dev.ironautospell;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SequenceEngineTest {
    static final SequenceEngine.Step A = new SequenceEngine.Step(3, "test:a");
    static final SequenceEngine.Step B = new SequenceEngine.Step(7, "test:b");
    static class Harness implements SequenceEngine.Driver {
        final List<SequenceEngine.Step> sent = new ArrayList<>();
        SequenceEngine.Gate gate = SequenceEngine.Gate.READY;
        String status;
        SequenceEngine engine = new SequenceEngine(this);
        public SequenceEngine.Gate check(SequenceEngine.Step s) { return gate; }
        public void send(SequenceEngine.Step s) { sent.add(s); }
        public void status(String k, Object... args) { status = k; }
        void start(SequenceEngine.Policy p) { engine.start(List.of(A, B), p); engine.tick(100); }
    }
    @Test void neverSendsSecondSpellUntilServerFinishesFirst() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        for (int i = 0; i < 50; i++) h.engine.tick(200 + i);
        assertEquals(List.of(A), h.sent);
        h.engine.started(A.spellId(), 80, 300);
        for (int i = 0; i < 100; i++) h.engine.tick(400 + i);
        assertEquals(List.of(A), h.sent);
        h.engine.finished(A.spellId(), false);
        h.engine.tick(600); h.engine.tick(650); h.engine.tick(700);
        assertEquals(List.of(A, B), h.sent);
    }
    @Test void instantaneousStartAndFinishInOneTickAreNotLost() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        h.engine.started(A.spellId(), 0, 101); h.engine.finished(A.spellId(), false);
        for (int i = 0; i < 3; i++) h.engine.tick(102 + i);
        assertEquals(List.of(A, B), h.sent);
    }
    @Test void duplicateAndUnrelatedFinishesDoNotAdvance() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        h.engine.finished(A.spellId(), false);
        assertEquals(0, h.engine.cursor());
        h.engine.started(A.spellId(), 20, 101);
        h.engine.finished("other", false); assertEquals(0, h.engine.cursor());
        h.engine.finished(A.spellId(), false); h.engine.finished(A.spellId(), false);
        assertEquals(1, h.engine.cursor());
    }
    @Test void consecutiveIdenticalSpellsRequireSeparateStartAcknowledgements() {
        var h = new Harness(); h.engine.start(List.of(A, A), SequenceEngine.Policy.SKIP); h.engine.tick(100);
        h.engine.started(A.spellId(), 0, 101); h.engine.finished(A.spellId(), false);
        for (int i=0;i<3;i++) h.engine.tick(200+i);
        h.engine.finished(A.spellId(), false); assertTrue(h.engine.active());
        h.engine.started(A.spellId(), 0, 204); h.engine.finished(A.spellId(), false);
        assertFalse(h.engine.active()); assertEquals("done", h.status);
    }
    @Test void cancelledCastStopsWithoutSendingNext() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        h.engine.started(A.spellId(), 40, 101); h.engine.finished(A.spellId(), true);
        h.engine.tick(999); assertEquals(List.of(A), h.sent); assertEquals("interrupted", h.status);
    }
    @Test void rejectedCastTimesOutInsteadOfPretendingSuccessOrSpamming() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        h.engine.tick(30100); assertFalse(h.engine.active()); assertEquals("timeout_start", h.status);
        assertEquals(List.of(A), h.sent);
    }
    @Test void longCastHasDurationAwareTimeout() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        h.engine.started(A.spellId(), 800, 200);
        h.engine.tick(70000); assertTrue(h.engine.active());
        h.engine.tick(230200); assertFalse(h.engine.active()); assertEquals("timeout_cast", h.status);
    }
    @Test void cooldownSkipDoesNotSendCoolingSpell() {
        var h = new Harness(); h.gate = SequenceEngine.Gate.COOLDOWN; h.start(SequenceEngine.Policy.SKIP);
        h.gate = SequenceEngine.Gate.READY;
        for(int i=0;i<3;i++) h.engine.tick(200+i);
        assertEquals(List.of(B), h.sent);
    }
    @Test void cooldownWaitCanResumeOrBeStopped() {
        var h = new Harness(); h.gate = SequenceEngine.Gate.COOLDOWN; h.start(SequenceEngine.Policy.WAIT);
        h.engine.tick(500); assertTrue(h.sent.isEmpty());
        h.gate = SequenceEngine.Gate.READY; h.engine.tick(600); assertEquals(List.of(A), h.sent);
        h.engine.stop("stopped"); h.engine.started(A.spellId(), 1, 601); h.engine.finished(A.spellId(), false);
        assertEquals("stopped", h.status); assertFalse(h.engine.active());
    }
    @Test void cooldownStopDoesNotSend() {
        var h = new Harness(); h.gate = SequenceEngine.Gate.COOLDOWN; h.start(SequenceEngine.Policy.STOP);
        assertFalse(h.engine.active()); assertTrue(h.sent.isEmpty());
    }
    @Test void noManaAndInvalidSlotStopBeforeSending() {
        for(var gate : List.of(SequenceEngine.Gate.NO_MANA, SequenceEngine.Gate.INVALID)) {
            var h = new Harness(); h.gate = gate; h.start(SequenceEngine.Policy.SKIP);
            assertFalse(h.engine.active()); assertTrue(h.sent.isEmpty());
        }
    }
    @Test void manualSpellDuringSequenceStopsIt() {
        var h = new Harness(); h.start(SequenceEngine.Policy.SKIP);
        h.engine.started("other", 20, 101);
        assertFalse(h.engine.active()); assertEquals("manual_cast", h.status);
    }
}
