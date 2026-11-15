package dev.hachimi.standalone.mixin;

import dev.hachimi.standalone.DurableElytra;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value=ClientPlayNetworkHandler.class,remap=false)
public abstract class DurableElytraNetworkMixin {
    @ModifyVariable(method="method_11093",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private EntityTrackerUpdateS2CPacket sikedang$preserveLocalGliding(EntityTrackerUpdateS2CPacket packet){
        return DurableElytra.protectUpdate(packet);
    }
}
