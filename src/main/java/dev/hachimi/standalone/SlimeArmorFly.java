package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public final class SlimeArmorFly extends HachimiModule {
    private final Setting<Boolean> autoJump=bool("auto-jump","在地面自动起跳。",true);
    private final Setting<Boolean> landDisable=bool("land-auto-close","开始飞行后落地自动停用。",false);
    private final Setting<Integer> requestDelay=integer("request-delay","发送滑翔请求的 tick 间隔。",1,1,20);
    private ChestSession session;
    private boolean airborne,warned;
    private int timer;
    public SlimeArmorFly(){super(HachimiAddon.SLIME_CATEGORY,"甲飞","参考 SlimefunHelper TICK 甲飞：玩家 tick 中临时穿鞘翅，之后恢复胸甲。");}
    @Override public void onActivate(){super.onActivate();airborne=false;warned=false;timer=0;}
    @Override public void onDeactivate(){restore();super.onDeactivate();if(ready())GameCompat.stopGliding(mc.player);}
    private void restore(){if(session!=null){ChestSession old=session;session=null;old.close();}}
    @EventHandler(priority=250) private void pre(TickEvent.Pre event){
        restore();
        if(!inventoryReady()||mc.player.hasVehicle()||mc.player.isTouchingWater()||mc.player.isClimbing())return;
        if(mc.player.isOnGround()){
            if(airborne&&landDisable.get()){toggle();return;}
            if(autoJump.get())mc.player.jump();
            return;
        }
        ItemStack chest=mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if(!GameCompat.isChestArmor(chest)){
            if(!warned){warning("甲飞需要先穿好胸甲，并把一件有耐久的鞘翅放在主背包或快捷栏。");warned=true;}
            return;
        }
        int slot=-1;
        for(int i=0;i<36;i++)if(usable(mc.player.getInventory().getStack(i))){slot=i;break;}
        if(slot==-1){if(!warned){warning("没有找到可用鞘翅。");warned=true;}return;}
        warned=false;
        final var player=mc.player;final var world=mc.world;
        session=ChestSession.open(slot,
            hotbar->{if(mc.player==player&&mc.world==world)mc.interactionManager.clickSlot(player.playerScreenHandler.syncId,6,hotbar,SlotActionType.SWAP,player);},
            (source,hotbar)->{if(mc.player==player&&mc.world==world)mc.interactionManager.clickSlot(player.playerScreenHandler.syncId,Rules.screenSlot(source),hotbar,SlotActionType.SWAP,player);});
        try{
            if(timer--<=0||!GameCompat.isGliding(mc.player)){
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                timer=requestDelay.get()-1;
            }
            GameCompat.startGliding(mc.player);airborne=true;
        }catch(RuntimeException e){restore();throw e;}
    }
    @EventHandler(priority=-250) private void post(TickEvent.Post event){restore();}
    private boolean usable(ItemStack stack){return stack.isOf(Items.ELYTRA)&&stack.getDamage()<stack.getMaxDamage()-1;}
}
