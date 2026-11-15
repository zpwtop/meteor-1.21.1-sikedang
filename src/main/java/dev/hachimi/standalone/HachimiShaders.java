package dev.hachimi.standalone;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

public final class HachimiShaders extends HachimiModule {
    public enum Fill { Solid, Gradient, Rainbow }
    private final Setting<Double> distance=number("render-distance","实体着色最大距离。",50,1,200);
    private final Setting<Fill> fill=general.add(new EnumSetting.Builder<Fill>().name("fill").description("实体填充效果。").defaultValue(Fill.Solid).build());
    private final Setting<SettingColor> fillColor=general.add(new ColorSetting.Builder().name("fill-color").description("填充颜色。").defaultValue(new SettingColor(200,60,60,255)).build());
    private final Setting<SettingColor> outlineColor=general.add(new ColorSetting.Builder().name("outline-color").description("描边颜色。").defaultValue(new SettingColor(255,120,120,255)).build());
    private final Setting<Double> opacity=number("opacity","实体填充不透明度。",0.35,0,1);
    private final Setting<Boolean> outline=bool("outline","实体轮廓描边。",true);
    private final Setting<Integer> width=integer("outline-width","描边像素宽度。",2,1,6);
    private final Setting<Boolean> glow=bool("glow","描边外发光。",false);
    private final Setting<Boolean> dots=bool("dots","填充点阵效果。",false);
    private final Setting<Integer> spacing=integer("dot-spacing","点阵间距像素。",8,5,16);
    private final Setting<Double> speed=number("speed","渐变与彩虹动画速度。",1.5,0.01,10);
    private final Setting<Boolean> players=bool("players","渲染其他玩家。",true);
    private final Setting<Boolean> self=bool("self","第三人称下渲染自己。",true);
    private final Setting<Boolean> monsters=bool("monsters","渲染敌对生物。",true);
    private final Setting<Boolean> animals=bool("animals","渲染友好生物。",true);
    private final Setting<Boolean> items=bool("items","渲染掉落物。",true);
    private final Setting<Boolean> crystals=bool("crystals","渲染末地水晶。",true);
    private final Setting<Boolean> projectiles=bool("projectiles","渲染投射物。",true);
    private Effect effect;
    private EntityShader previous;
    public HachimiShaders(){super("着色器","参考 Hachimi 的实体着色：纯色、渐变、彩虹、点阵、描边与发光。");}
    @Override public void onActivate(){
        super.onActivate();
        try{
            if(effect==null){effect=new Effect();effect.init("hachimi-entity");}
            effect.onResized(mc.getWindow().getFramebufferWidth(),mc.getWindow().getFramebufferHeight());
            previous=PostProcessShaders.CHAMS;PostProcessShaders.CHAMS=effect;
        }catch(RuntimeException e){
            if(effect!=null&&effect.framebuffer!=null)effect.framebuffer.delete();
            effect=null;
            error("着色器初始化失败：%s",e.getMessage());if(isActive())toggle();
        }
    }
    @Override public void onDeactivate(){
        if(effect!=null&&PostProcessShaders.CHAMS==effect&&previous!=null){
            PostProcessShaders.CHAMS=previous;
            previous.onResized(mc.getWindow().getFramebufferWidth(),mc.getWindow().getFramebufferHeight());
        }
        previous=null;super.onDeactivate();
    }
    private boolean accepts(Entity e){
        if(!ready()||e==null||e.isRemoved()||e.squaredDistanceTo(mc.player)>distance.get()*distance.get())return false;
        if(e==mc.player)return self.get()&&!mc.options.getPerspective().isFirstPerson();
        if(e instanceof PlayerEntity)return players.get();
        if(e instanceof HostileEntity)return monsters.get();
        if(e instanceof PassiveEntity)return animals.get();
        if(e instanceof ItemEntity)return items.get();
        if(e instanceof EndCrystalEntity)return crystals.get();
        return e instanceof ProjectileEntity&&projectiles.get();
    }
    private final class Effect extends EntityShader {
        @Override protected boolean shouldDraw(){return isActive()&&ready();}
        @Override public boolean shouldDraw(Entity entity){return isActive()&&accepts(entity);}
        @Override protected void setUniforms(){
            shader.set("u_Fill",fillColor.get());shader.set("u_Outline",outlineColor.get());
            shader.set("u_Opacity",opacity.get());shader.set("u_FillMode",fill.get().ordinal());
            shader.set("u_Width",outline.get()?width.get():0);shader.set("u_Glow",glow.get());
            shader.set("u_Dots",dots.get());shader.set("u_Spacing",spacing.get());shader.set("u_Speed",speed.get());
        }
    }
}
