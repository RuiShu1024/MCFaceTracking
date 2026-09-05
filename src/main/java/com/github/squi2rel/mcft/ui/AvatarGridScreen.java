package com.github.squi2rel.mcft.ui;

import com.github.squi2rel.mcft.*;
import com.github.squi2rel.mcft.tracking.EyeTrackingRect;
import com.github.squi2rel.mcft.tracking.MouthTrackingRect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.github.squi2rel.mcft.FTModel.model;
import static com.github.squi2rel.mcft.MCFTClient.config;

@SuppressWarnings("DataFlowIssue")
public class AvatarGridScreen extends GridScreen {
    private boolean showOverlay = true;
    private boolean preview = false, blinking = false;
    private static Selection eyeL, eyeR, mouth;
    private SettingsSlider<Float> eyeW, eyeH, eyeX, eyeY, brow;
    private SettingsSlider<Float> eyeOffsetXL, eyeOffsetYL, eyeOffsetXR, eyeOffsetYR;
    private SettingsSlider<Float> blinkInterval, blinkIntervalFix, blinkDuration, blinkDurationFix, blinkMaxY;

    public AvatarGridScreen() {
        super(Component.translatable("mcft.gui.editselection"), 8, 128);
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 100;
        int btnHeight = 20;
        int buttons = 10;
        int totalHeight = buttons * btnHeight + (buttons - 1) * 2;
        int y = (this.height - totalHeight) / 2;
        WidgetGroup defaultGroup = new WidgetGroup();
        WidgetGroup markGroup = new WidgetGroup();
        WidgetGroup previewGroup = new WidgetGroup();
        WidgetGroup blinkGroup = new WidgetGroup();
        markGroup.add(Button.builder(Component.translatable("mcft.gui.button.toggleoverlay"), b -> showOverlay = !showOverlay).bounds(20, y, btnWidth, btnHeight).build());
        markGroup.add(Button.builder(Component.translatable("mcft.gui.button.freeselection"), b -> {
            freeDrag = !freeDrag;
            b.setMessage(Component.translatable(freeDrag ? "mcft.gui.button.alignedselection" : "mcft.gui.button.freeselection"));
        }).bounds(20, y + btnHeight + 2, btnWidth, btnHeight).build());
        markGroup.add(Button.builder(Component.translatable("mcft.gui.button.markas.leye"), b -> eyeL = getSelection()).bounds(20, y + (btnHeight + 2) * 2, btnWidth, btnHeight).build());
        markGroup.add(Button.builder(Component.translatable("mcft.gui.button.markas.reye"), b -> eyeR = getSelection()).bounds(20, y + (btnHeight + 2) * 3, btnWidth, btnHeight).build());
        if (!model.isFlat) markGroup.add(Button.builder(Component.translatable("mcft.gui.button.markas.mouth"), b -> mouth = getSelection()).bounds(20, y + (btnHeight + 2) * 4, btnWidth, btnHeight).build());
        defaultGroup.add(Button.builder(Component.translatable("mcft.gui.button.preview"), b -> {
            markGroup.visible(preview);
            preview = !preview;
            if (preview) save();
            previewGroup.visible(preview);
        }).bounds(20, y + (btnHeight + 2) * 5, btnWidth, btnHeight).build());
        defaultGroup.add(Button.builder(Component.translatable("mcft.gui.button.previous"), b -> Minecraft.getInstance().setScreen(new UVGridScreen())).bounds(20, y + (btnHeight + 2) * 6, btnWidth, btnHeight).build());
        markGroup.add(Button.builder(Component.translatable("mcft.gui.button.reset"), b -> {
            eyeL = eyeR = mouth = null;
            Minecraft.getInstance().setScreen(new AvatarGridScreen());
        }).bounds(20, y + (btnHeight + 2) * 7, btnWidth, btnHeight).build());
        previewGroup.add(Button.builder(Component.translatable("mcft.gui.button.reset"), b -> {
            eyeW.setValue(0.75f);
            eyeH.setValue(0.75f);
            eyeX.setValue(0.5f);
            eyeY.setValue(0.3f);
            eyeOffsetXL.setValue(0f);
            eyeOffsetYL.setValue(0f);
            eyeOffsetXR.setValue(0f);
            eyeOffsetYR.setValue(0f);
            if (brow != null) brow.setValue(0f);
        }).bounds(20, y + (btnHeight + 2) * 7, btnWidth, btnHeight).build());
        defaultGroup.add(Button.builder(Component.translatable("mcft.gui.button.done"), b -> {
            save();
            writeConfig();
            Minecraft.getInstance().setScreen(null);
        }).bounds(20, y + (btnHeight + 2) * 8, btnWidth, btnHeight).build());
        previewGroup.add(Button.builder(Component.translatable("mcft.gui.button.autoblink"), b -> {
            defaultGroup.visible(false);
            previewGroup.visible(false);
            blinkGroup.visible(true);
            blinking = true;
        }).bounds(20, y + (btnHeight + 2) * 9, btnWidth, btnHeight).build());
        eyeW = previewGroup.add(SettingsSlider.floatSlider(20, y, btnWidth, btnHeight, model.eyeR.ball.w, 0.25f, 4f, f -> {
            model.eyeR.ball.w(f);
            model.eyeL.ball.w(f);
        }, f -> I18n.get("mcft.gui.slider.eyeballwidth", NUMBER_FORMAT.format(f))));
        eyeH = previewGroup.add(SettingsSlider.floatSlider(20, y + btnHeight + 2, btnWidth, btnHeight, model.eyeR.ball.h, 0.25f, 4f, f -> {
            model.eyeR.ball.h(f);
            model.eyeL.ball.h(f);
        }, f -> I18n.get("mcft.gui.slider.eyeballheight", NUMBER_FORMAT.format(f))));
        eyeOffsetXL = previewGroup.add(SettingsSlider.floatSlider(20 + btnWidth + 2, y, btnWidth, btnHeight, config.eyeOffsetXL, -2f, 2f, f -> {
            config.eyeOffsetXL = f;
            if (!model.active() || AutoBlink.enabled) model.eyeL.rawPos.x = f;
        }, f -> I18n.get("mcft.gui.slider.lefteyeballoffsetx", NUMBER_FORMAT.format(f))));
        eyeOffsetYL = previewGroup.add(SettingsSlider.floatSlider(20 + btnWidth + 2, y + btnHeight + 2, btnWidth, btnHeight, config.eyeOffsetYL, -2f, 2f, f -> {
            config.eyeOffsetYL = f;
            if (!model.active() || AutoBlink.enabled) model.eyeL.rawPos.y = f;
        }, f -> I18n.get("mcft.gui.slider.lefteyeballoffsety", NUMBER_FORMAT.format(f))));
        eyeOffsetXR = previewGroup.add(SettingsSlider.floatSlider(20 + btnWidth + 2, y + (btnHeight + 2) * 2, btnWidth, btnHeight, config.eyeOffsetXR, -2f, 2f, f -> {
            config.eyeOffsetXR = f;
            if (!model.active() || AutoBlink.enabled) model.eyeR.rawPos.x = f;
        }, f -> I18n.get("mcft.gui.slider.righteyeballoffsetx", NUMBER_FORMAT.format(f))));
        eyeOffsetYR = previewGroup.add(SettingsSlider.floatSlider(20 + btnWidth + 2, y + (btnHeight + 2) * 3, btnWidth, btnHeight, config.eyeOffsetYR, -2f, 2f, f -> {
            config.eyeOffsetYR = f;
            if (!model.active() || AutoBlink.enabled) model.eyeR.rawPos.y = f;
        }, f -> I18n.get("mcft.gui.slider.righteyeballoffsety", NUMBER_FORMAT.format(f))));
        brow = null;
        if (model.isFlat) brow = previewGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 2, btnWidth, btnHeight, model.mouth.h, -3f, 3f, f -> model.mouth.h(f), f -> I18n.get("mcft.gui.slider.eyebrowheight", NUMBER_FORMAT.format(f))));
        eyeX = previewGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 3, btnWidth, btnHeight, config.eyeXMul, 0.1f, 2f, f -> config.eyeXMul = f, f -> I18n.get("mcft.gui.slider.eyeballmovemulx", NUMBER_FORMAT.format(f))));
        eyeY = previewGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 4, btnWidth, btnHeight, config.eyeYMul, 0.1f, 2f, f -> config.eyeYMul = f, f -> I18n.get("mcft.gui.slider.eyeballmovemuly", NUMBER_FORMAT.format(f))));
        blinkGroup.add(Button.builder(Component.translatable(config.autoBlink ? "mcft.gui.button.autoblink.off" : "mcft.gui.button.autoblink.on"), b -> {
            config.autoBlink = !config.autoBlink;
            b.setMessage(Component.translatable(config.autoBlink ? "mcft.gui.button.autoblink.off" : "mcft.gui.button.autoblink.on"));
        }).bounds(20, y, btnWidth, btnHeight).build());
        blinkGroup.add(Button.builder(Component.translatable(config.autoSwitchBlink ? "mcft.gui.button.autoswitch.off" : "mcft.gui.button.autoswitch.on"), b -> {
            config.autoSwitchBlink = !config.autoSwitchBlink;
            b.setMessage(Component.translatable(config.autoSwitchBlink ? "mcft.gui.button.autoswitch.off" : "mcft.gui.button.autoswitch.on"));
        }).bounds(20, y + btnHeight + 2, btnWidth, btnHeight).build());
        blinkInterval = blinkGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 2, btnWidth, btnHeight, config.blinkInterval, 1f, 10f, f -> config.blinkInterval = f, f -> I18n.get("mcft.gui.slider.blinkinterval", NUMBER_FORMAT.format(f))));
        blinkIntervalFix = blinkGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 3, btnWidth, btnHeight, config.blinkIntervalFix, 0f, 10f, f -> config.blinkIntervalFix = f, f -> I18n.get("mcft.gui.slider.blinkintervalfix", NUMBER_FORMAT.format(f))));
        blinkDuration = blinkGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 4, btnWidth, btnHeight, config.blinkDuration, 0.01f, 0.5f, f -> config.blinkDuration = f, f -> I18n.get("mcft.gui.slider.blinkduration", NUMBER_FORMAT.format(f))));
        blinkDurationFix = blinkGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 5, btnWidth, btnHeight, config.blinkDurationFix, 0f, 0.5f, f -> config.blinkDurationFix = f, f -> I18n.get("mcft.gui.slider.blinkdurationfix", NUMBER_FORMAT.format(f))));
        blinkMaxY = blinkGroup.add(SettingsSlider.floatSlider(20, y + (btnHeight + 2) * 6, btnWidth, btnHeight, config.blinkMaxY, 0f, 1f, f -> config.blinkMaxY = f, f -> I18n.get("mcft.gui.slider.lidopenness", "%.0f".formatted(f * 100))));
        blinkGroup.add(Button.builder(Component.translatable("mcft.gui.button.reset"), b -> {
            blinkInterval.setValue(5f);
            blinkIntervalFix.setValue(7.5f);
            blinkDuration.setValue(0.1f);
            blinkDurationFix.setValue(0.25f);
            blinkMaxY.setValue(0.8f);
        }).bounds(20, y + (btnHeight + 2) * 7, btnWidth, btnHeight).build());
        blinkGroup.add(Button.builder(Component.translatable("mcft.gui.button.back"), b -> {
            defaultGroup.visible(true);
            previewGroup.visible(true);
            blinkGroup.visible(false);
            blinking = false;
        }).bounds(20, y + (btnHeight + 2) * 8, btnWidth, btnHeight).build());
        defaultGroup.visible(!blinking);
        markGroup.visible(!preview && !blinking);
        previewGroup.visible(preview && !blinking);
        blinkGroup.visible(blinking);
        gridX = (width * 3 / 2 - drawSize) / 2;
        gridY = (height - drawSize) / 2;
    }

    private void writeConfig() {
        Config config = MCFTClient.config;
        config.model = model;
        MCFT.saveConfig(config, MCFTClient.configPath);
    }

    private void save() {
        float d = (float) drawSize / gridLength;
        if (eyeR != null) {
            model.eyeR = new EyeTrackingRect(eyeR.x() / d, (eyeR.y() + eyeR.h()) / d, eyeR.w() / d, eyeR.h() / d);
            UVGridScreen.applyUV(UVGridScreen.eyeR, model.eyeR.ball);
            UVGridScreen.applyUV(UVGridScreen.lid, model.eyeR.lid);
            UVGridScreen.applyUV(UVGridScreen.inner, model.eyeR.inner);
        }
        if (eyeL != null) {
            model.eyeL = new EyeTrackingRect(eyeL.x() / d, (eyeL.y() + eyeL.h()) / d, eyeL.w() / d, eyeL.h() / d);
            UVGridScreen.applyUV(UVGridScreen.eyeL, model.eyeL.ball);
            UVGridScreen.applyUV(UVGridScreen.lid, model.eyeL.lid);
            UVGridScreen.applyUV(UVGridScreen.inner, model.eyeL.inner);
        }
        eyeW.applyValue();
        eyeH.applyValue();
        eyeOffsetXL.applyValue();
        eyeOffsetYL.applyValue();
        eyeOffsetXR.applyValue();
        eyeOffsetYR.applyValue();
        if (mouth != null && !model.isFlat) {
            model.mouth = new MouthTrackingRect(mouth.x() / d, (mouth.y() + mouth.h()) / d, mouth.w() / d, mouth.h() / d);
            UVGridScreen.applyUV(UVGridScreen.mouth, model.mouth);
        } else if (model.isFlat) {
            UVGridScreen.applyUV(UVGridScreen.mouth, model.mouth);
        }
        Minecraft.getInstance().execute(() -> FTClient.uploadParams(model));
    }

    @Override
    protected void drawGrid(GuiGraphicsExtractor context, int x, int y) {
        super.drawGrid(context, x, y);

        drawSelection(context, eyeR, 0x5500FFFF);
        drawSelection(context, eyeL, 0x55FFFF00);
        if (!model.isFlat) drawSelection(context, mouth, 0x55FF00FF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (preview) {
            renderModel(context);
        } else {
            renderHead(context);
        }
    }

    private void renderModel(GuiGraphicsExtractor context) {
        InventoryScreen.extractEntityInInventoryFollowsMouse(context, gridX, gridY, gridX + drawSize, gridY + drawSize, 200, 0.8f, gridX + drawSize / 2f, gridY + drawSize / 2f, Minecraft.getInstance().player);
    }

    private void renderHead(GuiGraphicsExtractor context) {
        Identifier skin = Minecraft.getInstance().player.getSkin().body().texturePath();

        context.blit(RenderPipelines.GUI_TEXTURED, skin, gridX, gridY, 8, 8, drawSize, drawSize, 8, 8, 64, 64);
        if (showOverlay) context.blit(RenderPipelines.GUI_TEXTURED, skin, gridX - 8, gridY - 8, 40, 8, drawSize + 16, drawSize + 16, 8, 8, 64, 64);

        drawGrid(context, gridX, gridY);
    }
}
