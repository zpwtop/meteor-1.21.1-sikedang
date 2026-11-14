package dev.hachimi.test;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.orbit.EventHandler;
import dev.hachimi.standalone.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

public final class SmokeAddon extends MeteorAddon {
    private int tick,elytraTicks;
    private boolean failed;
    private final BlockPos target=new BlockPos(2,91,0);
    private EntityShader previous;
    @Override public String getPackage(){return "dev.hachimi.test";}
    @Override public void onInitialize(){MeteorClient.EVENT_BUS.subscribe(this);}
    private void check(boolean value,String message){if(!value){failed=true;MeteorClient.LOG.error("HACHIMI_ACCEPTANCE FAILED: "+message);throw new AssertionError(message);}}
    @SuppressWarnings({"rawtypes","unchecked"})
    @EventHandler private void joined(GameJoinedEvent event){
        tick=0;elytraTicks=0;failed=false;
        Config.get().customFont.set(false);
        Class<?>[] types={HachimiCrystal.class,HachimiSelfTrap.class,HachimiSurround.class,HachimiMace.class,
            HachimiFakeFly.class,HachimiPotion.class,SlimeArmorFly.class,SlimeElytraControl.class,HachimiPacketMine.class,HachimiShaders.class};
        for(Class<?> type:types)check(Modules.get().get((Class)type)!=null,"module missing "+type.getSimpleName());
        MeteorClient.LOG.info("HACHIMI_ACCEPTANCE: ten distinct production modules registered");
    }
    @EventHandler(priority=0) private void pre(TickEvent.Pre event){
        if(tick>=180&&tick<230&&Modules.get().isActive(SlimeArmorFly.class)&&MeteorClient.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA))elytraTicks++;
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    @EventHandler(priority=-500) private void tick(TickEvent.Post event){
        if(failed)return;
        var mc=MeteorClient.mc;
        if(mc.player==null||mc.world==null)return;
        ++tick;
        if(tick==20){
            var server=mc.getServer();var uuid=mc.player.getUuid();
            check(server!=null,"not an integrated world");
            server.execute(()->{
                var player=server.getPlayerManager().getPlayer(uuid);var world=player.getServerWorld();
                for(int x=-4;x<=4;x++)for(int z=-4;z<=5;z++){
                    world.setBlockState(new BlockPos(x,90,z),Blocks.STONE.getDefaultState());
                    for(int y=91;y<99;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                }
                world.setTimeOfDay(6000);
                player.changeGameMode(GameMode.SURVIVAL);
                player.teleport(world,0.5,91,0.5,0,0);
                player.getInventory().clear();
                player.getInventory().setStack(0,new ItemStack(Items.STICK));
                player.getInventory().setStack(2,new ItemStack(Items.DIAMOND_PICKAXE));
                player.getInventory().setStack(24,new ItemStack(Items.ELYTRA));
                player.equipStack(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));
                player.currentScreenHandler.sendContentUpdates();
                world.setBlockState(target,Blocks.STONE.getDefaultState());
                var pig=EntityType.PIG.create(world);pig.refreshPositionAndAngles(1.5,91,3.5,0,0);pig.setAiDisabled(true);world.spawnEntity(pig);
            });
        }
        if(tick==80){
            check(!mc.player.isCreative(),"server has not changed to survival");
            check(mc.world.getBlockState(target).isOf(Blocks.STONE),"test target missing");
            var mine=Modules.get().get(HachimiPacketMine.class);if(!mine.isActive())mine.toggle();
            MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(target,Direction.UP));
            MeteorClient.LOG.info("HACHIMI_ACCEPTANCE: survival packet-mining requested");
        }
        if(tick==140){
            check(mc.world.getBlockState(target).isAir(),"packet mining did not receive real server block removal");
            check(mc.player.getInventory().selectedSlot==0,"packet mine did not restore selected hotbar slot");
            check(mc.player.getInventory().getStack(2).isOf(Items.DIAMOND_PICKAXE),"pickaxe moved unexpectedly");
            Modules.get().get(HachimiPacketMine.class).toggle();
            MeteorClient.LOG.info("HACHIMI_ACCEPTANCE: PASS survival stone mined and server-confirmed; tool restored");
            var shader=Modules.get().get(HachimiShaders.class);previous=PostProcessShaders.CHAMS;
            ((Setting)shader.settings.get("fill")).set(HachimiShaders.Fill.Rainbow);
            ((Setting)shader.settings.get("glow")).set(true);
            ((Setting)shader.settings.get("dots")).set(true);
            shader.toggle();check(shader.isActive(),"GPU shader did not activate");
            mc.player.setYaw(-20);mc.player.setPitch(5);
            MeteorClient.LOG.info("HACHIMI_ACCEPTANCE: GPU rainbow/dots/glow rendering active");
        }
        if(tick==180){
            Modules.get().get(SlimeArmorFly.class).toggle();
            Modules.get().get(SlimeElytraControl.class).toggle();
        }
        if(tick>185&&tick<230){
            check(mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.NETHERITE_CHESTPLATE),"chestplate not restored after player tick");
            check(mc.player.getInventory().getStack(24).isOf(Items.ELYTRA),"elytra not restored to inventory slot 24");
        }
        if(tick==230){
            check(elytraTicks>0,"armor fly never equipped elytra during player tick");
            Modules.get().get(SlimeElytraControl.class).toggle();Modules.get().get(SlimeArmorFly.class).toggle();
            check(mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.NETHERITE_CHESTPLATE),"stop did not restore chestplate");
            check(!mc.player.hasNoGravity(),"control did not restore gravity");
            var shader=Modules.get().get(HachimiShaders.class);shader.toggle();
            check(PostProcessShaders.CHAMS==previous,"original Meteor renderer not restored");
            shader.toggle(); // Keep the verified effect visible for a screenshot; test helper only.
            MeteorClient.LOG.info("HACHIMI_ACCEPTANCE: PASS armor-fly pre/post equipment, control and shader restoration; elytra ticks="+elytraTicks);
        }
        if(tick==260)MeteorClient.LOG.info("HACHIMI_ACCEPTANCE: ALL PASS; leaving shader visible for inspection");
    }
}
