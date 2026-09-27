//? if forge {
/*package dev.isxander.controlify.platform.client.forge;

import dev.isxander.controlify.platform.client.CreativeTabHelper;
import dev.isxander.controlify.platform.forge.mixins.CreativeModeInventoryScreenAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.List;

public class ForgeCreativeTabHelper implements CreativeTabHelper {
    private final CreativeModeInventoryScreen screen;

    public ForgeCreativeTabHelper(CreativeModeInventoryScreen screen) {
        this.screen = screen;
    }

    @Override
    public void setCurrentPage(int page) {
    }

    @Override
    public int getCurrentPage() {
        return 0;
    }

    @Override
    public int getPageCount() {
        return 1;
    }

    @Override
    public List<CreativeModeTab> getTabsForPage(int page) {
        return CreativeModeTabs.tabs();
    }

    @Override
    public CreativeModeTab getSelectedTab() {
        return CreativeModeInventoryScreenAccessor.getSelectedTab();
    }

    @Override
    public void setSelectedTab(CreativeModeTab tab) {
        ((CreativeModeInventoryScreenAccessor) screen).invokeSelectTab(tab);
    }
}
*///?}
