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
    private int miningWait;
    private int tick,elytraTicks,durablePackets,durableGliding,baselineDamage,baselineWear,durableDamage;
    private boolean failed,maceObserved;
    private final BlockPos target=new BlockPos(2,91,0);
    private EntityShader previous;
    @Override public String getPackage(){return "dev.hachimi.test";}
    @Override public void onInitialize(){MeteorClient.EVENT_BUS.subscribe(this);MeteorClient.EVENT_BUS.subscribe(new CityChecks(this));MeteorClient.EVENT_BUS.subscribe(new JumpChecks(this));}
    private void check(boolean value,String message){if(!value){failed=true;MeteorClient.LOG.error("SIKEDANG_ACCEPTANCE FAILED: "+message);throw new AssertionError(message);}}
    @SuppressWarnings({"rawtypes","unchecked"})
    @EventHandler private void joined(GameJoinedEvent event){
        tick=0;elytraTicks=0;miningWait=0;failed=false;
        Config.get().customFont.set(false);
        Class<?>[] types={HachimiCrystal.class,HachimiSelfTrap.class,HachimiSurround.class,HachimiMace.class,
            HachimiFakeFly.class,HachimiPotion.class,SlimeArmorFly.class,SlimeElytraControl.class,HachimiPacketMine.class,HachimiShaders.class,DurableElytra.class,AncientCitySearch.class,JumpingTiger.class};
        for(Class<?> type:types) {
            var module=Modules.get().get((Class)type);
            check(module!=null,"module missing "+type.getSimpleName());
            if(module.isActive())module.toggle();
        }
        String[] current={"自动水晶","自动围住","自动围脚","重锤秒切","假飞","自动药水","甲飞","简易鞘翅控制","发包挖掘","着色器","无限耐久鞘翅","古城搜索","跳跳虎"};
        for(int i=0;i<types.length;i++){
            var module=Modules.get().get((Class)types[i]);
            check(module.name.equals(current[i]),"module name still contains a source prefix");
            check(module.category==HachimiAddon.CATEGORY,"module in old source category");
            if(i<10){String old=(i==6||i==7?"slime":"hachimi")+current[i];check(Modules.get().get(old)==module,"old configuration lookup failed: "+old);}
        }
        MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: thirteen modules, clean names and ten legacy configuration lookups verified");
    }
    @EventHandler(priority=-100) private void sent(meteordevelopment.meteorclient.events.packets.PacketEvent.Send event){
        if(!event.isCancelled()&&event.packet instanceof net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket command
            &&command.getMode()==net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING
            &&Modules.get().isActive(DurableElytra.class))durablePackets++;
        if(!event.isCancelled()&&event.packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
            &&MeteorClient.mc.player.getMainHandStack().isOf(Items.MACE))maceObserved=true;
    }
    @EventHandler(priority=0) private void pre(TickEvent.Pre event){
        if(tick>=180&&tick<230&&Modules.get().isActive(SlimeArmorFly.class)&&MeteorClient.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA))elytraTicks++;
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    @EventHandler(priority=-500) private void tick(TickEvent.Post event){
        if(failed||Boolean.getBoolean("sikedang.city.fast")||Boolean.getBoolean("sikedang.jump.fast"))return;
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
                TestCompat.teleport(player,world);
                player.getInventory().clear();
                player.getInventory().setStack(0,new ItemStack(Items.STICK));
                player.getInventory().setStack(2,new ItemStack(Items.DIAMOND_PICKAXE));
                player.getInventory().setStack(24,new ItemStack(Items.ELYTRA));
                player.getInventory().setStack(5,new ItemStack(Items.MACE));
                player.equipStack(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));
                player.currentScreenHandler.sendContentUpdates();
                world.setBlockState(target,Blocks.STONE.getDefaultState());
                var pig=new net.minecraft.entity.passive.PigEntity(EntityType.PIG,world);pig.refreshPositionAndAngles(1.5,91,3.5,0,0);pig.setAiDisabled(true);world.spawnEntity(pig);
            });
        }
        if(tick==80){
            check(!mc.player.isCreative(),"server has not changed to survival");
            check(mc.world.getBlockState(target).isOf(Blocks.STONE),"test target missing");
            var mine=Modules.get().get(HachimiPacketMine.class);if(!mine.isActive())mine.toggle();
            MeteorClient.EVENT_BUS.post(StartBreakingBlockEvent.get(target,Direction.UP));
            MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: survival packet-mining requested");
        }
        if(tick==140){
            if(!mc.world.getBlockState(target).isAir()&&miningWait++<140&&Modules.get().get(HachimiPacketMine.class).getInfoString()!=null){--tick;return;}
            check(mc.world.getBlockState(target).isAir(),"packet mining did not receive real server block removal: progress="+Modules.get().get(HachimiPacketMine.class).getInfoString()+", ground="+mc.player.isOnGround()+", glide="+GameCompat.isGliding(mc.player)+", slot="+mc.player.getInventory().selectedSlot+", wait="+miningWait);
            check(mc.player.getInventory().selectedSlot==0,"packet mine did not restore selected hotbar slot");
            check(mc.player.getInventory().getStack(2).isOf(Items.DIAMOND_PICKAXE),"pickaxe moved unexpectedly");
            Modules.get().get(HachimiPacketMine.class).toggle();
            MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: PASS survival stone mined and server-confirmed; tool restored");
            var shader=Modules.get().get(HachimiShaders.class);previous=PostProcessShaders.CHAMS;
            ((Setting)shader.settings.get("fill")).set(HachimiShaders.Fill.Rainbow);
            ((Setting)shader.settings.get("glow")).set(true);
            ((Setting)shader.settings.get("dots")).set(true);
            shader.toggle();check(shader.isActive(),"GPU shader did not activate");
            mc.player.setYaw(-20);mc.player.setPitch(5);
            MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: GPU rainbow/dots/glow rendering active");
        }
        if(tick==160){
            net.minecraft.entity.Entity pig=null;
            for(var entity:mc.world.getEntities())if(entity instanceof net.minecraft.entity.passive.PigEntity){pig=entity;break;}
            check(pig!=null,"test pig missing");
            var packet=net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.attack(pig,false);
            check(GameCompat.attackTarget(packet)==pig,"attack packet target adapter failed");
            var mace=Modules.get().get(HachimiMace.class);mace.toggle();maceObserved=false;
            mc.getNetworkHandler().sendPacket(packet);
            check(maceObserved,"attack packet was not sent while holding mace");
            check(mc.player.getInventory().selectedSlot==0,"mace did not restore selected slot");
            mace.toggle();
            MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: PASS mace target lookup and attack-before-restore switch");
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
            MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: PASS armor-fly pre/post equipment, control and shader restoration; elytra ticks="+elytraTicks);
        }
        if(tick==280){
            var server=mc.getServer();var uuid=mc.player.getUuid();
            server.execute(()->{
                var player=server.getPlayerManager().getPlayer(uuid);
                TestCompat.teleportHigh(player,player.getServerWorld());
                var elytra=new ItemStack(Items.ELYTRA);elytra.setDamage(17);
                player.equipStack(EquipmentSlot.CHEST,elytra);
                player.getInventory().setStack(7,new ItemStack(Items.DIAMOND,13));
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.RESISTANCE,1000,4));
                player.currentScreenHandler.sendContentUpdates();
            });
        }
        if(tick==310){
            check(mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA),"durability fixture not equipped");
            var control=Modules.get().get(SlimeElytraControl.class);if(!control.isActive())control.toggle();
            GameCompat.startGliding(mc.player);
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player,net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
        if(tick==330){
            check(GameCompat.isGliding(mc.player),"baseline glide failed");
            baselineDamage=mc.player.getEquippedStack(EquipmentSlot.CHEST).getDamage();
        }
        if(tick==390){
            baselineWear=mc.player.getEquippedStack(EquipmentSlot.CHEST).getDamage()-baselineDamage;
            check(baselineWear>0,"baseline did not demonstrate normal elytra durability loss");
            durableDamage=mc.player.getEquippedStack(EquipmentSlot.CHEST).getDamage();durablePackets=0;durableGliding=0;
            Modules.get().get(DurableElytra.class).toggle();
        }
        if(tick>390&&tick<510){
            check(mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA),"durable reset left chest slot swapped");
            check(mc.player.getInventory().getStack(7).isOf(Items.DIAMOND)&&mc.player.getInventory().getStack(7).getCount()==13,"durable reset changed buffer contents");
            if(GameCompat.isGliding(mc.player))durableGliding++;
        }
        if(tick==395){
            var flag=new net.minecraft.entity.data.DataTracker.SerializedEntry<>(0,net.minecraft.entity.data.TrackedDataHandlerRegistry.BYTE,(byte)1);
            var extra=new net.minecraft.entity.data.DataTracker.SerializedEntry<>(99,net.minecraft.entity.data.TrackedDataHandlerRegistry.BYTE,(byte)42);
            var packet=new net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket(mc.player.getId(),java.util.List.of(flag,extra));
            var protectedPacket=DurableElytra.protectUpdate(packet);
            check(protectedPacket!=packet,"local glide flag not protected");
            check(protectedPacket.trackedValues().get(0).value().equals((byte)129),"other player flag bits were changed");
            check(protectedPacket.trackedValues().get(1)==extra&&packet.trackedValues().get(0)==flag,"other metadata or original packet changed");
            var remote=new net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket(mc.player.getId()+100,packet.trackedValues());
            check(DurableElytra.protectUpdate(remote)==remote,"another entity metadata was changed");
        }
        if(tick==510){
            check(durablePackets>=10,"too few durable glide-reset requests observed");
            check(durableGliding>=100,"durable reset interrupted continuous gliding");
            check(mc.player.getInventory().selectedSlot==0,"durable reset changed selected slot");
            int wear=mc.player.getEquippedStack(EquipmentSlot.CHEST).getDamage()-durableDamage;
            DurableElytra.protectUpdate(new net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket(mc.player.getId(),java.util.List.of(new net.minecraft.entity.data.DataTracker.SerializedEntry<>(0,net.minecraft.entity.data.TrackedDataHandlerRegistry.BYTE,(byte)0))));
            Modules.get().get(DurableElytra.class).toggle();Modules.get().get(SlimeElytraControl.class).toggle();
            check(!GameCompat.isGliding(mc.player),"suppressed server stop was not restored on disable");
            var disabledPacket=new net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket(mc.player.getId(),java.util.List.of(new net.minecraft.entity.data.DataTracker.SerializedEntry<>(0,net.minecraft.entity.data.TrackedDataHandlerRegistry.BYTE,(byte)0)));
            check(DurableElytra.protectUpdate(disabledPacket)==disabledPacket,"metadata protection remained active after disable");
            check(!mc.player.hasNoGravity(),"durability test did not restore gravity");
            MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: durable swap/request safety PASS; normalWear60="+baselineWear+", durableWear120="+wear+", glideTicks="+durableGliding+", resetPackets="+durablePackets);
            var server=mc.getServer();var uuid=mc.player.getUuid();server.execute(()->MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: server confirmed elytra damage="+server.getPlayerManager().getPlayer(uuid).getEquippedStack(EquipmentSlot.CHEST).getDamage()));
        }
        if(tick==540)MeteorClient.LOG.info("SIKEDANG_ACCEPTANCE: ALL PASS");
    }
    private static final class CityChecks {
        private final SmokeAddon owner;
        private int ticks,stage,deadline;
        private dev.hachimi.standalone.city.CityArea area;
        private java.util.concurrent.CompletableFuture<String> oracle;
        CityChecks(SmokeAddon owner){this.owner=owner;}
        @EventHandler private void joined(GameJoinedEvent event){ticks=0;stage=0;deadline=0;oracle=null;}
        @SuppressWarnings({"unchecked","rawtypes"})
        private void setting(AncientCitySearch module,String name,Object value){((Setting)module.settings.get(name)).set(value);}
        @EventHandler(priority=-600) private void tick(TickEvent.Post event){
            if(owner.failed||Boolean.getBoolean("sikedang.jump.fast"))return;
            var mc=MeteorClient.mc;if(mc.player==null||mc.world==null)return;
            ticks++;
            var module=Modules.get().get(AncientCitySearch.class);
            if(stage==0&&ticks>=(Boolean.getBoolean("sikedang.city.fast")?25:560)){
                owner.check(mc.world.getRegistryKey()==net.minecraft.world.World.OVERWORLD,"city test did not start in overworld");
                setting(module,"seed","");setting(module,"radius",4096);setting(module,"max-results",12);setting(module,"singleplayer-seed",true);
                area=new dev.hachimi.standalone.city.CityArea(mc.player.getBlockX(),mc.player.getBlockZ(),4096);
                module.toggle();stage=1;deadline=ticks+1200;
                MeteorClient.LOG.info("CITY_ACCEPTANCE: native singleplayer-seed search started");
            }
            if(stage==1){
                owner.check(ticks<=deadline,"city prediction timed out");
                if(module.searching())return;
                owner.check(!module.results().isEmpty(),"city search produced no results; see search failure log");
                var result=java.util.List.copyOf(module.results());
                long previous=-1;
                for(var city:result){long distance=area.distanceSquared(city.x(),city.z());owner.check(area.contains(city.x(),city.z())&&distance>=previous,"unbounded or unsorted city results");previous=distance;}
                var server=mc.getServer();long worldSeed=server.getOverworld().getSeed();
                oracle=java.util.concurrent.CompletableFuture.supplyAsync(()->{
                    java.util.List<net.minecraft.util.math.ChunkPos> rejected=new java.util.ArrayList<>();
                    try(var fixture=new dev.hachimi.standalone.city.CityPredictionWorld(0)){
                        var chunk=fixture.candidate(2,3);
                        if(chunk.x!=50||chunk.z!=79||fixture.locate(chunk).isEmpty())throw new AssertionError("seed-0 ancient-city fixture mismatch");
                    }catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
                    try(var model=new dev.hachimi.standalone.city.CityPredictionWorld(worldSeed)){
                        outer:for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                            var candidate=model.candidate(x,z);
                            if(model.locate(candidate).isEmpty())rejected.add(candidate);
                            if(rejected.size()>=8)break outer;
                        }
                    }catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
                    var verified=new java.util.concurrent.CompletableFuture<String>();
                    server.execute(()->{
                        try{
                            var world=server.getOverworld();
                            var structure=GameCompat.worldgenRegistry(world.getRegistryManager(),net.minecraft.registry.RegistryKeys.STRUCTURE).getOrThrow(net.minecraft.world.gen.structure.StructureKeys.ANCIENT_CITY).value();
                            int positive=0;
                            for(var city:result.subList(0,Math.min(3,result.size()))){
                                var chunk=world.getChunk(city.chunkX(),city.chunkZ(),net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS);
                                var start=chunk.getStructureStart(structure);
                                if(start==null||!start.hasChildren())throw new AssertionError("predicted city absent in generated chunk "+city);
                                var box=start.getBoundingBox();
                                if(city.x()<box.getMinX()||city.x()>box.getMaxX()||city.z()<box.getMinZ()||city.z()>box.getMaxZ())throw new AssertionError("predicted marker outside actual city "+city);
                                positive++;
                            }
                            for(var pos:rejected){
                                var start=world.getChunk(pos.x,pos.z,net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS).getStructureStart(structure);
                                if(start!=null&&start.hasChildren())throw new AssertionError("false rejection at "+pos);
                            }
                            verified.complete("positive="+positive+", rejected="+rejected.size()+", results="+result.size()+", seed0=(50,79)");
                        }catch(Throwable failure){verified.completeExceptionally(failure);}
                    });
                    return verified.join();
                });
                stage=2;deadline=ticks+1800;
            }
            if(stage==2){
                owner.check(ticks<=deadline,"vanilla structure oracle timed out");
                if(!oracle.isDone())return;
                try{MeteorClient.LOG.info("CITY_ACCEPTANCE: vanilla generated STRUCTURE_STARTS match: "+oracle.join());}
                catch(Throwable failure){MeteorClient.LOG.error("CITY_ACCEPTANCE: oracle error",failure);owner.check(false,"vanilla city oracle failed: "+failure);}
                setting(module,"radius",32768);module.searchNow();module.cancelSearch();
                owner.check(!module.searching()&&module.results().isEmpty(),"cancel did not clear current search");
                stage=3;deadline=ticks+30;
            }
            if(stage==3&&ticks>=deadline){
                owner.check(!module.searching()&&module.results().isEmpty(),"cancelled worker published stale cities");
                setting(module,"seed","not-a-number");module.searchNow();owner.check(!module.searching(),"invalid seed started prediction");
                setting(module,"seed","0");setting(module,"radius",2048);setting(module,"max-results",64);module.searchNow();
                stage=4;deadline=ticks+1200;
            }
            if(stage==4){
                owner.check(ticks<=deadline,"seed-0 replacement search timed out");
                if(module.searching())return;
                owner.check(module.results().stream().anyMatch(city->city.chunkX()==50&&city.chunkZ()==79),"numeric zero seed was not used or replacement search returned stale data");
                module.toggle();owner.check(module.results().isEmpty()&&!module.searching(),"disable retained city results");
                MeteorClient.LOG.info("CITY_ACCEPTANCE: PASS seed-0 search, cancellation, stale-result guard, disable cleanup");
                stage=5;
                MeteorClient.LOG.info("CITY_ACCEPTANCE: ALL PASS");
            }
        }
    }
    private static final class JumpChecks {
        private final SmokeAddon owner;
        private int ticks,stage,deadline,glide,jumps,requests,rotations,landings;
        private boolean ground=true;
        private double startZ;
        private java.util.concurrent.CompletableFuture<Void> prepared;
        JumpChecks(SmokeAddon owner){this.owner=owner;}
        @EventHandler private void joined(GameJoinedEvent e){ticks=stage=glide=jumps=requests=rotations=landings=0;}
        @EventHandler private void send(meteordevelopment.meteorclient.events.packets.PacketEvent.Send e){
            if(stage==3&&e.packet instanceof net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket p&&p.getMode()==net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)requests++;
        }
        @EventHandler private void beforeSend(meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent.Pre e){
            if(stage==3&&Math.abs(MeteorClient.mc.player.getPitch()-80)<0.01)rotations++;
        }
        @EventHandler(priority=-700) private void tick(TickEvent.Post e){
            if(owner.failed)return;
            var mc=MeteorClient.mc;if(mc.player==null||mc.world==null)return;
            ticks++;var module=Modules.get().get(JumpingTiger.class);
            if(stage==0&&ticks>=(Boolean.getBoolean("sikedang.jump.fast")?25:720)){
                prepared=new java.util.concurrent.CompletableFuture<>();var server=mc.getServer();var id=mc.player.getUuid();
                server.execute(()->{
                    try{
                        var world=server.getOverworld();var player=server.getPlayerManager().getPlayer(id);
                        for(int x=-5;x<=5;x++)for(int z=-5;z<=120;z++){
                            world.setBlockState(new BlockPos(x,90,z),Blocks.STONE.getDefaultState());
                            for(int y=91;y<96;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                        }
                        player.getInventory().clear();player.equipStack(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
                        player.setVelocity(0,0,0);player.setNoGravity(false);TestCompat.teleport(player,world);
                        player.changeGameMode(GameMode.SURVIVAL);player.currentScreenHandler.sendContentUpdates();
                        prepared.complete(null);
                    }catch(Throwable failure){prepared.completeExceptionally(failure);}
                });stage=1;deadline=ticks+500;
            }
            if(stage==1){owner.check(ticks<deadline,"jump lane preparation timed out");if(!prepared.isDone())return;prepared.join();stage=2;deadline=ticks+20;}
            if(stage==2&&ticks>=deadline){
                owner.check(mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA),"jump test missing equipped elytra");
                mc.player.setYaw(0);mc.player.setPitch(33);mc.player.setVelocity(0,0,0);mc.player.setNoGravity(false);GameCompat.stopGliding(mc.player);
                var input=mc.player.input;float f=input.movementForward,s=input.movementSideways;
                Runnable undo=GameCompat.jumpInput(input,true,false);owner.check(input.movementForward==1&&input.movementSideways==0,"jump input override missing");undo.run();
                owner.check(input.movementForward==f&&input.movementSideways==s,"jump input adapter did not restore");
                try{
                    @SuppressWarnings("unchecked") var avoid=(Setting<Boolean>)module.settings.get("auto-avoid-obstacles");
                    @SuppressWarnings("unchecked") var holes=(Setting<Boolean>)module.settings.get("avoid-holes");
                    var clearance=JumpingTiger.class.getDeclaredMethod("clearDistance",float.class,double.class);clearance.setAccessible(true);
                    var wall=new BlockPos(0,91,3);var previous=mc.world.getBlockState(wall);mc.world.setBlockState(wall,Blocks.STONE.getDefaultState());
                    mc.player.setVelocity(0,0,0.4);avoid.set(true);
                    double blocked=(Double)clearance.invoke(module,0f,91d),free=(Double)clearance.invoke(module,90f,91d);
                    owner.check(blocked<4&&free>10,"jump obstacle forecast failed: "+blocked+" / "+free);
                    mc.world.setBlockState(wall,previous);avoid.set(false);holes.set(true);
                    var gap=new BlockPos(0,90,2);previous=mc.world.getBlockState(gap);mc.world.setBlockState(gap,Blocks.AIR.getDefaultState());
                    double missing=(Double)clearance.invoke(module,0f,91d);
                    owner.check(missing<3,"jump hole forecast failed: "+missing);
                    mc.world.setBlockState(gap,previous);holes.set(false);mc.player.setVelocity(0,0,0);
                    MeteorClient.LOG.info("JUMP_ACCEPTANCE: obstacle and missing-ground forecast PASS");
                }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
                startZ=mc.player.getZ();module.toggle();stage=3;deadline=ticks+240;
                MeteorClient.LOG.info("JUMP_ACCEPTANCE: automatic jump/glide cycle started with no physical keys");
            }
            if(stage==3){
                boolean now=mc.player.isOnGround();if(ground&&!now)jumps++;if(!ground&&now)landings++;ground=now;
                if(GameCompat.isGliding(mc.player))glide++;
                owner.check(Math.abs(mc.player.getPitch()-33)<0.01&&Math.abs(mc.player.getYaw())<0.01,"jump module left camera rotation changed after tick");
                owner.check(!mc.player.hasNoGravity(),"jump module changed gravity");
                owner.check(mc.player.input.movementForward==0&&mc.player.input.movementSideways==0,"jump module left forced input after tick");
                if(ticks<deadline)return;
                double distance=mc.player.getZ()-startZ;
                MeteorClient.LOG.info("JUMP_ACCEPTANCE: observed jumps="+jumps+", landings="+landings+", glideTicks="+glide+", requests="+requests+", temporaryRotations="+rotations+", forwardBlocks="+distance);
                owner.check(jumps>=2&&landings>=2,"jump module did not repeat ground/air cycles");
                owner.check(glide>=2&&requests>=2&&rotations>=2,"jump module did not start repeated real gliding");
                owner.check(distance>3,"jump module did not move automatically");
                module.toggle();owner.check(!module.isActive(),"jump disable failed");
                stage=4;deadline=ticks+10;
            }
            if(stage==4&&ticks>=deadline){
                owner.check(mc.player.input.movementForward==0&&mc.player.input.movementSideways==0,"jump input remained after disable");
                owner.check(Math.abs(mc.player.getPitch()-33)<0.01,"jump pitch remained after disable");
                MeteorClient.LOG.info("JUMP_ACCEPTANCE: ALL PASS repeated jumps/glides, input/camera restoration, disable cleanup");stage=5;
            }
        }
    }
}
