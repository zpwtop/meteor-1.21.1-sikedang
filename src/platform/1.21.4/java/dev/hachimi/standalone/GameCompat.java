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
}
