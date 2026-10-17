package dev.ironautospell.smoke;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
@Mod("smoketest")
public class SmokeTest {
    public SmokeTest() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> new ClientFixture());
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> new ServerFixture());
    }
}
