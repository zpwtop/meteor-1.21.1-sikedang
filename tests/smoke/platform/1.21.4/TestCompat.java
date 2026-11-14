package dev.hachimi.test;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
public final class TestCompat {
    private TestCompat() {}
    public static void teleport(ServerPlayerEntity player,ServerWorld world){player.teleport(world,0.5,91,0.5,java.util.Set.of(),0,0,false);}
}
