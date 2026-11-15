package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public final class HachimiFakeFly extends HachimiModule {
    private final Setting<Double> speed = number("speed", "水平速度，方块/tick。", 0.6, 0.05, 2);
    private final Setting<Double> vertical = number("vertical-speed", "跳跃/潜行时的垂直速度。", 0.4, 0.05, 1);
    private final Setting<Boolean> autoJump = bool("auto-jump", "在地面时自动起跳。", true);
    private final Setting<Boolean> autoBoost = bool("auto-boost", "自动使用快捷栏烟花。", true);
    private final Setting<Double> rocketDelay = number("rocket-delay", "使用烟花间隔秒数。", 1, 0.1, 5);
    private final Setting<Boolean> freeze = bool("freeze", "没有移动输入时悬停。", true);
    private long lastRocket;
    private boolean warned;
    private net.minecraft.client.network.ClientPlayerEntity gravityPlayer;
    private boolean previousNoGravity;
    public HachimiFakeFly() { super("假飞", "临时穿鞘翅请求滑翔后恢复胸甲，通过输入控制移动；服务器可能拒绝。"); }
    @Override public void onActivate() {
        super.onActivate(); lastRocket = 0; warned = false;
        gravityPlayer = mc.player;
        previousNoGravity = gravityPlayer != null && gravityPlayer.hasNoGravity();
    }
    private void restoreGravity() {
        if (mc.player != null && mc.player == gravityPlayer) mc.player.setNoGravity(previousNoGravity);
    }
    @Override public void onDeactivate() {
        super.onDeactivate();
        restoreGravity();
        if (ready()) { GameCompat.stopGliding(mc.player); mc.player.setVelocity(0,0,0); }
    }
    @EventHandler(priority=-50) private void tick(TickEvent.Pre event) {
        if (!inventoryReady() || mc.player.isTouchingWater() || mc.player.hasVehicle() || mc.player.isClimbing()) { restoreGravity(); return; }
        int elytra = findElytra();
        boolean equipped = usable(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        if (!equipped && elytra == -1) {
            if (!warned) { warning("需要一件有耐久的鞘翅，放在主背包或胸甲栏。"); warned = true; }
            restoreGravity();
            return;
        }
        warned = false;
        if (mc.player.isOnGround()) { restoreGravity(); if (autoJump.get()) mc.player.jump(); return; }
        mc.player.setNoGravity(true);
        // Player inventory only: chest screen slot 6, temporary hotbar slot 8.
        Transactions.withElytra(equipped ? -1 : elytra, this::chestSwap, this::swapMain, this::requestFlight);
        GameCompat.stopGliding(mc.player); // Keep local movement controls instead of the vanilla glide controller.
        double forward = (mc.options.forwardKey.isPressed()?1:0)-(mc.options.backKey.isPressed()?1:0);
        double strafe = (mc.options.leftKey.isPressed()?1:0)-(mc.options.rightKey.isPressed()?1:0);
        double[] horizontal = Rules.motion(mc.player.getYaw(),forward,strafe,speed.get());
        double up = (mc.options.jumpKey.isPressed()?1:0)-(mc.options.sneakKey.isPressed()?1:0);
        if (forward != 0 || strafe != 0 || up != 0 || freeze.get())
            mc.player.setVelocity(horizontal[0],up*vertical.get(),horizontal[1]);
    }
    private void requestFlight() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        GameCompat.startGliding(mc.player);
        if (autoBoost.get() && (lastRocket == 0 || System.nanoTime()-lastRocket >= rocketDelay.get()*1_000_000_000L)) {
            var rocket = meteordevelopment.meteorclient.utils.player.InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (rocket.found()) {
                if (rocket.isOffhand()) mc.interactionManager.interactItem(mc.player,Hand.OFF_HAND);
                else withSlot(rocket.slot(),() -> mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND));
                lastRocket = System.nanoTime();
            }
        }
    }
    private boolean usable(ItemStack stack) { return stack.isOf(Items.ELYTRA) && stack.getDamage() < stack.getMaxDamage()-1; }
    private int findElytra() {
        for (int i=0;i<36;i++) if (usable(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }
    private void chestSwap(int hotbar) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,6,hotbar,SlotActionType.SWAP,mc.player);
    }
    private void swapMain(int source, int hotbar) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,Rules.screenSlot(source),hotbar,SlotActionType.SWAP,mc.player);
    }
}
