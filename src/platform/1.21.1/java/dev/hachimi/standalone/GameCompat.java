package dev.hachimi.standalone;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;

/** The only Minecraft 1.21.1 / Meteor 0.5.8 API differences. */
public final class GameCompat {
    private GameCompat() {}
    public static boolean isGliding(PlayerEntity player) { return player.isFallFlying(); }
    public static void startGliding(PlayerEntity player) { player.startFallFlying(); }
    public static void stopGliding(PlayerEntity player) { player.stopFallFlying(); }
    public static boolean isChestArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor && armor.getSlotType()==EquipmentSlot.CHEST;
    }
    public static Entity attackTarget(PlayerInteractEntityC2SPacket packet) {
        return ((IPlayerInteractEntityC2SPacket)packet).getEntity();
    }
    public static <T> net.minecraft.registry.RegistryWrapper.Impl<T> worldgenRegistry(net.minecraft.registry.RegistryWrapper.WrapperLookup lookup, net.minecraft.registry.RegistryKey<? extends net.minecraft.registry.Registry<T>> key) {
        return lookup.getWrapperOrThrow(key);
    }
    public static boolean tryStartGliding(net.minecraft.client.network.ClientPlayerEntity player) { return player.checkFallFlying(); }
    public static Runnable jumpInput(net.minecraft.client.input.Input input,boolean jump,boolean sneak) {
        boolean f=input.pressingForward,b=input.pressingBack,l=input.pressingLeft,r=input.pressingRight,j=input.jumping,s=input.sneaking;
        float forward=input.movementForward,sideways=input.movementSideways;
        input.pressingForward=true;input.pressingBack=false;input.pressingLeft=false;input.pressingRight=false;
        input.jumping=jump;input.sneaking=sneak;input.movementForward=sneak?0.3f:1;input.movementSideways=0;
        return ()->{input.pressingForward=f;input.pressingBack=b;input.pressingLeft=l;input.pressingRight=r;input.jumping=j;input.sneaking=s;input.movementForward=forward;input.movementSideways=sideways;};
    }
}
