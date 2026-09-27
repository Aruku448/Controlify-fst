package dev.isxander.controlify;

import dev.isxander.controlify.server.ControlifyServer;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;

public class ControlifyBootstrap implements ClientModInitializer, ModInitializer, DedicatedServerModInitializer {
    @Override
    public void onInitializeClient() {
        Controlify.instance().preInitialiseControlify();
    }

    @Override
    public void onInitializeServer() {
        ControlifyServer.getInstance().onInitializeServer();
    }

    @Override
    public void onInitialize() {
        ControlifyServer.getInstance().onInitialize();
    }
}
//?} elif neoforge {
/*import dev.isxander.controlify.gui.screen.ModConfigOpenerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod("controlify")
public class ControlifyBootstrap {
    public ControlifyBootstrap(IEventBus modBus) {
        ControlifyServer.getInstance().onInitialize();

        ModLoadingContext.get().registerExtensionPoint(
                //? if >=1.20.6 {
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                () -> (client, parent) -> new ModConfigOpenerScreen(parent)
                //?} else {
                /^net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new ModConfigOpenerScreen(parent))
                ^///?}
        );

        modBus.addListener(FMLCommonSetupEvent.class, event -> {
            event.enqueueWork(ControlifyServer.getInstance()::onInitialize);
        });
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            event.enqueueWork(Controlify.instance()::preInitialiseControlify);
        });
        modBus.addListener(FMLDedicatedServerSetupEvent.class, event -> {
            event.enqueueWork(ControlifyServer.getInstance()::onInitializeServer);
        });
    }
}
*///?} elif forge {
/*import dev.isxander.controlify.gui.screen.ModConfigOpenerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("controlify")
public class ControlifyBootstrap {
    public ControlifyBootstrap() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Register before Forge starts the initial client resource reload.
            Controlify.instance().registerAssetReloadListeners();
        }
        ControlifyServer.getInstance().onInitialize();

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new ModConfigOpenerScreen(parent))
        );

        modBus.<FMLCommonSetupEvent>addListener(event ->
                event.enqueueWork(ControlifyServer.getInstance()::onInitialize));
        modBus.<FMLClientSetupEvent>addListener(event -> {
            event.enqueueWork(Controlify.instance()::preInitialiseControlify);
            java.util.concurrent.atomic.AtomicBoolean fallbackRan = new java.util.concurrent.atomic.AtomicBoolean();
            MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent tick) -> {
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                if (!fallbackRan.get()
                        && tick.phase == net.minecraftforge.event.TickEvent.Phase.END
                        && (client.screen != null || client.level != null)) {
                    fallbackRan.set(true);
                    Controlify.instance().initializeControlify();
                }
            });
        });
        modBus.<FMLDedicatedServerSetupEvent>addListener(event ->
                event.enqueueWork(ControlifyServer.getInstance()::onInitializeServer));
    }
}
*///?}
