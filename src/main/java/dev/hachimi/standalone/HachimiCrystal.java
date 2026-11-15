package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import java.util.ArrayList;
import java.util.List;

public final class HachimiCrystal extends HachimiModule {
    private final Setting<Double> targetRange = number("target-range", "目标距离。", 10, 1, 16);
    private final Setting<Double> placeRange = number("place-range", "放置距离。", 4.5, 1, 6);
    private final Setting<Double> breakRange = number("break-range", "攻击距离。", 4.5, 1, 6);
    private final Setting<Double> minDamage = number("min-damage", "对目标的最低预计伤害。", 6, 0, 36);
    private final Setting<Double> maxSelf = number("max-self-damage", "允许的最高自身伤害；同时检查致命伤害。", 6, 0, 36);
    private final Setting<Integer> placeDelay = integer("place-delay", "放置间隔，单位 tick。", 2, 0, 20);
    private final Setting<Integer> breakDelay = integer("break-delay", "攻击间隔，单位 tick。", 1, 0, 20);
    private final Setting<Boolean> place = bool("place", "自动放置末地水晶。", true);
    private final Setting<Boolean> breakCrystals = bool("break", "自动攻击末地水晶。", true);
    private final Setting<Boolean> rotate = bool("rotate", "发送朝向目标的转头。", true);
    private final Setting<Boolean> predict = bool("predict", "使用 Meteor 伤害计算的移动预测。", true);
    private final Setting<Boolean> pauseUse = bool("pause-on-use", "吃东西或使用物品时暂停。", true);
    private final Setting<Boolean> protectFriends = bool("protect-friends", "同样限制对 Meteor 好友造成的爆炸伤害。", true);
    private int placeTimer, breakTimer;
    private boolean pending;
    public HachimiCrystal() { super("自动水晶", "独立自动水晶：选择伤害位置、放置、攻击和自身伤害限制。"); }
    @Override public void onActivate() { super.onActivate(); placeTimer = breakTimer = 0; pending = false; }
    @Override public void onDeactivate() { super.onDeactivate(); pending = false; }
    @EventHandler private void tick(TickEvent.Pre event) {
        if (!ready()) return;
        if (placeTimer > 0) placeTimer--;
        if (breakTimer > 0) breakTimer--;
        if (pending || mc.currentScreen != null || (pauseUse.get() && mc.player.isUsingItem())) return;
        List<PlayerEntity> targets = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.isAlive() && !player.isSpectator() && !player.isCreative()
                && !Friends.get().isFriend(player) && player.squaredDistanceTo(mc.player) <= targetRange.get()*targetRange.get()) targets.add(player);
        }
        if (targets.isEmpty()) return;
        if (breakCrystals.get() && breakTimer == 0) {
            EndCrystalEntity best = null; double bestDamage = -1;
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive()
                    || mc.player.getEyePos().squaredDistanceTo(crystal.getPos()) > breakRange.get()*breakRange.get()) continue;
                double score = score(crystal.getPos(), targets);
                if (score > bestDamage) { bestDamage = score; best = crystal; }
            }
            if (best != null) {
                final EndCrystalEntity crystal = best;
                aim(crystal.getPos(), () -> {
                    if (!crystal.isAlive() || score(crystal.getPos(), targets) < 0) return;
                    mc.interactionManager.attackEntity(mc.player, crystal);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    breakTimer = Math.max(1, breakDelay.get());
                });
                return;
            }
        }
        if (!place.get() || placeTimer != 0 || !InvUtils.findInHotbar(Items.END_CRYSTAL).found()) return;
        BlockPos best = null; double bestDamage = -1;
        int radius = (int)Math.ceil(placeRange.get());
        BlockPos center = mc.player.getBlockPos();
        for (BlockPos mutable : BlockPos.iterate(center.add(-radius,-radius,-radius),center.add(radius,radius,radius))) {
            BlockPos base = mutable.toImmutable();
            if (!canPlace(base)) continue;
            Vec3d explosion = Vec3d.ofCenter(base).add(0,0.5,0);
            if (mc.player.getEyePos().squaredDistanceTo(explosion) > placeRange.get()*placeRange.get()) continue;
            double score = score(explosion, targets);
            if (score > bestDamage) { bestDamage = score; best = base; }
        }
        if (best != null) {
            final BlockPos base = best;
            Vec3d hit = Vec3d.ofCenter(base).add(0,0.5,0);
            aim(hit, () -> {
                if (!canPlace(base) || score(hit, targets) < 0) return;
                var crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
                if (!crystal.found()) return;
                if (crystal.isOffhand()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(hit,Direction.UP,base,false));
                    mc.player.swingHand(Hand.OFF_HAND);
                } else withSlot(crystal.slot(), () -> {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit,Direction.UP,base,false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                });
                placeTimer = Math.max(1,placeDelay.get());
            });
        }
    }
    private boolean canPlace(BlockPos base) {
        var state = mc.world.getBlockState(base);
        return (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) && mc.world.getBlockState(base.up()).isAir()
            && mc.world.getOtherEntities(null, new Box(base.up()).stretch(0,1,0)).isEmpty();
    }
    private double score(Vec3d position, List<PlayerEntity> targets) {
        double self = Math.max(DamageUtils.crystalDamage(mc.player, position, predict.get(), null),
            DamageUtils.crystalDamage(mc.player, position));
        double health = mc.player.getHealth()+mc.player.getAbsorptionAmount();
        if (!Rules.safeDamage(minDamage.get(), self, minDamage.get(), maxSelf.get(), health)) return -1;
        if (protectFriends.get()) for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.isAlive() && Friends.get().isFriend(p)) {
                double damage = DamageUtils.crystalDamage(p,position,predict.get(),null);
                if (damage > maxSelf.get() || damage + 0.5 >= p.getHealth()+p.getAbsorptionAmount()) return -1;
            }
        }
        double best = -1;
        for (PlayerEntity p : targets) {
            if (!p.isAlive() || p.squaredDistanceTo(mc.player) > targetRange.get()*targetRange.get()) continue;
            double damage = DamageUtils.crystalDamage(p,position,predict.get(),null);
            if (Rules.safeDamage(damage,self,minDamage.get(),maxSelf.get(),health)) best = Math.max(best, damage);
        }
        return best;
    }
    private void aim(Vec3d pos, Runnable action) {
        int token = generation;
        var world = mc.world;
        Runnable guarded = () -> {
            try { if (isActive() && token == generation && ready() && mc.world == world) action.run(); }
            finally { pending = false; }
        };
        if (rotate.get()) { pending = true; Rotations.rotate(Rotations.getYaw(pos),Rotations.getPitch(pos),70,guarded); }
        else guarded.run();
    }
}
