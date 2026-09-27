package dev.isxander.controlify.compatibility.cpt.mixins;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.compatibility.cpt.CptRumbleProfiles;
import dev.isxander.controlify.compatibility.cpt.CptReflection;
import dev.isxander.controlify.compatibility.cpt.CptTriggerRumble;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.rumble.BasicRumbleEffect;
import dev.isxander.controlify.rumble.RumbleSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ignis.createpneumatictacticals.client.ClientGunInput", remap = false)
public abstract class ClientGunInputMixin {
    @Inject(method = "tryFire", at = @At("TAIL"), remap = false)
    private static void controlify$rumbleOnSuccessfulShot(Player player, ItemStack gunStack, @Coerce Object stats, CallbackInfo ci) {
        if (!Controlify.instance().currentInputMode().isController()) return;

        ControllerEntity controller = Controlify.instance().getCurrentController().orElse(null);
        if (controller == null) return;

        String ammoId = CptReflection.loadedAmmo(gunStack);
        CptRumbleProfiles.Profile profile = CptRumbleProfiles.forAmmo(ammoId);
        float damageScale = controlify$getDamageScale(ammoId, stats, profile.damageReference());
        float strong = controlify$scaleAmplitude(profile.strong(), damageScale);
        float weak = controlify$scaleAmplitude(profile.weak(), damageScale);
        float triggerLeft = controlify$scaleAmplitude(profile.triggerLeft(), damageScale);
        float triggerRight = controlify$scaleAmplitude(profile.triggerRight(), damageScale);

        controller.rumble().ifPresent(rumble -> rumble.rumbleManager().play(
                RumbleSource.PLAYER,
                BasicRumbleEffect.constant(strong, weak, profile.durationTicks())
        ));
        CptTriggerRumble.play(controller, triggerLeft, triggerRight, profile.triggerDurationTicks());
    }

    private static float controlify$getDamageScale(String ammoId, Object stats, float damageReference) {
        if (ammoId == null) return 1f;
        double damage = CptReflection.shotDamage(ammoId, stats);
        if (!Double.isFinite(damage)) return 1f;
        if (!Double.isFinite(damage) || damage <= 0) return 1f;
        return (float) Math.max(0.25, Math.min(2.0, damage / damageReference));
    }

    private static float controlify$scaleAmplitude(float amplitude, float scale) {
        return Math.max(0f, Math.min(1f, amplitude * scale));
    }
}
