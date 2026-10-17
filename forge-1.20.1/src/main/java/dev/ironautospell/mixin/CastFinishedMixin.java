package dev.ironautospell.mixin;

import dev.ironautospell.IronAutoSpell;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientSpellCastHelper.class, remap = false)
public class CastFinishedMixin {
    @Inject(method = "handleClientBoundOnCastFinished", at = @At("TAIL"))
    private static void autoSpell$finished(UUID caster, String spell, boolean cancelled, CallbackInfo ci) {
        IronAutoSpell.onFinished(caster, spell, cancelled);
    }
}
