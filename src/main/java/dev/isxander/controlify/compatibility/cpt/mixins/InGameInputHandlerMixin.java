package dev.isxander.controlify.compatibility.cpt.mixins;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.compatibility.cpt.CptReflection;
import dev.isxander.controlify.ingame.InGameInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameInputHandler.class, remap = false)
public abstract class InGameInputHandlerMixin {
    @Unique private static final String CPT_CYCLE_AMMO_KEY = "key.createpneumatictacticals.cycle_ammo";

    @Shadow @Final private Minecraft minecraft;
    @Shadow private double lookInputX;
    @Shadow private double lookInputY;

    @Inject(method = "handlePlayerLookInput", at = @At("HEAD"), cancellable = true)
    private void controlify$freezeLookWhileCptAmmoWheelIsHeld(boolean isController, CallbackInfo ci) {
        if (!isController || minecraft.screen != null || minecraft.player == null
                || !CptReflection.isGun(minecraft.player.getMainHandItem())
                || !Controlify.instance().currentInputMode().isController()) {
            return;
        }

        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            if (CPT_CYCLE_AMMO_KEY.equals(keyMapping.getName()) && keyMapping.isDown()) {
                lookInputX = 0;
                lookInputY = 0;
                ci.cancel();
                return;
            }
        }
    }
}
