package dev.ironautospell;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;

/** Keep all Minecraft client classes behind the physical client boundary. */
@Mod(IronAutoSpell.ID)
public final class SpellRelay {
    public SpellRelay() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.fml.IExtensionPoint.DisplayTest.class,
                () -> new net.minecraftforge.fml.IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY, (remote, server) -> true));
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> IronAutoSpell::init);
    }
}
