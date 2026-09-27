package dev.isxander.controlify.mixins.feature.screenop.vanilla;

import dev.isxander.controlify.mixins.feature.guide.screen.AbstractContainerScreenAccessor;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.screenop.compat.vanilla.CreativeModeInventoryScreenProcessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin implements ScreenProcessorProvider {
    @Unique
    private CreativeModeInventoryScreenProcessor controlify$creativeScreenProcessor = new CreativeModeInventoryScreenProcessor(
            (CreativeModeInventoryScreen) (Object) this,
            () -> ((AbstractContainerScreenAccessor) this).getHoveredSlot(),
            ((AbstractContainerScreenAccessor) this)::invokeSlotClicked,
            controller -> false
    );

    @Override
    public ScreenProcessor<?> screenProcessor() {
        return controlify$creativeScreenProcessor;
    }
}
