package snownee.everpotion.client.gui;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.texture.PotionSpriteUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.client.settings.KeyModifier;
import snownee.everpotion.EverCommonConfig;
import snownee.everpotion.cap.EverCapabilities;
import snownee.everpotion.client.ClientHandler;
import snownee.everpotion.handler.EverHandler;
import snownee.everpotion.handler.EverHandler.Cache;

public class UseScreen extends Screen {

    private static final ITextComponent TITLE = new TranslationTextComponent("gui.everpotion.use.title");
    private EverHandler handler;
    private final float[] scales = new float[4];
    private final String[] names = new String[4];
    private boolean closing;
    private float openTick;
    private int clickIndex = -1;
    private float drinkTick;

    private KeyBinding[] keyBindsHotbar;

    public UseScreen() {
        super(TITLE);

    }

    @Override
    protected void init() {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        handler = player.getCapability(EverCapabilities.HANDLER).orElse(null);
        keyBindsHotbar = minecraft.gameSettings.keyBindsHotbar;
    }

    @Override
    public void render(int mouseX, int mouseY, float pTicks) {
        if (handler == null) {
            return;
        }

        float aTicks = Animation.getWorldTime(minecraft.world, pTicks) * 0.005f;

        openTick += closing ? -aTicks * .4f : aTicks * .2f;
        if (closing && openTick <= 0) {
            Minecraft.getInstance().displayGuiScreen(null);
            return;
        }
        openTick = MathHelper.clamp(openTick, 0, 1);

        float xCenter = width / 2f;
        float yCenter = height / 2f;
        if (EverCommonConfig.maxSlots == 3) {
            yCenter += 20;
        }

        if (!closing && (drinkTick > 0 || handler.drinkIndex != -1)) {
            drinkTick += aTicks;
            if (drinkTick > EverCommonConfig.drinkDelay) {
                onClose();
            }
        }

        float offset = 35 + openTick * 25;
        Matrix4f matrix = TransformationMatrix.identity().getMatrix();
        if (EverCommonConfig.maxSlots == 1) {
            drawButton(matrix, xCenter, yCenter, mouseX, mouseY, 0, aTicks);
        } else if (EverCommonConfig.maxSlots == 2) {
            drawButton(matrix, xCenter - offset, yCenter, mouseX, mouseY, 0, aTicks);
            drawButton(matrix, xCenter + offset, yCenter, mouseX, mouseY, 1, aTicks);
        } else if (EverCommonConfig.maxSlots == 3) {
            drawButton(matrix, xCenter - offset, yCenter, mouseX, mouseY, 0, aTicks);
            drawButton(matrix, xCenter, yCenter - offset, mouseX, mouseY, 1, aTicks);
            drawButton(matrix, xCenter + offset, yCenter, mouseX, mouseY, 2, aTicks);
        } else if (EverCommonConfig.maxSlots == 4) {
            drawButton(matrix, xCenter - offset, yCenter, mouseX, mouseY, 0, aTicks);
            drawButton(matrix, xCenter, yCenter - offset, mouseX, mouseY, 1, aTicks);
            drawButton(matrix, xCenter + offset, yCenter, mouseX, mouseY, 2, aTicks);
            drawButton(matrix, xCenter, yCenter + offset, mouseX, mouseY, 3, aTicks);
        }
        if (clickIndex < 0) {
            int range = EverCommonConfig.maxSlots == 1 ? 60 : 120;
            boolean out = Math.abs(mouseX - xCenter) + Math.abs(mouseY - yCenter) > range;
            clickIndex = out ? -2 : -1;
        }

        RenderSystem.disableBlend();

        super.render(mouseX, mouseY, pTicks);
    }

    @SuppressWarnings("null")
    private void drawButton(Matrix4f matrix, float xCenter, float yCenter, int mouseX, int mouseY, int index, float pTicks) {
        Cache cache = handler.caches[index];
        float a = .5F * openTick;

        float hd = 40;
        boolean hover = !closing && cache != null && openTick == 1 && Math.abs(mouseX - xCenter) + Math.abs(mouseY - yCenter) < hd + 10;
        hover = hover && (handler.drinkIndex == index || handler.drinkIndex == -1);
        if (hover) {
            clickIndex = index;
        } else if (clickIndex == index) {
            clickIndex = -1;
        }

        scales[index] += (hover ? pTicks : -pTicks) * 0.5f;
        scales[index] = MathHelper.clamp(scales[index], 0, 1);
        hd += scales[index] * 5;

        float r, g, b;
        if (hover) {
            int color = cache.color;
            r = Math.max(.1F, (color >> 16 & 255) / 255.0F * .2f * scales[index]);
            g = Math.max(.1F, (color >> 8 & 255) / 255.0F * .2f * scales[index]);
            b = Math.max(.1F, (color & 255) / 255.0F * .2f * scales[index]);
        } else {
            r = .1F;
            g = .1F;
            b = .1F;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter - hd, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter + hd, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter + hd, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter - hd, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();

        a = .75f * openTick;
        if (hover) {
            int color = cache.color;
            r = Math.max(.1F, (color >> 16 & 255) / 255.0F * scales[index]);
            g = Math.max(.1F, (color >> 8 & 255) / 255.0F * scales[index]);
            b = Math.max(.1F, (color & 255) / 255.0F * scales[index]);
        }

        float hdborder = hd + 3;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter - hdborder, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter + hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter + hd, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter - hd, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter + hdborder, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter + hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter + hd, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter + hd, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter + hdborder, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter - hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter - hd, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter + hd, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter - hdborder, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter - hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter - hd, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter - hd, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();

        if (handler.drinkIndex == index) {
            float h = hd * drinkTick * 2 / EverCommonConfig.drinkDelay - hd;
            float ia = .2f;
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(matrix, xCenter + hd - Math.abs(h), yCenter - h, 0.0F).color(1, 1, 1, ia).endVertex();
            if (h > 0) {
                buffer.pos(matrix, xCenter + hd, yCenter, 0.0F).color(1, 1, 1, ia).endVertex();
            }
            buffer.pos(matrix, xCenter, yCenter + hd, 0.0F).color(1, 1, 1, ia).endVertex();
            if (h > 0) {
                buffer.pos(matrix, xCenter - hd, yCenter, 0.0F).color(1, 1, 1, ia).endVertex();
            }
            buffer.pos(matrix, xCenter - hd + Math.abs(h), yCenter - h, 0.0F).color(1, 1, 1, ia).endVertex();
            tessellator.draw();
        }

        float hdshadow;
        if (hover) {
            a = .3f * openTick;
            hdshadow = hdborder + 6;
        } else {
            a = .2f * openTick;
            hdshadow = hdborder + 5;
        }
        r = .1F;
        g = .1F;
        b = .1F;

        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter - hdshadow, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter + hdshadow, yCenter, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter + hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter - hdborder, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter + hdshadow, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter + hdshadow, yCenter, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter + hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter + hdborder, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter + hdshadow, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter - hdshadow, yCenter, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter - hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter + hdborder, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, xCenter, yCenter - hdshadow, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter - hdshadow, yCenter, 0.0F).color(r, g, b, 0).endVertex();
        buffer.pos(matrix, xCenter - hdborder, yCenter, 0.0F).color(r, g, b, a).endVertex();
        buffer.pos(matrix, xCenter, yCenter - hdborder, 0.0F).color(r, g, b, a).endVertex();
        tessellator.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();

        refreshName(index);
        RenderSystem.pushMatrix();
        RenderSystem.color4f(1, 1, 1, openTick);
        int textAlpha = (int) (openTick * 255);
        int textColor = textAlpha << 24 | 0xffffff;

        String name = names[index];
        if (cache != null && cache.progress < EverCommonConfig.refillTime) {
            float percent = 100 * cache.progress / EverCommonConfig.refillTime;
            name = (int) percent + "%";
        }

        if (cache == null) {
            RenderSystem.translatef(xCenter, yCenter - 3, 0);
            RenderSystem.scalef(0.75f, 0.75f, 0.75f);
            drawCenteredString(font, name, 0, 0, textColor);
        } else if (cache.effect != null) {
            PotionSpriteUploader potionspriteuploader = this.minecraft.getPotionSpriteUploader();
            TextureAtlasSprite sprite = potionspriteuploader.getSprite(cache.effect.getPotion());
            sprite.getAtlasTexture().bindTexture();

            blit((int) xCenter - 12, (int) yCenter - 18, this.getBlitOffset(), 24, 24, sprite);

            float yCenter2 = yCenter - 6 * (1 + 0.125f * scales[index]);
            float halfwidth = 12 + 1.5f * scales[index];
            float left = xCenter - halfwidth;
            float right = xCenter + halfwidth;
            float top = yCenter2 - halfwidth;
            float bottom = yCenter2 + halfwidth;

            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(left, bottom, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
            buffer.pos(right, bottom, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(right, top, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(left, top, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            tessellator.draw();

            RenderSystem.translatef(xCenter, yCenter + 10, 0);
            RenderSystem.scalef(0.75f, 0.75f, 0.75f);
            drawCenteredString(font, name, 0, 0, textColor);
        } else {
            RenderSystem.translatef(xCenter, yCenter - 3, 0);
            RenderSystem.scalef(0.75f, 0.75f, 0.75f);
            drawCenteredString(font, name, 0, 0, textColor);
        }
        RenderSystem.popMatrix();
    }

    private void refreshName(int i) {
        if (names[i] != null) {
            return;
        }
        Cache cache = handler.caches[i];
        if (cache == null) {
            if (i < handler.getSlots()) {
                names[i] = "";
            } else {
                names[i] = TextFormatting.GRAY + "Locked";
            }
        } else if (cache.effect == null) {
            names[i] = I18n.format("effect.none");
        } else {
            names[i] = I18n.format(cache.effect.getEffectName());
            if (cache.effect.getAmplifier() > 0) {
                names[i] += " " + I18n.format("potion.potency." + cache.effect.getAmplifier());
            }
        }
    }

    @Override
    public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
        if (clickIndex == -2) {
            onClose();
            return true;
        }
        if (closing || clickIndex == -1) {
            return false;
        }
        if (!handler.canDrink(clickIndex)) {
            return false;
        }
        scales[clickIndex] = 0.25f;
        handler.startDrinking(clickIndex);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (closing || handler.drinkIndex != -1) {
            return false;
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.canDrink(i) && keyBindsHotbar[i].getKey().getKeyCode() == key) {
                scales[i] = 1;
                handler.startDrinking(i);
                return true;
            }
        }
        if (ClientHandler.kbUse.getKeyModifier().isActive(null) && ClientHandler.kbUse.getKey().getKeyCode() == key) {
            if (ClientHandler.kbUse.getKeyModifier() == KeyModifier.NONE && modifiers != 0) {
                return false;
            }
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        closing = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
