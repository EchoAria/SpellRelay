package dev.ironautospell.mixin;

import dev.ironautospell.IronAutoSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMagicData.class, remap = false)
public class CastStateMixin {
    @Inject(method = "setClientCastState", at = @At("TAIL"))
    private static void autoSpell$started(String spell, int level, int duration, CastSource source, String slot, CallbackInfo ci) {
        IronAutoSpell.onStarted(spell, duration);
    }
}
