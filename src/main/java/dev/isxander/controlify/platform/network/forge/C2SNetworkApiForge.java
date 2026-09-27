//? if forge {
/*package dev.isxander.controlify.platform.network.forge;

import dev.isxander.controlify.platform.network.C2SNetworkApi;
import dev.isxander.controlify.platform.network.ControlifyPacketCodec;
import dev.isxander.controlify.platform.network.PacketPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class C2SNetworkApiForge implements C2SNetworkApi {
    public static final C2SNetworkApiForge INSTANCE = new C2SNetworkApiForge();
    private final Map<ResourceLocation, ControlifyPacketCodec<?>> codecs = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, PacketListener<?>> listeners = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Boolean> registered = new ConcurrentHashMap<>();

    @Override
    public <T> void sendPacket(ResourceLocation channel, T packet) {
        ForgeNetworkChannels.get(channel).sendToServer(encode(codec(channel), packet));
    }

    @Override
    public <T> PacketPayload createPayload(ResourceLocation channel, T packet) {
        return null;
    }

    @Override
    public <T> void listenForPacket(ResourceLocation channel, PacketListener<T> listener) {
        listeners.put(channel, listener);
    }

    @Override
    public <T> void registerPacket(ResourceLocation channel, ControlifyPacketCodec<T> codec) {
        codecs.put(channel, codec);
        if (registered.putIfAbsent(channel, true) == null) {
            ForgeNetworkChannels.get(channel).registerMessage(0, byte[].class,
                    (bytes, buf) -> buf.writeByteArray(bytes), FriendlyByteBuf::readByteArray,
                    (bytes, context) -> receive(channel, bytes, context),
                    java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ControlifyPacketCodec<T> codec(ResourceLocation channel) {
        return (ControlifyPacketCodec<T>) codecs.get(channel);
    }

    private <T> byte[] encode(ControlifyPacketCodec<T> codec, T packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buffer, packet);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.release();
        return bytes;
    }

    @SuppressWarnings("unchecked")
    private <T> void receive(ResourceLocation channel, byte[] bytes, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            T packet = (T) codec(channel).decode(buffer);
            buffer.release();
            PacketListener<T> listener = (PacketListener<T>) listeners.get(channel);
            if (listener != null && context.getSender() != null)
                listener.listen(packet, context.getSender());
        });
        context.setPacketHandled(true);
    }
}
*///?}
