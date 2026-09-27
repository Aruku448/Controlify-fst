package dev.isxander.controlify.mixins.feature.virtualmouse.snapping;

import dev.isxander.controlify.api.vmousesnapping.ISnapBehaviour;
import dev.isxander.controlify.mixins.feature.guide.screen.AbstractContainerScreenAccessor;
import dev.isxander.controlify.api.vmousesnapping.SnapPoint;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin implements ISnapBehaviour {
    @Shadow protected abstract int getTabX(CreativeModeTab group);
    @Shadow private float scrollOffs;
    @Shadow private EditBox searchBox;
    @Shadow protected abstract boolean canScroll();

    @Override
    public Set<SnapPoint> getSnapPoints() {
        var accessor = (AbstractContainerScreenAccessor) this;
        var screen = (CreativeModeInventoryScreen) (Object) this;
        int leftPos = accessor.getLeftPos();
        int topPos = accessor.getTopPos();
        int imageHeight = accessor.getImageHeight();
        Set<SnapPoint> points = screen.getMenu().slots.stream()
                .map(slot -> new SnapPoint(new Vector2i(leftPos + slot.x + 8, topPos + slot.y + 8), 17))
                .collect(Collectors.toCollection(HashSet::new));
        for (var tab : CreativeModeTabs.tabs()) {
            boolean topRow = tab.row() == CreativeModeTab.Row.TOP;
            int x = leftPos + getTabX(tab);
            int y = topPos + (topRow ? -28 : imageHeight - 4);

            points.add(new SnapPoint(new Vector2i(x + 13, y + 16), 18));
        }

        if (canScroll()) {
            int scrollTop = topPos + 18;
            int scrollBottom = scrollTop + 112;
            points.add(new SnapPoint(new Vector2i(leftPos + 175 + 6, scrollTop + (int)((float)(scrollBottom - scrollTop - 17) * scrollOffs) + 7), 15));
        }

        if (searchBox.isVisible())
            points.add(new SnapPoint(new Vector2i(searchBox.getX() + searchBox.getWidth() / 2, searchBox.getY() + searchBox.getHeight() / 2), searchBox.getHeight() + 2));

        return points;
    }
}
