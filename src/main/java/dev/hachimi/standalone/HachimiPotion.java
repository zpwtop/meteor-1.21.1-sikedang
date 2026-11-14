package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class HachimiPotion extends HachimiModule {
    private final Setting<Double> delay = number("delay", "扔药水间隔秒数。", 2, 0, 10);
    private final Setting<Boolean> inventory = bool("inventory-swap", "允许从背包交换药水，用完恢复。", true);
    private final Setting<Boolean> speed = bool("speed", "自动补速度。", false);
    private final Setting<Boolean> resistance = bool("resistance", "自动补抗性；优先抗性 II 及以上。", false);
    private final Setting<Boolean> strength = bool("strength", "自动补力量。", false);
    private final Setting<Boolean> multitask = bool("multitask", "使用其他物品时仍允许扔药。", true);
    private final Setting<Boolean> groundOnly = bool("ground-only", "只在地面扔药。", false);
    private final Setting<Integer> refresh = integer("refresh-ticks", "效果剩余少于这些 tick 时补药；0 为结束后补。", 0, 0, 200);
    private long lastThrow;
    private boolean pending;
    public HachimiPotion() { super("hachimi自动药水", "独立自动向脚下投掷速度、抗性、力量喷溅药水。"); }
    @Override public void onActivate() { super.onActivate(); pending = false; lastThrow = 0; }
    @Override public void onDeactivate() { super.onDeactivate(); pending = false; }
    @EventHandler private void tick(TickEvent.Pre event) {
        if (!inventoryReady() || pending || (!multitask.get() && mc.player.isUsingItem()) || (groundOnly.get() && !mc.player.isOnGround())) return;
        if (lastThrow != 0 && System.nanoTime()-lastThrow < delay.get()*1_000_000_000L) return;
        if (tryEffect(StatusEffects.RESISTANCE,resistance.get(),1)) return;
        if (tryEffect(StatusEffects.SPEED,speed.get(),0)) return;
        tryEffect(StatusEffects.STRENGTH,strength.get(),0);
    }
    private boolean tryEffect(RegistryEntry<StatusEffect> effect, boolean enabled, int minimumAmplifier) {
        StatusEffectInstance current = mc.player.getStatusEffect(effect);
        int amplifier = current == null ? -1 : current.getAmplifier();
        int duration = current == null ? 0 : current.isInfinite() ? Integer.MAX_VALUE : current.getDuration();
        if (!Rules.refreshEffect(enabled, amplifier, duration, refresh.get(), minimumAmplifier)) return false;
        int slot = -1;
        for (int i=0; i<(inventory.get()?36:9); i++) {
            if (matches(mc.player.getInventory().getStack(i),effect,minimumAmplifier)) { slot=i; break; }
        }
        if (slot == -1) return false;
        final int selected = slot, token = generation;
        final var world = mc.world;
        pending = true;
        Rotations.rotate(mc.player.getYaw(),90,90,() -> {
            try {
                if (!isActive() || token != generation || !inventoryReady() || mc.world != world
                    || !matches(mc.player.getInventory().getStack(selected),effect,minimumAmplifier)) return;
                withSlot(selected, () -> {
                    float oldPitch = mc.player.getPitch();
                    try {
                        mc.player.setPitch(90); // 1.21.1 item-use packets include their own yaw and pitch.
                        mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND);
                        mc.player.swingHand(Hand.MAIN_HAND);
                    } finally { mc.player.setPitch(oldPitch); }
                });
                lastThrow = System.nanoTime();
            } finally { pending = false; }
        });
        return true;
    }
    private boolean matches(ItemStack stack, RegistryEntry<StatusEffect> effect, int amplifier) {
        if (!stack.isOf(Items.SPLASH_POTION)) return false;
        for (StatusEffectInstance e : stack.getOrDefault(DataComponentTypes.POTION_CONTENTS,PotionContentsComponent.DEFAULT).getEffects()) {
            if (e.getEffectType().equals(effect) && e.getAmplifier() >= amplifier) return true;
        }
        return false;
    }
}
