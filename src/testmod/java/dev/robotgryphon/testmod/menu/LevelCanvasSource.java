package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import dev.robotgryphon.screenlib.menu.CanvasSource;
import dev.robotgryphon.testmod.TestModMenus;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;

public record LevelCanvasSource(Level level) implements CanvasSource {
    @Override
    public CanvasState load() {
        return this.level.getData(TestModMenus.CANVAS_ATTACHMENT);
    }

    @Override
    public void save(CanvasState state) {
        this.level.setData(TestModMenus.CANVAS_ATTACHMENT, state);
    }

    @Override
    public MenuProvider menu() {
        return new TestScreenMenuProvider(this);
    }
}
