package dev.isxander.controlify.compatibility.cpt;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.platform.client.PlatformClientUtil;
import dev.isxander.controlify.rumble.TriggerRumbleState;

public final class CptTriggerRumble {
    private static ControllerEntity activeController;
    private static TriggerRumbleState activeState = TriggerRumbleState.NONE;
    private static int ticksRemaining;

    static {
        PlatformClientUtil.registerClientTickEnded(minecraft -> tick());
    }

    private CptTriggerRumble() {}

    public static synchronized void play(ControllerEntity controller, float left, float right, int durationTicks) {
        if (controller.triggerRumble().isEmpty()) return;

        activeController = controller;
        activeState = new TriggerRumbleState(clamp(left), clamp(right));
        ticksRemaining = Math.max(1, durationTicks);
        controller.triggerRumble().orElseThrow().queueTriggerRumble(activeState);
    }

    private static synchronized void tick() {
        if (activeController == null) return;

        if (Controlify.instance().getCurrentController().orElse(null) != activeController
                || !Controlify.instance().currentInputMode().isController()) {
            stop();
            return;
        }

        if (ticksRemaining-- > 0) {
            activeController.triggerRumble().ifPresent(component -> component.queueTriggerRumble(activeState));
        } else {
            stop();
        }
    }

    private static void stop() {
        if (activeController != null) {
            activeController.triggerRumble()
                    .ifPresent(component -> component.queueTriggerRumble(TriggerRumbleState.NONE));
        }
        activeController = null;
        activeState = TriggerRumbleState.NONE;
        ticksRemaining = 0;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
