package dev.hachimi.standalone.city;

import dev.hachimi.standalone.GameCompat;
import com.mojang.serialization.Lifecycle;
import net.minecraft.datafixer.Schemas;
import net.minecraft.registry.*;
import net.minecraft.resource.*;
import net.minecraft.structure.*;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.*;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.*;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;
import net.minecraft.world.level.storage.LevelStorage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Isolated vanilla structure-start prediction. Does not load or edit the player's chunks. */
public final class CityPredictionWorld implements AutoCloseable {
    private final long seed;
    private final Path temporary;
    private final LevelStorage.Session session;
    private final LifecycledResourceManager resources;
    private final DynamicRegistryManager registries;
    private final NoiseChunkGenerator generator;
    private final NoiseConfig noise;
    private final StructureTemplateManager templates;
    private final Structure ancientCity;
    private final RandomSpreadStructurePlacement placement;
    public CityPredictionWorld(long seed) throws IOException {
        this.seed=seed;
        temporary=Files.createTempDirectory("sikedang-city-");
        LevelStorage.Session opened=null;LifecycledResourceManager data=null;
        try {
            var lookup=BuiltinRegistries.createWrapperLookup();
            var settings=GameCompat.worldgenRegistry(lookup,RegistryKeys.CHUNK_GENERATOR_SETTINGS).getOrThrow(ChunkGeneratorSettings.OVERWORLD);
            var biomes=MultiNoiseBiomeSource.create(GameCompat.worldgenRegistry(lookup,RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
            generator=new NoiseChunkGenerator(biomes,settings);
            noise=NoiseConfig.create(settings.value(),GameCompat.worldgenRegistry(lookup,RegistryKeys.NOISE_PARAMETERS),seed);
            var pools=new SimpleRegistry<StructurePool>(RegistryKeys.TEMPLATE_POOL,Lifecycle.stable());
            GameCompat.worldgenRegistry(lookup,RegistryKeys.TEMPLATE_POOL).streamEntries().forEach(entry->Registry.register(pools,entry.getKey().orElseThrow(),entry.value()));
            registries=new DynamicRegistryManager.ImmutableImpl(List.of(pools.freeze()));
            opened=LevelStorage.create(temporary).createSession("templates");
            data=new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA,List.of(VanillaDataPackProvider.createDefaultPack()));
            templates=new StructureTemplateManager(data,opened,Schemas.getFixer(),GameCompat.worldgenRegistry(lookup,RegistryKeys.BLOCK));
            ancientCity=GameCompat.worldgenRegistry(lookup,RegistryKeys.STRUCTURE).getOrThrow(StructureKeys.ANCIENT_CITY).value();
            var set=GameCompat.worldgenRegistry(lookup,RegistryKeys.STRUCTURE_SET).getOrThrow(StructureSetKeys.ANCIENT_CITIES).value();
            if(!(set.placement() instanceof RandomSpreadStructurePlacement spread))throw new IllegalStateException("Ancient city placement is not random spread");
            placement=spread;
            session=opened;resources=data;
        } catch(Throwable failure) {
            if(data!=null)data.close();
            if(opened!=null)try{opened.close();}catch(IOException e){failure.addSuppressed(e);}
            removeTemporary(temporary);
            if(failure instanceof IOException io)throw io;
            throw new IOException("Cannot initialize vanilla ancient-city prediction",failure);
        }
    }
    public int regionSizeBlocks(){return placement.getSpacing()*16;}
    public ChunkPos candidate(int regionX,int regionZ){return placement.getStartChunk(seed,regionX*placement.getSpacing(),regionZ*placement.getSpacing());}
    public Optional<BlockPos> locate(ChunkPos chunk){
        var context=new Structure.Context(registries,generator,generator.getBiomeSource(),noise,templates,seed,chunk,HeightLimitView.create(-64,384),biome->biome.matchesKey(BiomeKeys.DEEP_DARK));
        return ancientCity.getValidStructurePosition(context).map(Structure.StructurePosition::position);
    }
    @Override public void close() throws IOException {
        resources.close();
        try{session.close();}finally{removeTemporary(temporary);}
    }
    private static void removeTemporary(Path path) throws IOException {
        if(!Files.exists(path))return;
        try(var paths=Files.walk(path)){
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);
        }
    }
}
