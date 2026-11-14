package dev.hachimi.standalone;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public final class HachimiPacketMine extends HachimiModule {
    private final Setting<Double> range=number("range","可持续挖掘的距离。",5,1,6);
    private final Setting<Double> speed=number("speed","发送完成包所需的预计进度；原版默认 0.99。",0.99,0.1,2);
    private final Setting<Boolean> autoSwitch=bool("auto-switch","发送完成包时临时使用快捷栏最快工具，随后恢复。",true);
    private final Setting<Boolean> rotate=bool("rotate","开始和结束挖掘时转头。",true);
    private final Setting<Boolean> pauseUse=bool("pause-on-use","使用物品时暂停。",true);
    private final Setting<Boolean> instant=bool("instant-remine","方块被挖掉后短暂保留位置；同种方块放回时自动重新挖掘。",false);
    private final Setting<Integer> retryDelay=integer("retry-ticks","等待服务器确认的 tick 数。",10,2,60);
    private final Setting<Integer> maxRetries=integer("max-retries","服务器没有确认时最多重试次数。",3,0,10);
    private final Setting<Boolean> render=bool("render","显示挖掘目标及预计进度。",true);
    private Task task;
    private static final class Task {
        final BlockPos pos;final Direction side;final BlockState state;final ClientWorld world;
        MiningCycle cycle;boolean started,pending,wasAir;int airTicks;
        Task(BlockPos pos,Direction side,BlockState state,ClientWorld world,double threshold){this.pos=pos;this.side=side;this.state=state;this.world=world;cycle=new MiningCycle(threshold);}
    }
    public HachimiPacketMine(){super("hachimi发包挖掘","参考 Hachimi SpeedMine 的单目标发包挖掘；松开鼠标后继续等待完成。");}
    @Override public void onActivate(){super.onActivate();task=null;}
    @Override public void onDeactivate(){clear(true);super.onDeactivate();}
    @EventHandler(priority=100) private void start(StartBreakingBlockEvent event){
        if(event.isCancelled()||!inventoryReady()||mc.player.isCreative()||mc.player.isSpectator()||!BlockUtils.canBreak(event.blockPos)
            ||!within(event.blockPos))return;
        event.cancel();
        if(task!=null&&task.world==mc.world&&task.pos.equals(event.blockPos))return;
        clear(true);
        task=new Task(event.blockPos.toImmutable(),event.direction,mc.world.getBlockState(event.blockPos),mc.world,speed.get());
    }
    private boolean within(BlockPos pos){return mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos))<=range.get()*range.get();}
    @EventHandler private void tick(TickEvent.Pre event){
        Task t=task;
        if(t==null)return;
        if(!ready()||mc.world!=t.world){clear(false);return;}
        if(!within(t.pos)){clear(true);return;}
        BlockState current=mc.world.getBlockState(t.pos);
        if(current.isAir()){
            mc.world.setBlockBreakingInfo(mc.player.getId(),t.pos,-1);
            if(instant.get()&&++t.airTicks<=100){t.wasAir=true;return;}
            clear(false);return;
        }
        if(current.getBlock()!=t.state.getBlock()){clear(true);return;}
        if(t.wasAir){t.wasAir=false;t.airTicks=0;t.started=false;t.pending=false;t.cycle=new MiningCycle(speed.get());}
        if(!inventoryReady()||(pauseUse.get()&&mc.player.isUsingItem())||t.pending)return;
        if(t.cycle.waiting()){
            if(!t.cycle.acknowledgementExpired(retryDelay.get()))return;
            if(!t.cycle.retry(maxRetries.get())){warning("服务器未确认挖掘，已停止重试。");clear(true);return;}
            t.started=false;
        }
        if(!t.started){
            aimed(t,()->{action(t,PlayerActionC2SPacket.Action.START_DESTROY_BLOCK);t.started=true;mc.player.swingHand(Hand.MAIN_HAND);});
            return;
        }
        int best=mc.player.getInventory().selectedSlot;
        double delta=BlockUtils.getBreakDelta(best,current);
        if(autoSwitch.get())for(int i=0;i<9;i++){
            double candidate=BlockUtils.getBreakDelta(i,current);
            if(candidate>delta){delta=candidate;best=i;}
        }
        if(t.cycle.advance(delta)){
            final int tool=best;
            aimed(t,()->{
                Runnable finish=()->{action(t,PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK);mc.player.swingHand(Hand.MAIN_HAND);};
                if(autoSwitch.get())withSlot(tool,finish);else finish.run();
                t.cycle.stopSent();
            });
        }
    }
    private void aimed(Task t,Runnable action){
        int token=generation;t.pending=true;
        Runnable guarded=()->{
            try{if(isActive()&&token==generation&&task==t&&ready()&&mc.world==t.world&&within(t.pos)&&inventoryReady())action.run();}
            finally{t.pending=false;}
        };
        if(rotate.get())Rotations.rotate(Rotations.getYaw(t.pos),Rotations.getPitch(t.pos),80,guarded);else guarded.run();
    }
    private void action(Task t,PlayerActionC2SPacket.Action action){
        try(var update=((dev.hachimi.standalone.mixin.ClientWorldInvoker)mc.world).hachimi$getPendingUpdateManager().incrementSequence()){
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(action,t.pos,t.side,update.getSequence()));
        }
    }
    private void clear(boolean abort){
        Task old=task;task=null;
        if(old!=null&&mc.player!=null&&mc.world==old.world&&mc.getNetworkHandler()!=null){
            mc.world.setBlockBreakingInfo(mc.player.getId(),old.pos,-1);
            if(abort&&old.started)action(old,PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK);
        }
    }
    @EventHandler private void draw(Render3DEvent event){
        if(!render.get()||task==null||!ready()||mc.world!=task.world||task.wasAir)return;
        double progress=task.cycle.fraction();
        int red=(int)(255*(1-progress)),green=(int)(220*progress);
        event.renderer.box(task.pos,new Color(red,green,40,40),new Color(red,green,40,220),ShapeMode.Both,0);
    }
    @Override public String getInfoString(){return task==null?null:(int)(100*task.cycle.fraction())+"%";}
}
