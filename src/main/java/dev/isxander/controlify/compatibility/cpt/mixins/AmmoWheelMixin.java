package dev.isxander.controlify.compatibility.cpt.mixins;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.compatibility.cpt.CptRumbleProfiles;
import dev.isxander.controlify.compatibility.cpt.CptReflection;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.input.GamepadInputs;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "dev.ignis.createpneumatictacticals.client.AmmoWheel", remap = false)
public abstract class AmmoWheelMixin {
    @Shadow private static int selected;
    @Shadow private static List<String> entries;

    @Inject(method = "updateSelection", at = @At("HEAD"), cancellable = true, remap = false)
    private static void controlify$selectAmmoWithRightStick(Minecraft minecraft, CallbackInfo ci) {
        if (minecraft.player == null || !CptReflection.isGun(minecraft.player.getMainHandItem())) return;

        List<String> discoveredAmmo = new ArrayList<>(entries == null ? List.of() : entries);
        if (minecraft.player != null) {
            String loadedAmmo = CptReflection.loadedAmmo(minecraft.player.getMainHandItem());
            if (loadedAmmo != null) discoveredAmmo.add(loadedAmmo);
        }
        CptRumbleProfiles.observeAmmoIds(discoveredAmmo);

        if (Controlify.instance().currentInputMode() == InputMode.KEYBOARD_MOUSE) return;

        ControllerEntity controller = Controlify.instance().getCurrentController().orElse(null);
        if (controller == null || controller.input().isEmpty()) return;

        var input = controller.input().orElseThrow();
        var state = input.stateNow();
        float x = state.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_RIGHT)
                - state.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_LEFT);
        float y = state.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_DOWN)
                - state.getAxisState(GamepadInputs.RIGHT_STICK_AXIS_UP);
        float threshold = input.confObj().buttonActivationThreshold;

        if (Math.abs(x) < threshold && Math.abs(y) < threshold) {
            selected = -1;
            ci.cancel();
            return;
        }

        if (entries == null || entries.isEmpty()) {
            selected = -1;
            ci.cancel();
            return;
        }

        double angle = 90.0 - Math.toDegrees(Math.atan2(-y, x));
        if (angle < 0.0) angle += 360.0;
        if (angle >= 360.0) angle -= 360.0;

        float each = 360.0f / entries.size();
        selected = Math.min((int) (((angle + each / 2.0) % 360.0) / each), entries.size() - 1);
        ci.cancel();
    }
}
