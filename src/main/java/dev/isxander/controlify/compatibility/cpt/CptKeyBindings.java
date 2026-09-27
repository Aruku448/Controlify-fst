package dev.isxander.controlify.compatibility.cpt;

import dev.isxander.controlify.api.bind.InputBinding;

/**
 * Identifies the Controlify bindings that were auto-generated from CPT's own {@code KeyMapping}s.
 *
 * <p>{@code ControlifyBindings.registerModdedBindings()} builds these ids from the key mapping
 * name, so CPT's keys land under the {@code key.createpneumatictacticals.*} path. Matching on
 * that prefix (rather than on the {@code fabric-key-binding-api-v1} namespace, which is an
 * implementation detail of the auto-registration) keeps this working if that namespace changes.
 */
public final class CptKeyBindings {
    private static final String KEY_PATH_PREFIX = "key.createpneumatictacticals.";

    private CptKeyBindings() {
    }

    public static boolean isCptBinding(InputBinding binding) {
        return binding.id().getPath().startsWith(KEY_PATH_PREFIX);
    }
}
