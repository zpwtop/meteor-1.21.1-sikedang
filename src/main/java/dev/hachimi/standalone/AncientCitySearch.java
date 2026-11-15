package dev.hachimi.standalone;

import dev.hachimi.standalone.city.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

public final class AncientCitySearch extends HachimiModule {
    private final Setting<String> seed=general.add(new StringSetting.Builder().name("seed").description("多人世界需填写真实的数字世界种子，0 也是有效种子。").defaultValue("").build());
    private final Setting<Boolean> localSeed=bool("singleplayer-seed","单人世界中，种子为空时自动读取本地世界种子。",true);
    private final Setting<Integer> radius=integer("radius","以搜索开始位置为中心的水平圆形半径，单位方块。",4096,384,32768);
    private final Setting<Integer> limit=integer("max-results","最多返回多少座古城，按水平距离排序。",10,1,64);
    private final Setting<Boolean> automatic=bool("auto-search","开启模块时开始搜索。",true);
    private final Setting<Boolean> render=bool("render","显示本次预测结果的游戏内标记；远处结果以聊天坐标为准。",true);
    private final Setting<Boolean> tracers=bool("tracers","向最近结果画引导线。",true);
    private final Setting<Boolean> search=general.add(new BoolSetting.Builder().name("search").description("点击开始或重新搜索，完成点击后自动复位。").defaultValue(false).onChanged(value->{if(value){searchNow();resetButton();}}).build());
    private final Setting<Boolean> cancel=general.add(new BoolSetting.Builder().name("cancel").description("取消当前搜索，保留已完成的结果。").defaultValue(false).onChanged(value->{if(value){cancelSearch();resetCancel();info("搜索已取消。");}}).build());
    private final CitySearchEngine engine=new CitySearchEngine();
    private ClientWorld searchWorld;
    private List<CityLocation> locations=List.of();
    private boolean searching,resetting;
    public AncientCitySearch(){super("古城搜索","使用已知种子和对应版本的原版生成规则，预测搜索半径内的古城位置。");}
    private void resetButton(){if(!resetting){resetting=true;try{search.set(false);}finally{resetting=false;}}}
    private void resetCancel(){cancel.set(false);}
    public List<CityLocation> results(){return locations;}
    public boolean searching(){return searching;}
    @Override public void onActivate(){super.onActivate();locations=List.of();searchWorld=mc.world;if(automatic.get())searchNow();}
    @Override public void onDeactivate(){cancelSearch();locations=List.of();searchWorld=null;super.onDeactivate();}
    public void cancelSearch(){generation++;engine.cancel();searching=false;resetButton();}
    public void searchNow(){
        if(!isActive()){warning("请先启用古城搜索。");return;}
        cancelSearch();locations=List.of();searchWorld=mc.world;
        if(!ready())return;
        if(mc.world.getRegistryKey()!=World.OVERWORLD){warning("古城只在主世界生成，请到主世界搜索。");return;}
        long chosen;
        try{
            if(seed.get().isBlank()&&localSeed.get()&&mc.getServer()!=null)chosen=mc.getServer().getOverworld().getSeed();
            else chosen=CityResults.seed(seed.get());
        }catch(IllegalArgumentException e){error("%s",e.getMessage());return;}
        CityArea area=new CityArea(mc.player.getBlockX(),mc.player.getBlockZ(),radius.get());
        int token=generation;var world=mc.world;searching=true;
        info("开始搜索古城：半径 %d 方块；默认原版生成规则。",radius.get());
        engine.search(chosen,area,limit.get(),result->mc.execute(()->{
            if(!isActive()||generation!=token||mc.world!=world)return;
            searching=false;locations=List.copyOf(result);
            info("搜索完成：返回最近 %d 座预测古城；点击坐标可复制。",result.size());
            if(result.isEmpty())info("范围内没有结果，可扩大半径；多人世界请确认种子与生成规则。");
            int index=0;
            for(var city:result){
                int distance=(int)Math.sqrt(area.distanceSquared(city.x(),city.z()));
                var text=Text.literal("#"+(++index)+" 古城 ["+city.coordinates()+"] 约 "+distance+" 格").formatted(Formatting.AQUA)
                    .styled(style->style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,city.coordinates()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Text.literal("点击复制坐标；这是默认原版种子预测结果。"))));
                info(text);
            }
        }),failure->mc.execute(()->{
            if(!isActive()||generation!=token||mc.world!=world)return;
            searching=false;error("古城搜索失败：%s",failure.getCause()==null?failure.getMessage():failure.getMessage()+" / "+failure.getCause().getMessage());
        }));
    }
    @EventHandler private void left(GameLeftEvent event){cancelSearch();locations=List.of();searchWorld=null;}
    @EventHandler private void tick(TickEvent.Pre event){
        if(mc.world!=searchWorld){cancelSearch();locations=List.of();searchWorld=mc.world;}
    }
    @EventHandler private void draw(Render3DEvent event){
        if(!render.get()||!ready()||mc.world!=searchWorld)return;
        int index=0;
        for(var city:locations){
            var pos=new BlockPos(city.x(),city.y(),city.z());
            event.renderer.box(pos,new Color(70,180,255,35),new Color(70,210,255,240),ShapeMode.Both,0);
            if(index++==0&&tracers.get())event.renderer.line(mc.player.getX(),mc.player.getEyeY(),mc.player.getZ(),city.x()+0.5,city.y()+0.5,city.z()+0.5,new Color(70,210,255,190));
        }
    }
    @Override public String getInfoString(){
        if(searching){var p=engine.progress();return p.total()==0?"准备中":p.completed()+"/"+p.total();}
        return locations.isEmpty()?null:locations.size()+" 座";
    }
}
