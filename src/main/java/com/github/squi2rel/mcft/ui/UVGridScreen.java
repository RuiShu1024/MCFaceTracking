package com.github.squi2rel.mcft.ui;

import com.github.squi2rel.mcft.tracking.TrackingRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

import static com.github.squi2rel.mcft.FTModel.model;

public class UVGridScreen extends GridScreen {
    public static Selection eyeL, eyeR, mouth, lid, inner;

    public UVGridScreen() {
        super(Component.translatable("mcft.gui.edittexture"), 32, 128);
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 100;
        int btnHeight = 20;
        int buttons = 10;
        int totalHeight = buttons * btnHeight + (buttons - 1) * 2;
        int y = (this.height - totalHeight) / 2;
        WidgetGroup group3D = new WidgetGroup();
        WidgetGroup groupFlat = new WidgetGroup();
        addRenderableWidget(Button.builder(Component.translatable(model.isFlat ? "mcft.gui.button.3dmodel" : "mcft.gui.button.flatmodel"), b -> {
            model.isFlat = !model.isFlat;
            b.setMessage(Component.translatable(model.isFlat ? "mcft.gui.button.3dmodel" : "mcft.gui.button.flatmodel"));
            groupFlat.visible(model.isFlat);
            group3D.visible(!model.isFlat);
        }).bounds(20, y, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.mark.eyewhite"), b -> inner = getSelection()).bounds(20, y + btnHeight + 2, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.mark.lid"), b -> lid = getSelection()).bounds(20, y + (btnHeight + 2) * 2, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.mark.lpupil"), b -> eyeL = getSelection()).bounds(20, y + (btnHeight + 2) * 3, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.mark.rpupil"), b -> eyeR = getSelection()).bounds(20, y + (btnHeight + 2) * 4, btnWidth, btnHeight).build());
        group3D.add(Button.builder(Component.translatable("mcft.gui.button.mark.mouth"), b -> mouth = getSelection()).bounds(20, y + (btnHeight + 2) * 5, btnWidth, btnHeight).build());
        groupFlat.add(Button.builder(Component.translatable("mcft.gui.button.mark.eyebrow"), b -> mouth = getSelection()).bounds(20, y + (btnHeight + 2) * 5, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.next"), b -> Minecraft.getInstance().setScreen(new AvatarGridScreen())).bounds(20, y + (btnHeight + 2) * 6, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.reset"), b -> inner = lid = eyeL = eyeR = mouth = null).bounds(20, y + (btnHeight + 2) * 7, btnWidth, btnHeight).build());
        addRenderableWidget(Button.builder(Component.translatable("mcft.gui.button.close"), b -> Minecraft.getInstance().setScreen(null)).bounds(20, y + (btnHeight + 2) * 8, btnWidth, btnHeight).build());
        groupFlat.visible(model.isFlat);
        group3D.visible(!model.isFlat);
        gridX = (width * 3 / 2 - drawSize) / 2;
        gridY = (height - drawSize) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        Identifier skin = Objects.requireNonNull(Minecraft.getInstance().player).getSkin().body().texturePath();

        context.blit(RenderPipelines.GUI_TEXTURED, skin, gridX, gridY, 0, 0, drawSize, drawSize / 2, 32, 16, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, skin, gridX, gridY + drawSize / 2, 32, 0, drawSize, drawSize / 2, 32, 16, 64, 64);

        drawGrid(context, gridX, gridY);

        drawSelection(context, inner, 0x5500FF00);
        drawSelection(context, lid, 0x550000FF);
        drawSelection(context, eyeR, 0x5500FFFF);
        drawSelection(context, eyeL, 0x55FFFF00);
        drawSelection(context, mouth, 0x55FF00FF);
    }

    public static void applyUV(Selection s, TrackingRect rect) {
        float drawSize = 128;
        if (s == null) return;
        if (s.y() > 63) {
            rect.uv((s.x() + drawSize) / drawSize / 2, (s.y() - drawSize / 2) / drawSize / 2, (s.x() + s.w() + drawSize) / drawSize / 2, (s.y() + s.h() - drawSize / 2) / drawSize / 2);
        } else {
            rect.uv((float) s.x() / drawSize / 2, (float) s.y() / drawSize / 2, (float) (s.x() + s.w()) / drawSize / 2, (float) (s.y() + s.h()) / drawSize / 2);
        }
    }
}
