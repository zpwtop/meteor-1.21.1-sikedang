package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public final class HachimiMace extends HachimiModule {
    private final Setting<Double> delay = number("delay", "两次切换之间的秒数。", 0, 0, 10);
    private final Setting<Boolean> ignoreCrystal = bool("ignore-crystals", "攻击末地水晶时不切换重锤。", true);
    private final Setting<Boolean> fallingOnly = bool("falling-only", "只在下落时切换。", false);
    private boolean sending;
    private long lastSwap;
    public HachimiMace() { super("hachimi重锤秒切", "攻击包发送前切到重锤，发送后立即恢复；对应原版切换模式。"); }
    private static boolean isAttack(PlayerInteractEntityC2SPacket packet) {
        boolean[] attack = {false};
        packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override public void attack() { attack[0] = true; }
            @Override public void interact(net.minecraft.util.Hand hand) {}
            @Override public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}
        });
        return attack[0];
    }
    @Override public void onActivate() { super.onActivate(); lastSwap = 0; sending = false; }
    @EventHandler(priority=100) private void send(PacketEvent.Send event) {
        if (event.isCancelled() || !inventoryReady() || sending || !(event.packet instanceof PlayerInteractEntityC2SPacket packet)) return;
        var target = GameCompat.attackTarget(packet);
        if (!isAttack(packet)) return;
        if (target == null || (ignoreCrystal.get() && target instanceof EndCrystalEntity)) return;
        if (fallingOnly.get() && (mc.player.isOnGround() || mc.player.getVelocity().y >= 0)) return;
        long now = System.nanoTime();
        if (lastSwap != 0 && now-lastSwap < delay.get()*1_000_000_000L) return;
        var mace = InvUtils.find(stack -> stack.isOf(Items.MACE), 0, 8);
        if (!mace.found() || mace.isOffhand() || mace.slot() == mc.player.getInventory().selectedSlot) return;
        event.cancel();
        sending = true;
        try {
            withSlot(mace.slot(), () -> mc.getNetworkHandler().sendPacket(packet));
            lastSwap = now;
        } finally { sending = false; }
    }
}
