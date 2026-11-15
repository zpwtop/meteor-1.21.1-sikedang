package dev.hachimi.standalone.mixin;

import dev.hachimi.standalone.DurableElytra;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Mirrors the original local-player glide-flag guard. Server entities are never changed. */
@Mixin(value=LivingEntity.class,remap=false)
public abstract class DurableElytraMixin {
    @ModifyArg(method="method_6053",at=@At(value="INVOKE",target="Lnet/minecraft/class_1309;method_5729(IZ)V",remap=false),index=1,remap=false)
    private boolean sikedang$preserveGlideFlag(boolean value){
        return value||DurableElytra.preserveGliding((LivingEntity)(Object)this);
    }
}
