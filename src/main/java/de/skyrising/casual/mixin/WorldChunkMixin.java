package de.skyrising.casual.mixin;

import de.skyrising.casual.WorldDataTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements Chunk {
    @Shadow @Final private World world;

    /*
    @Inject(method = "loadFromPacket", at = @At("RETURN"))
    private void onLoadedFromPacket(PacketByteBuf buf, CompoundTag tag, int bitmask, boolean full, CallbackInfo ci) {
        if (!full) return;
        WorldDataTracker.getInstance(world).onChunkLoad((WorldChunk) (Object) this);
    }a
     */
}
