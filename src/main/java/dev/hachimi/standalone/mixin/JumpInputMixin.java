package dev.hachimi.standalone.mixin;

import dev.hachimi.standalone.JumpingTiger;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=KeyboardInput.class,remap=false)
public abstract class JumpInputMixin {
    @Inject(method="method_3129",at=@At("TAIL"),remap=false)
    private void sikedang$jump(CallbackInfo ci){JumpingTiger.afterInput((Input)(Object)this);}
}
