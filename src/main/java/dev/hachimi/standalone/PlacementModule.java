package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.math.*;
import java.util.LinkedHashSet;

abstract class PlacementModule extends HachimiModule {
    private final boolean trap;
    private final Setting<Integer> blocksPerTick = integer("blocks-per-tick", "每 tick 最多尝试放置的方块。", 2, 1, 8);
    private final Setting<Integer> delay = integer("delay", "每批放置后的等待 tick。", 0, 0, 20);
    private final Setting<Double> range = number("range", "放置距离。", 4.5, 1, 6);
    private final Setting<Boolean> rotate = bool("rotate", "放置时转头。", true);
    private final Setting<Boolean> floor = bool("floor", "脚下补方块。", true);
    private final Setting<Boolean> groundOnly = bool("ground-only", "只在地面工作。", true);
    private final Setting<Boolean> pauseUse = bool("pause-on-use", "使用物品时暂停。", true);
    private final Setting<Boolean> disableMove = bool("disable-on-move", "离开启用位置时停用。", true);
    private final Setting<Boolean> center = bool("center", "启用时移到方块中心。", false);
    private final Setting<Boolean> doubleHeight;
    private BlockPos anchor;
    private int timer;
    PlacementModule(String name, boolean trap) {
        super(name, trap ? "独立 SelfTrap：围住自己并封顶。" : "独立 Surround：在脚周围放置黑曜石。");
        this.trap = trap;
        doubleHeight = bool("double-height", "围脚增加第二层。", false);
    }
    @Override public void onActivate() {
        super.onActivate(); timer = 0; anchor = ready() ? mc.player.getBlockPos() : null;
        if (ready() && center.get() && mc.player.isOnGround())
            mc.player.setPosition(anchor.getX()+0.5, mc.player.getY(), anchor.getZ()+0.5);
    }
    @EventHandler private void tick(TickEvent.Pre event) {
        if (!ready()) return;
        BlockPos current = mc.player.getBlockPos();
        if (anchor == null) anchor = current;
        if (disableMove.get() && !current.equals(anchor)) { toggle(); return; }
        if (timer > 0) { timer--; return; }
        if (!inventoryReady() || (groundOnly.get() && !mc.player.isOnGround()) || (pauseUse.get() && mc.player.isUsingItem())) return;
        var block = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!block.found()) block = InvUtils.findInHotbar(Items.CRYING_OBSIDIAN);
        if (!block.found()) return;
        // Build around the complete player footprint, including straddling two blocks.
        Box box = mc.player.getBoundingBox();
        int minX = MathHelper.floor(box.minX+0.01), maxX = MathHelper.floor(box.maxX-0.01);
        int minZ = MathHelper.floor(box.minZ+0.01), maxZ = MathHelper.floor(box.maxZ-0.01);
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        for (int x=minX; x<=maxX; x++) for (int z=minZ; z<=maxZ; z++) {
            BlockPos feet = new BlockPos(x,current.getY(),z);
            if (floor.get()) positions.add(feet.down());
            for (Direction direction : Direction.Type.HORIZONTAL) positions.add(feet.offset(direction));
        }
        // Never attempt to fill a block occupied by the player.
        positions.removeIf(p -> p.getX()>=minX && p.getX()<=maxX && p.getZ()>=minZ && p.getZ()<=maxZ && p.getY()==current.getY());
        if (trap || doubleHeight.get()) {
            var feetRing = new java.util.ArrayList<>(positions);
            for (BlockPos p : feetRing) if (p.getY()==current.getY()) positions.add(p.up());
        }
        if (trap) {
            positions.add(current.north().up(2)); // Roof support, then the roof itself.
            positions.add(current.up(2));
        }
        int count = 0;
        for (BlockPos p : positions) {
            if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(p)) > range.get()*range.get() || !BlockUtils.canPlace(p,true)) continue;
            // Add support below exposed side blocks when no adjacent face exists.
            if (BlockUtils.getPlaceSide(p) == null && BlockUtils.canPlace(p.down(),true)) {
                if (BlockUtils.place(p.down(),block,rotate.get(),50,true)) count++;
                if (count >= blocksPerTick.get()) break;
            }
            if (BlockUtils.place(p,block,rotate.get(),50,true)) count++;
            if (count >= blocksPerTick.get()) break;
        }
        if (count > 0) timer = delay.get();
    }
}
