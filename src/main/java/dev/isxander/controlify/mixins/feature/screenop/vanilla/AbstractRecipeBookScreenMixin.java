package dev.isxander.controlify.mixins.feature.screenop.vanilla;

import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.screenop.compat.vanilla.RecipeBookScreenProcessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;


//? if >=1.21.2 {
@Mixin(AbstractRecipeBookScreen.class)
//?} else {
/*@Mixin(value = {
        InventoryScreen.class,
        AbstractFurnaceScreen.class,
        CraftingScreen.class
})
*///?}
public abstract class AbstractRecipeBookScreenMixin<T extends AbstractContainerMenu>
        implements ScreenProcessorProvider, /*? if <1.21.2 {*/ /*RecipeUpdateListener *//*?} else {*/ RecipeBookScreenProcessor.RecipeBookScreenAccessor/*?}*/ {

    @Unique
    private final RecipeBookScreenProcessor<?> processor =
            new RecipeBookScreenProcessor<>(/*? if >=1.21.2 {*/ (AbstractRecipeBookScreen<?>) (Object) /*?} else {*/ /*(Screen & RecipeUpdateListener) (Object) *//*?}*/this);

    //? if >=1.21.2 {
    @Shadow
    @Final
    private RecipeBookComponent<?> recipeBookComponent;

    @Override
    public RecipeBookComponent<?> getRecipeBookComponent() {
        return recipeBookComponent;
    }
    //?}

    @Override
    public ScreenProcessor<?> screenProcessor() {
        return processor;
    }
}
