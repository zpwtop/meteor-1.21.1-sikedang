package dev.hachimi.standalone.mixin;

import dev.hachimi.standalone.ModuleNames;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve old profiles and keybindings when removing the displayed name prefixes. */
@Mixin(value=Modules.class,remap=false)
public abstract class ModuleNameMigrationMixin {
    @Inject(method="get(Ljava/lang/String;)Lmeteordevelopment/meteorclient/systems/modules/Module;",at=@At("HEAD"),cancellable=true,remap=false)
    private void sikedang$oldName(String name,CallbackInfoReturnable<Module> callback){
        String canonical=ModuleNames.canonical(name);
        if(name!=null&&!name.equals(canonical)){
            Module module=((Modules)(Object)this).get(canonical);
            if(module!=null)callback.setReturnValue(module);
        }
    }
}
