package dev.isxander.controlify.ingame;

import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.api.ingameinput.LookInputModifier;
import dev.isxander.controlify.compatibility.cpt.CptReflection;
import dev.isxander.controlify.controller.input.InputComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

/** Softly steers controller aim toward visible living targets while the player is aiming. */
public final class AimAssist {
    private AimAssist() {
    }

    public static void register() {
        ControlifyEvents.LOOK_INPUT_MODIFIER.register(AimAssist::modifyLookInput);
    }

    private static void modifyLookInput(LookInputModifier event) {
        InputComponent input = event.controller().input().orElse(null);
        if (input == null) return;

        InputComponent.Config config = input.confObj();
        if (!config.aimAssistEnabled) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) return;

        boolean useButtonHeld = ControlifyBindings.USE.on(event.controller()).digitalNow();
        if (!isAiming(player, useButtonHeld)) return;

        Target target = findTarget(player, config.aimAssistRange, config.aimAssistFov);
        if (target == null) return;

        // Apply a configurable fraction of the angular error even when the stick is centered.
        // This makes holding the aim input produce a noticeable soft lock without a hard camera takeover.
        Vector2f lookInput = event.lookInput();
        lookInput.x += target.yawError * config.aimAssistStrength;
        lookInput.y += target.pitchError * config.aimAssistStrength;
    }

    private static boolean isAiming(LocalPlayer player, boolean useButtonHeld) {
        if (player.isUsingItem()) {
            UseAnim animation = player.getUseItem().getUseAnimation();
            if (isAimAnimation(animation)) return true;
        }

        if (CptReflection.isGun(player.getMainHandItem()) && CptReflection.isAiming()) return true;

        return useButtonHeld && (isAimAnimation(player.getMainHandItem().getUseAnimation())
                || isAimAnimation(player.getOffhandItem().getUseAnimation()));
    }

    private static boolean isAimAnimation(UseAnim animation) {
        return animation == UseAnim.BOW || animation == UseAnim.CROSSBOW
                || animation == UseAnim.SPEAR || animation == UseAnim.SPYGLASS;
    }

    private static Target findTarget(LocalPlayer player, float range, float fieldOfView) {
        Vec3 eye = player.getEyePosition(1f);
        double rangeSqr = range * range;
        AABB searchArea = player.getBoundingBox().inflate(range);
        Target best = null;

        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchArea,
                candidate -> candidate != player && candidate.isAlive() && candidate.isPickable()
                        && !candidate.isSpectator() && player.hasLineOfSight(candidate))) {
            Vec3 targetPoint = entity.getEyePosition(1f);
            double distanceSqr = eye.distanceToSqr(targetPoint);
            if (distanceSqr > rangeSqr) continue;

            Vec3 direction = targetPoint.subtract(eye);
            double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance));
            float yawError = Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchError = Mth.wrapDegrees(targetPitch - player.getXRot());
            double angle = Math.hypot(yawError, pitchError);

            if (angle > fieldOfView) continue;
            if (best == null || angle < best.angle) {
                best = new Target(yawError, pitchError, angle);
            }
        }

        return best;
    }

    private record Target(float yawError, float pitchError, double angle) {
    }
}
