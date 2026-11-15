package dev.hachimi.standalone;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;

/** The only Minecraft 1.21.4 / Meteor 1.21.4-42 API differences. */
public final class GameCompat {
    private GameCompat() {}
    public static boolean isGliding(PlayerEntity player) { return player.isGliding(); }
    public static void startGliding(PlayerEntity player) { player.startGliding(); }
    public static void stopGliding(PlayerEntity player) { player.stopGliding(); }
    public static boolean isChestArmor(ItemStack stack) {
        var equipment=stack.get(DataComponentTypes.EQUIPPABLE);
        return stack.getItem() instanceof ArmorItem && equipment!=null && equipment.slot()==EquipmentSlot.CHEST;
    }
    public static Entity attackTarget(PlayerInteractEntityC2SPacket packet) {
        return ((IPlayerInteractEntityC2SPacket)packet).meteor$getEntity();
    }
    public static <T> net.minecraft.registry.RegistryWrapper.Impl<T> worldgenRegistry(net.minecraft.registry.RegistryWrapper.WrapperLookup lookup, net.minecraft.registry.RegistryKey<? extends net.minecraft.registry.Registry<T>> key) {
        return lookup.getOrThrow(key);
    }
    public static boolean tryStartGliding(net.minecraft.client.network.ClientPlayerEntity player) { return player.checkGliding(); }
    public static Runnable jumpInput(net.minecraft.client.input.Input input,boolean jump,boolean sneak) {
        var old=input.playerInput;float forward=input.movementForward,sideways=input.movementSideways;
        input.playerInput=new net.minecraft.util.PlayerInput(true,false,false,false,jump,sneak,true);
        input.movementForward=1;input.movementSideways=0;
        return ()->{input.playerInput=old;input.movementForward=forward;input.movementSideways=sideways;};
    }
}
