package dev.isxander.controlify.compatibility.cpt;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional reflective access to CPT public APIs without linking against its classes. */
public final class CptReflection {
    private static final String GUN_NBT = "dev.ignis.createpneumatictacticals.gun.GunNbt";
    private static final String AMMO_EXTENSION = "dev.ignis.createpneumatictacticals.ammo.AmmoExtension";
    private static final String MOD_KEYBINDS = "dev.ignis.createpneumatictacticals.client.ModKeybinds";
    private static volatile Class<?> geoGunClass;
    private static volatile boolean gunClassLookupAttempted;
    private static volatile Method isAimingMethod;
    private static volatile boolean isAimingLookupAttempted;

    private CptReflection() {
    }

    public static boolean isGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Class<?> gunClass = geoGunClass;
        if (!gunClassLookupAttempted) {
            synchronized (CptReflection.class) {
                if (!gunClassLookupAttempted) {
                    try {
                        geoGunClass = loadClass("dev.ignis.createpneumatictacticals.item.GeoGunItem");
                    } catch (ClassNotFoundException | LinkageError ignored) {
                        geoGunClass = null;
                    }
                    gunClassLookupAttempted = true;
                }
                gunClass = geoGunClass;
            }
        }
        return gunClass != null && gunClass.isInstance(stack.getItem());
    }

    public static boolean isAiming() {
        Method method = isAimingMethod;
        if (!isAimingLookupAttempted) {
            synchronized (CptReflection.class) {
                if (!isAimingLookupAttempted) {
                    try {
                        isAimingMethod = loadClass(MOD_KEYBINDS).getMethod("isAiming");
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        isAimingMethod = null;
                    }
                    isAimingLookupAttempted = true;
                }
                method = isAimingMethod;
            }
        }
        if (method == null) return false;

        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static String loadedAmmo(ItemStack stack) {
        try {
            Class<?> gunNbt = loadClass(GUN_NBT);
            Method getAmmo = gunNbt.getMethod("getAmmo", ItemStack.class);
            Object result = getAmmo.invoke(null, stack);
            return result instanceof String ammoId ? ammoId : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    public static double shotDamage(String ammoId, Object gunStats) {
        if (ammoId == null || gunStats == null) return Double.NaN;
        try {
            Class<?> ammoExtension = loadClass(AMMO_EXTENSION);
            Method getExtension = ammoExtension.getMethod("get", String.class);
            Object extension = getExtension.invoke(null, ammoId);
            if (extension == null) return Double.NaN;

            Field ammoDamage = ammoExtension.getField("damage");
            Field damageMultiplier = gunStats.getClass().getField("damageMultiplier");
            return ammoDamage.getDouble(extension) * damageMultiplier.getDouble(gunStats);
        } catch (ReflectiveOperationException | LinkageError e) {
            return Double.NaN;
        }
    }

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            try {
                return Class.forName(name, false, contextLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return Class.forName(name, false, CptReflection.class.getClassLoader());
    }
}
