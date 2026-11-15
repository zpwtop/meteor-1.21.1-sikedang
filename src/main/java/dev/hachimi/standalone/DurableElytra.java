package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

/** Re-equips the existing elytra and requests a new glide; never changes item damage. */
public final class DurableElytra extends HachimiModule {
    private static DurableElytra instance;
    private final Setting<Double> delay=number("delay","重装并重新请求滑翔的间隔，单位 tick（50 毫秒）；原版默认 3。",3,0,20);
    private final Setting<Integer> swapSlot=integer("swap-slot","用于临时交换胸甲栏的快捷栏位置（1—9），默认第 8 格。",8,1,9);
    private final ElytraResetClock clock=new ElytraResetClock();
    private boolean suppressedClear;
    public DurableElytra(){super("无限耐久鞘翅","周期性重新装备鞘翅并重发滑翔请求，尝试避免飞行耐久消耗；实际效果依服务器。");instance=this;}
    @Override public void onActivate(){super.onActivate();clock.reset();suppressedClear=false;}
    @Override public void onDeactivate(){
        if(suppressedClear&&ready())GameCompat.stopGliding(mc.player);
        suppressedClear=false;clock.reset();super.onDeactivate();
    }
    private boolean compatibleFlight(){return !Modules.get().isActive(SlimeArmorFly.class)&&!Modules.get().isActive(HachimiFakeFly.class);}
    private boolean eligible(){
        if(!inventoryReady()||!compatibleFlight()||mc.player.isOnGround()||mc.player.hasVehicle()||mc.player.isClimbing()||mc.player.isTouchingWater())return false;
        var stack=mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return stack.isOf(Items.ELYTRA)&&stack.getDamage()<stack.getMaxDamage()-1;
    }
    public static boolean preserveGliding(LivingEntity entity){
        DurableElytra module=instance;
        return module!=null&&module.isActive()&&entity==module.mc.player&&module.eligible()&&GameCompat.isGliding(module.mc.player);
    }
    /** Retain only this player's local gliding bit, without dropping other metadata. */
    public static net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket protectUpdate(net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket packet){
        var module=instance;
        if(module==null||!module.isActive()||!module.mc.isOnThread()||module.mc.player==null||packet.id()!=module.mc.player.getId())return packet;
        boolean preserve=preserveGliding(module.mc.player);
        java.util.List<net.minecraft.entity.data.DataTracker.SerializedEntry<?>> entries=null;
        for(int i=0;i<packet.trackedValues().size();i++){
            var entry=packet.trackedValues().get(i);
            if(entry.id()==0&&entry.value() instanceof Byte flags){
                module.suppressedClear=(flags&0x80)==0&&preserve;
                if(!module.suppressedClear)continue;
                if(entries==null)entries=new java.util.ArrayList<>(packet.trackedValues());
                entries.set(i,new net.minecraft.entity.data.DataTracker.SerializedEntry<>(entry.id(),net.minecraft.entity.data.TrackedDataHandlerRegistry.BYTE,ElytraFlags.retainGlide(flags)));
            }
        }
        return entries==null?packet:new net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket(packet.id(),entries);
    }
    @EventHandler(priority=200) private void pre(TickEvent.Pre event){
        if(!eligible()||!GameCompat.isGliding(mc.player))return;
        long now=System.nanoTime();if(!clock.due(now,delay.get()))return;
        var player=mc.player;var world=mc.world;
        Transactions.withChestSwap(swapSlot.get()-1,
            slot->{if(mc.player==player&&mc.world==world)mc.interactionManager.clickSlot(player.playerScreenHandler.syncId,6,slot,SlotActionType.SWAP,player);},()->{});
        if(mc.player!=player||mc.world!=world)return;
        GameCompat.startGliding(player);
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(player,ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        clock.sent(now);
    }
}
