package dev.isxander.controlify.controller.dualsense;

import dev.isxander.controlify.controller.ECSComponent;
import dev.isxander.controlify.driver.sdl.dualsense.DS5EffectsState;
import dev.isxander.controlify.utils.CUtil;
import net.minecraft.resources.ResourceLocation;

public class DualSenseComponent implements ECSComponent {
    public static final ResourceLocation ID = CUtil.rl("dualsense");

    private boolean muteLight;

    private DS5EffectsState.TriggerEffect leftTriggerEffect;
    private DS5EffectsState.TriggerEffect rightTriggerEffect;

    private boolean dirty;

    public void setLeftTriggerEffect(DS5EffectsState.TriggerEffect effect) {
        this.leftTriggerEffect = effect;
        this.setDirty();
    }

    public DS5EffectsState.TriggerEffect getLeftTriggerEffect() {
        return this.leftTriggerEffect;
    }

    public void setRightTriggerEffect(DS5EffectsState.TriggerEffect effect) {
        this.rightTriggerEffect = effect;
        this.setDirty();
    }

    public DS5EffectsState.TriggerEffect getRightTriggerEffect() {
        return this.rightTriggerEffect;
    }

    public void setMuteLight(boolean on) {
        if (this.muteLight != on) {
            this.muteLight = on;
            this.setDirty();
        }
    }

    public boolean getMuteLight() {
        return this.muteLight;
    }

    private void setDirty() {
        this.dirty = true;
    }

    public boolean consumeDirty() {
        boolean old = this.dirty;
        this.dirty = false;
        return old;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
