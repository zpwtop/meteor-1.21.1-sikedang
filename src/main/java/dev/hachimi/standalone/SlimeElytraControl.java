package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.client.network.ClientPlayerEntity;

public final class SlimeElytraControl extends HachimiModule {
    public enum Control { CONTROL, ROTATION }
    public enum Motion { VOID, FIRE_WORKS }
    private final Setting<Control> control=general.add(new EnumSetting.Builder<Control>().name("control-mode").description("CONTROL 按键方向；ROTATION 按视角方向。").defaultValue(Control.CONTROL).build());
    private final Setting<Motion> motion=general.add(new EnumSetting.Builder<Motion>().name("motion-mode").description("VOID 直接控制速度；FIRE_WORKS 转向并用烟花推进。").defaultValue(Motion.VOID).build());
    private final Setting<Double> speed=number("speed","VOID 模式每 tick 的速度；原版默认 0.05。",0.05,0.01,5);
    private final Setting<Boolean> autoFly=bool("auto-fly","无按键时沿视角方向前进。",false);
    private final Setting<Boolean> rotate=bool("rotate","控制飞行朝向；飞行后的镜头恢复原朝向。",true);
    private final Setting<Boolean> noGravity=bool("horizontal-no-gravity","水平飞行时临时取消重力。",true);
    private final Setting<Integer> rocketDelay=integer("rocket-delay","FIRE_WORKS 模式使用烟花的 tick 间隔。",20,5,100);
    private final Setting<Boolean> landDisable=bool("land-auto-close","滑翔后落地自动停用。",false);
    private ClientPlayerEntity changedPlayer;
    private float yaw,pitch;
    private boolean gravity,changed,airborne;
    private int rocketTimer;
    public SlimeElytraControl(){super(HachimiAddon.SLIME_CATEGORY,"slime简易鞘翅控制","参考 SlimefunHelper 简易控制；可与 slime甲飞 同时使用，也可穿鞘翅使用。");}
    @Override public void onActivate(){super.onActivate();airborne=false;rocketTimer=0;}
    @Override public void onDeactivate(){restore();super.onDeactivate();}
    private void restore(){
        if(changed&&mc.player==changedPlayer){mc.player.setYaw(yaw);mc.player.setPitch(pitch);mc.player.setNoGravity(gravity);}
        changed=false;changedPlayer=null;
    }
    @EventHandler(priority=100) private void pre(TickEvent.Pre event){
        restore();
        if(rocketTimer>0)rocketTimer--;
        if(!ready())return;
        if(!mc.player.isFallFlying()){
            if(airborne&&mc.player.isOnGround()&&landDisable.get())toggle();
            return;
        }
        airborne=true;
        if(mc.currentScreen!=null||mc.player.hasVehicle()||mc.player.isTouchingWater())return;
        double f=(mc.options.forwardKey.isPressed()?1:0)-(mc.options.backKey.isPressed()?1:0);
        double s=(mc.options.leftKey.isPressed()?1:0)-(mc.options.rightKey.isPressed()?1:0);
        double up=(mc.options.jumpKey.isPressed()?1:0)-(mc.options.sneakKey.isPressed()?1:0);
        double[] velocity=FlightMath.direction(mc.player.getYaw(),mc.player.getPitch(),f,s,up,control.get()==Control.ROTATION,speed.get());
        boolean moving=velocity[0]*velocity[0]+velocity[1]*velocity[1]+velocity[2]*velocity[2]>1e-10;
        if(!moving&&autoFly.get()){velocity=FlightMath.direction(mc.player.getYaw(),mc.player.getPitch(),1,0,0,true,speed.get());moving=true;}
        changedPlayer=mc.player;yaw=mc.player.getYaw();pitch=mc.player.getPitch();gravity=mc.player.hasNoGravity();changed=true;
        if(rotate.get()&&moving){
            double horizontal=Math.hypot(velocity[0],velocity[2]);
            if(horizontal>1e-8)mc.player.setYaw((float)Math.toDegrees(Math.atan2(-velocity[0],velocity[2])));
            mc.player.setPitch((float)-Math.toDegrees(Math.atan2(velocity[1],horizontal)));
        }
        if(motion.get()==Motion.VOID){
            if(noGravity.get()&&Math.abs(velocity[1])<1e-8)mc.player.setNoGravity(true);
            mc.player.setVelocity(velocity[0],velocity[1],velocity[2]);
        }else if(moving&&rocketTimer==0&&inventoryReady()){
            var rocket=InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if(rocket.found()){
                if(rocket.isOffhand())mc.interactionManager.interactItem(mc.player,Hand.OFF_HAND);
                else withSlot(rocket.slot(),()->mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND));
                rocketTimer=rocketDelay.get();
            }
        }
    }
    @EventHandler(priority=-100) private void post(TickEvent.Post event){restore();}
}
