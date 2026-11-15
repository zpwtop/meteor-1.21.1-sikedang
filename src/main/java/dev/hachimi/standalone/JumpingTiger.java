package dev.hachimi.standalone;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Box;

/** Ground-hugging jump/glide cycle using vanilla input and flight requests. */
public final class JumpingTiger extends HachimiModule {
    private final Setting<Double> pitch=number("pitch","滑翔期间临时使用的俯角。",80,-90,90);
    private final Setting<Double> groundHeight=number("ground-height","只在脚下此距离内有碰撞地面时工作，单位方块。",3,0,10);
    private final Setting<Boolean> sneak=bool("sneak","跳跃和滑翔时潜行。",false);
    private final Setting<Boolean> axis=bool("axis-strict","接近八方向的 10 度以内时吸附方向。",false);
    private final Setting<Boolean> avoid=bool("auto-avoid-obstacles","预测前方三格高的通路，转向避开障碍。",false);
    private final Setting<Boolean> holes=bool("avoid-holes","预测前方同一高度的地面，转向避开缺口。",false);
    private final Setting<Integer> yawRange=integer("avoid-obstacles-yaw-range","最多向两侧搜索多少度。",50,0,180);
    private final Setting<Integer> predictTicks=integer("avoid-obstacles-predict-ticks","按当前水平速度预测的 tick 数；最长检查 32 格。",80,0,400);
    private ClientPlayerEntity changedPlayer;
    private Runnable inputRestore;
    private float oldYaw,oldPitch;
    private boolean rotated,lastGlide;
    private int requestDelay;
    public JumpingTiger(){super("跳跳虎","参考 SlimefunHelper 鞘翅跳跃：贴地自动前进、起跳、滑翔并重复；需穿鞘翅。");}
    @Override public void onActivate(){super.onActivate();lastGlide=false;requestDelay=0;}
    @Override public void onDeactivate(){restore();lastGlide=false;super.onDeactivate();}
    @EventHandler private void left(GameLeftEvent event){restore();lastGlide=false;}
    @EventHandler(priority=-300) private void post(TickEvent.Post event){restore();}
    private void restore(){
        if(inputRestore!=null){inputRestore.run();inputRestore=null;}
        if(rotated&&mc.player==changedPlayer){changedPlayer.setYaw(oldYaw);changedPlayer.setPitch(oldPitch);}
        rotated=false;changedPlayer=null;
    }
    /** Called after vanilla reads physical keys, before player movement and movement packets. */
    public static void afterInput(Input input){
        var modules=Modules.get();
        if(modules==null)return;
        var module=modules.get(JumpingTiger.class);
        if(module!=null&&module.isActive()&&MeteorClient.mc.player!=null&&MeteorClient.mc.player.input==input)module.move(input);
    }
    private void move(Input input){
        restore();
        if(requestDelay>0)requestDelay--;
        if(!inventoryReady()||mc.player.hasVehicle()||mc.player.isTouchingWater()||mc.player.isInLava()||mc.player.isClimbing()
            ||mc.player.getAbilities().flying||Modules.get().isActive(HachimiFakeFly.class)||Modules.get().isActive(SlimeElytraControl.class)){
            lastGlide=false;return;
        }
        var stack=mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if(!stack.isOf(Items.ELYTRA)||stack.getDamage()>=stack.getMaxDamage()-1){lastGlide=false;return;}
        var bounds=mc.player.getBoundingBox();double feet=mc.player.getY();
        var below=new Box(bounds.minX,feet-groundHeight.get()-0.002,bounds.minZ,bounds.maxX,feet+0.002,bounds.maxZ);
        double top=Double.NEGATIVE_INFINITY;
        for(var shape:mc.world.getBlockCollisions(mc.player,below))for(var box:shape.getBoundingBoxes())
            if(box.maxY<=feet+0.01&&box.maxY>top)top=box.maxY;
        boolean gliding=GameCompat.isGliding(mc.player);
        if(!Double.isFinite(top)){lastGlide=gliding;return;}
        changedPlayer=mc.player;oldYaw=mc.player.getYaw();oldPitch=mc.player.getPitch();rotated=true;
        float yaw=axis.get()?JumpDirection.snap(oldYaw):oldYaw;
        final double floor=top;
        if(avoid.get()||holes.get())yaw=JumpDirection.choose(yaw,yawRange.get(),candidate->clearDistance(candidate,floor));
        mc.player.setYaw(yaw);
        if(gliding||lastGlide)mc.player.setPitch(pitch.get().floatValue());
        boolean ground=mc.player.isOnGround();
        inputRestore=GameCompat.jumpInput(input,ground,sneak.get());
        mc.player.setSprinting(true);
        if(!ground&&!gliding&&requestDelay==0&&GameCompat.tryStartGliding(mc.player)){
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            requestDelay=3;
        }
        lastGlide=GameCompat.isGliding(mc.player);
    }
    private double clearDistance(float yaw,double floor){
        double speed=mc.player.getVelocity().horizontalLength();
        double distance=Math.min(32,speed*predictTicks.get());
        if(distance<0.001)return 0;
        double rad=Math.toRadians(yaw),dx=-Math.sin(rad),dz=Math.cos(rad);
        double x=mc.player.getX(),z=mc.player.getZ();
        for(double d=0;d<distance;){
            double next=Math.min(distance,d+0.5),nx=x+dx*next,nz=z+dz*next;
            if(!mc.world.isChunkLoaded((int)Math.floor(nx)>>4,(int)Math.floor(nz)>>4))return d;
            if(avoid.get()){
                var corridor=new Box(x+dx*d-0.4,floor+0.01,z+dz*d-0.4,x+dx*d+0.4,floor+3,z+dz*d+0.4).stretch(dx*(next-d),0,dz*(next-d));
                if(!mc.world.isSpaceEmpty(mc.player,corridor))return d;
            }
            if(holes.get()&&mc.world.isSpaceEmpty(mc.player,new Box(nx-0.35,floor-0.1,nz-0.35,nx+0.35,floor+0.001,nz+0.35)))return d;
            d=next;
        }
        return distance;
    }
}
