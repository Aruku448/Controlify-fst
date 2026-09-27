//? if forge {
/*package dev.isxander.controlify.platform.network.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ForgeNetworkChannels {
    private static final Map<ResourceLocation, SimpleChannel> CHANNELS = new ConcurrentHashMap<>();

    static SimpleChannel get(ResourceLocation id) {
        return CHANNELS.computeIfAbsent(id, key -> NetworkRegistry.newSimpleChannel(
                key,
                () -> "1",
                NetworkRegistry.acceptMissingOr("1"),
                NetworkRegistry.acceptMissingOr("1")
        ));
    }

    static byte[] encode(dev.isxander.controlify.platform.network.ControlifyPacketCodec<Object> codec, Object packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        codec.encode(buffer, packet);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.release();
        return bytes;
    }

    private ForgeNetworkChannels() {
    }
}
*///?}
