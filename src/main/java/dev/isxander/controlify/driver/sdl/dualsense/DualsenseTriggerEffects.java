package dev.isxander.controlify.driver.sdl.dualsense;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

// https://gist.github.com/Nielk1/6d54cc2c00d2201ccb8c2720ad7538db
public final class DualsenseTriggerEffects {
    /**
     * Turn the trigger effect off and return the trigger stop to the neutral position.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#OFF
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect off() {
        return new DS5EffectsState.TriggerEffect(EffectType.OFF, new byte[0]);
    }

    /**
     * Trigger will resist movement beyond the start position.
     * The trigger status nybble will report 0 before the effect and 1 when in the effect.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#FEEDBACK
     *
     * @param position The starting zone of the trigger effect. Must be between 0 and 9 inclusive.
     * @param strength The force of the resistance. Must be between 0 and 8 inclusive.
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect feedback(
            @Range(from = 0, to = 9) byte position,
            @Range(from = 0, to = 8) byte strength
    ) {
        Validate.inclusiveBetween(0, 9, position, "Position must be between 0 and 9 inclusive");
        Validate.inclusiveBetween(0, 8, strength, "Strength must be between 0 and 8 inclusive");

        if (strength > 0) {
            byte forceValue = (byte) ((strength - 1) & 0x07);
            int forceZones = 0;
            char activeZones = 0;
            for (int i = position; i < 10; i++) {
                forceZones |= (forceValue << (3 * i));
                activeZones |= (char) (1 << i);
            }

            return new DS5EffectsState.TriggerEffect(EffectType.FEEDBACK, new byte[]{
                    (byte) (activeZones & 0xff),
                    (byte) ((activeZones >> 8) & 0xff),
                    (byte) (forceZones & 0xff),
                    (byte) ((forceZones >> 8) & 0xff),
                    (byte) ((forceZones >> 16) & 0xff),
                    (byte) ((forceZones >> 24) & 0xff),
            });
        } else {
            return off();
        }
    }

    /**
     * Trigger will resist movement beyond the start position until the end position.
     * The trigger status nybble will report 0 before the effect and 1 when in the effect,
     * and 2 after until again before the start position.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#WEAPON
     *
     * @param startPosition The starting zone of the trigger effect. Must be between 2 and 7 inclusive.
     * @param endPosition The ending zone of the trigger effect. Must be between start+1 and 8 inclusive.
     * @param strength The force of the resistance. Must be between 0 and 8 inclusive.
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect weapon(
            @Range(from = 2, to = 7) byte startPosition,
            @Range(from = 2+1, to = 8) byte endPosition,
            @Range(from = 0, to = 8) byte strength
    ) {
        Validate.inclusiveBetween(2, 7, startPosition, "Start position must be between 2 and 7 inclusive");
        Validate.inclusiveBetween(startPosition+1, 8, endPosition, "End position must be between start+1 and 8 inclusive");
        Validate.inclusiveBetween(0, 8, strength, "Strength must be between 0 and 8 inclusive");
        Validate.isTrue(startPosition < endPosition, "Start position must be less than end position");

        if (strength > 0) {
            char startAndStopZones = (char) ((1 << startPosition) | (1 << endPosition));

            return new DS5EffectsState.TriggerEffect(EffectType.WEAPON, new byte[]{
                    (byte) (startAndStopZones & 0xff),
                    (byte) ((startAndStopZones >> 8) & 0xff),
                    (byte) (strength - 1), // this is actually packed into 3 bits, but since it's only one why bother with the fancy code?
            });
        } else {
            return off();
        }
    }

    /**
     * Trigger will vibrate with the input amplitude and frequency beyond the start position.
     * The trigger status nybble will report 0 before the effect and 1 when in the effect.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#VIBRATION
     * @see #vibrationMultiplePosition(byte, byte[])
     *
     * @param position The starting zone of the trigger effect. Must be between 0 and 9 inclusive.
     * @param amplitude Strength of the automatic cycling action. Must be between 0 and 8 inclusive.
     * @param frequency Frequency of the automatic cycling action in hertz.
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect vibration(
            @Range(from = 0, to = 9) byte position,
            @Range(from = 0, to = 8) byte amplitude,
            byte frequency
    ) {
        Validate.inclusiveBetween(0, 9, position, "Position must be between 0 and 9 inclusive");
        Validate.inclusiveBetween(0, 8, amplitude, "Amplitude must be between 0 and 8 inclusive");

        if (amplitude > 0 && frequency > 0) {
            byte strengthValue = (byte) ((amplitude - 1) & 0x07);
            int amplitudeZones = 0;
            char activeZones = 0;

            for (int i = position; i < 10; i++) {
                amplitudeZones |= (strengthValue << (3 * i));
                activeZones |= (char) (1 << i);
            }

            return new DS5EffectsState.TriggerEffect(EffectType.VIBRATION, new byte[]{
                    (byte) (activeZones & 0xff),
                    (byte) ((activeZones >> 8) & 0xff),
                    (byte) (amplitudeZones & 0xff),
                    (byte) ((amplitudeZones >> 8) & 0xff),
                    (byte) ((amplitudeZones >> 16) & 0xff),
                    (byte) ((amplitudeZones >> 24) & 0xff),
                    0, 0,
                    frequency,
            });
        } else {
            return off();
        }
    }

    /**
     * Trigger will resist movement at varying strengths in 10 regions.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#FEEDBACK
     * @see #feedback(byte, byte)
     * @see #feedbackSlope(byte, byte, byte, byte)
     *
     * @param strength Array of 10 resistance values for zones 0 through 9. Must be between 0 and 8 inclusive.
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect feedbackMultiplePosition(@Range(from = 0, to = 9) byte @NotNull [] strength) {
        Validate.notNull(strength, "Strength array must not be null");
        Validate.isTrue(strength.length == 10, "Strength array must have 10 elements");

        boolean allZero = true;
        for (int i = 0; i < 10; i++) {
            allZero &= strength[i] == 0;
            Validate.inclusiveBetween(0, 8, strength[i], "Strength i=%s must be between 0 and 8 inclusive".formatted(i));
        }

        if (!allZero) {
            int forceZones = 0;
            char activeZones = 0;

            for (int i = 0; i < 10; i++) {
                byte strengthValue = strength[i];
                if (strengthValue > 0) {
                    byte forceValue = (byte) ((strengthValue - 1) & 0x07);
                    forceZones |= (forceValue << (3 * i));
                    activeZones |= (char) (1 << i);
                }
            }

            return new DS5EffectsState.TriggerEffect(EffectType.FEEDBACK, new byte[]{
                    (byte) (activeZones & 0xff),
                    (byte) ((activeZones >> 8) & 0xff),
                    (byte) (forceZones & 0xff),
                    (byte) ((forceZones >> 8) & 0xff),
                    (byte) ((forceZones >> 16) & 0xff),
                    (byte) ((forceZones >> 24) & 0xff),
            });
        } else {
            return off();
        }
    }

    /**
     * Trigger will resist movement at a linear range of strengths.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#FEEDBACK
     * @see #feedback(byte, byte)
     * @see #feedbackMultiplePosition(byte[])
     *
     * @param startPosition The starting zone of the trigger effect. Must be between 0 and 8 inclusive.
     * @param endPosition The ending zone of the trigger effect. Must be between start+1 and 9 inclusive.
     * @param startStrength The force of the resistance at the start position. Must be between 1 and 8 inclusive.
     * @param endStrength The force of the resistance at the end. Must be between 1 and 8 inclusive.
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect feedbackSlope(
            @Range(from = 0, to = 8) byte startPosition,
            @Range(from = 1, to = 9) byte endPosition,
            @Range(from = 1, to = 8) byte startStrength,
            @Range(from = 1, to = 8) byte endStrength
    ) {
        Validate.inclusiveBetween(0, 8, startPosition, "Start position must be between 0 and 8 inclusive");
        Validate.inclusiveBetween(startPosition+1, 9, endPosition, "End position must be between start+1 and 9 inclusive");
        Validate.inclusiveBetween(1, 8, startStrength, "Start strength must be between 1 and 8 inclusive");
        Validate.inclusiveBetween(1, 8, endStrength, "End strength must be between 1 and 8 inclusive");
        Validate.isTrue(startPosition < endPosition, "Start strength must be less than end position");

        byte[] strength = new byte[10];
        float gradient = (endStrength - startStrength) / (float) (endPosition - startPosition);
        for (int i = startPosition; i < 10; i++) {
            strength[i] = i <= endPosition
                    ? (byte) Math.round(startStrength + gradient * (i - startPosition))
                    : endStrength;
        }

        return feedbackMultiplePosition(strength);
    }

    /**
     * Trigger will vibrate movement at varying amplitudes and one frequency in 10 regions.
     * This is an official effect and is expected to be present in future DualSense firmware versions.
     *
     * @see EffectType#VIBRATION
     * @see #vibration(byte, byte, byte)
     *
     * @param frequency Frequency of the automatic cycling action in hertz.
     * @param amplitude Array of 10 strength values for zones 0 through 9. Must between 0 and 8 inclusive.
     */
    @Contract(pure = true)
    public static DS5EffectsState.TriggerEffect vibrationMultiplePosition(
            byte frequency,
            @Range(from = 0, to = 8) byte @NotNull [] amplitude
    ) {
        Validate.notNull(amplitude, "Amplitude array must not be null");
        Validate.isTrue(amplitude.length == 10, "Amplitude array must have 10 elements");

        if (frequency > 0) {
            boolean allZero = true;
            for (int i = 0; i < 10; i++) {
                allZero &= amplitude[i] == 0;
                Validate.inclusiveBetween(0, 8, amplitude[i], "Amplitude i=%s must be between 0 and 8 inclusive".formatted(i));
            }

            if (!allZero) {
                int strengthZones = 0;
                char activeZones = 0;

                for (int i = 0; i < 10; i++) {
                    byte amplitudeValue = amplitude[i];

                    if (amplitudeValue > 0) {
                        byte strengthValue = (byte) ((amplitudeValue - 1) & 0x07);
                        strengthZones |= (strengthValue << (3 * i));
                        activeZones |= (char) (1 << i);
                    }
                }

                return new DS5EffectsState.TriggerEffect(EffectType.VIBRATION, new byte[]{
                        (byte) (activeZones & 0xff),
                        (byte) ((activeZones >> 8) & 0xff),
                        (byte) (strengthZones & 0xff),
                        (byte) ((strengthZones >> 8) & 0xff),
                        (byte) ((strengthZones >> 16) & 0xff),
                        (byte) ((strengthZones >> 24) & 0xff),
                        0, 0,
                        frequency,
                });
            }
        }

        return off();
    }

    public enum EffectType {
        // Officially recognised modes
        // These are 100% safe and are the only effects that modify the trigger status nibble
        OFF(0x05),       // 00 00 0 101
        FEEDBACK(0x21),  // 00 10 0 001
        WEAPON(0x25),    // 00 10 0 101
        VIBRATION(0x26), // 00 10 0 110

        // Unofficial but unique effects left in the firmware
        // These might be removed in the future
        BOW(0x22),       // 00 10 0 010
        GALLOPING(0x23), // 00 10 0 011
        MACHINE(0x27),   // 00 10 0 111

        // Leftover versions of official modes with simpler logic and no parameter protections
        // These should not be used
        SIMPLE_FEEDBACK(0x01),  // 00 00 0 001
        SIMPLE_WEAPON(0x02),    // 00 00 0 010
        SIMPLE_VIBRATION(0x03), // 00 00 0 011

        // Leftover versions of official modes with limited parameter ranges
        // These should not be used
        LIMITED_FEEDBACK(0x11), // 00 01 0 001
        LIMITED_WEAPON(0x12),   // 00 01 0 010

        // Debug or calibration functions
        // Don't use these as they will corrupt the trigger state until the reset button is pressed
        DEBUG_FC(0xFC), // 11 11 1 100
        DEBUG_FD(0xFD), // 11 11 1 101
        DEBUG_FE(0xFE), // 11 11 1 110
        ;

        public final byte value;

        EffectType(int value) {
            this.value = (byte) value;
        }
    }
}
