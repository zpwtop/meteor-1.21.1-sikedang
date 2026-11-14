package dev.hachimi.standalone.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.PendingUpdateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Explicit 1.21.1 intermediary target; the class reference is remapped by the build. */
@Mixin(value=ClientWorld.class,remap=false)
public interface ClientWorldInvoker {
    @Invoker(value="method_41925",remap=false)
    PendingUpdateManager hachimi$getPendingUpdateManager();
}
