package de.ellpeck.naturesaura.events;

import com.mojang.blaze3d.platform.GlStateManager;
import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.ModConfig;
import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.api.aura.container.IAuraContainer;
import de.ellpeck.naturesaura.api.aura.type.IAuraType;
import de.ellpeck.naturesaura.api.render.IVisualizable;
import de.ellpeck.naturesaura.blocks.tiles.TileEntityGratedChute;
import de.ellpeck.naturesaura.blocks.tiles.TileEntityNatureAltar;
import de.ellpeck.naturesaura.blocks.tiles.TileEntityRFConverter;
import de.ellpeck.naturesaura.compat.Compat;
import de.ellpeck.naturesaura.items.AuraCache;
import de.ellpeck.naturesaura.items.ModItems;
import de.ellpeck.naturesaura.items.RangeVisualizer;
import de.ellpeck.naturesaura.particles.ParticleHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IGrowable;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    public static final ResourceLocation OVERLAYS = new ResourceLocation(NaturesAura.MOD_ID, "textures/gui/overlays.png");
    public static final ResourceLocation BOOK_GUI = new ResourceLocation(NaturesAura.MOD_ID, "textures/gui/book.png");
    private static final ItemStack ITEM_FRAME = new ItemStack(Items.ITEM_FRAME);
    private static final Map<ResourceLocation, Tuple<ItemStack, Boolean>> SHOWING_EFFECTS = new HashMap<>();
    private static ItemStack heldCache = ItemStack.EMPTY;
    private static ItemStack heldEye = ItemStack.EMPTY;
    private static ItemStack heldOcular = ItemStack.EMPTY;

    @SubscribeEvent
    public void onDebugRender(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameSettings.showDebugInfo && ModConfig.client.debugText) {
            String prefix = TextFormatting.GREEN + "[" + NaturesAura.MOD_NAME + "]" + TextFormatting.RESET + " ";
            List<String> left = event.getLeft();
            left.add("");

            int depth = ParticleHandler.getParticleAmount(true);
            int noDepth = ParticleHandler.getParticleAmount(false);
            left.add(prefix + "P: " + (depth + noDepth) + " (D: " + depth + " nD: " + noDepth + ")");

            if (mc.player.isCreative()) {
                MutableInt amount = new MutableInt(IAuraChunk.DEFAULT_AURA);
                MutableInt spots = new MutableInt();
                IAuraChunk.getSpotsInArea(mc.world, mc.player.getPosition(), 35, (blockPos, drainSpot) -> {
                    spots.increment();
                    amount.add(drainSpot);
                });
                NumberFormat format = NumberFormat.getInstance();
                left.add(prefix + "A: " + format.format(amount.intValue()) + " (S: " + spots.intValue() + ")");
                left.add(prefix + "AT: " + IAuraType.forWorld(mc.world).getName());
            }
        }
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ParticleHandler.renderParticles(event.getPartialTicks());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            heldCache = ItemStack.EMPTY;
            heldEye = ItemStack.EMPTY;
            heldOcular = ItemStack.EMPTY;

            Minecraft mc = Minecraft.getInstance();
            if (mc.world == null) {
                ParticleHandler.clearParticles();
                RangeVisualizer.clear();
            } else if (!mc.isGamePaused()) {
                if (mc.world.getGameTime() % 20 == 0) {
                    int amount = MathHelper.floor(190 * ModConfig.client.excessParticleAmount);
                    for (int i = 0; i < amount; i++) {
                        int x = MathHelper.floor(mc.player.posX) + mc.world.rand.nextInt(64) - 32;
                        int z = MathHelper.floor(mc.player.posZ) + mc.world.rand.nextInt(64) - 32;
                        BlockPos pos = new BlockPos(x, mc.world.getHeight(Heightmap.Type.WORLD_SURFACE, x, z) - 1, z);
                        BlockState state = mc.world.getBlockState(pos);
                        Block block = state.getBlock();
                        if (block instanceof IGrowable || block instanceof IPlantable || block instanceof LeavesBlock) {
                            int excess = IAuraChunk.triangulateAuraInArea(mc.world, pos, 45) - IAuraChunk.DEFAULT_AURA;
                            if (excess > 0) {
                                int chance = Math.max(10, 50 - excess / 25000);
                                if (mc.world.rand.nextInt(chance) <= 0)
                                    NaturesAuraAPI.instance().spawnMagicParticle(
                                            pos.getX() + mc.world.rand.nextFloat(),
                                            pos.getY() + 0.5F,
                                            pos.getZ() + mc.world.rand.nextFloat(),
                                            mc.world.rand.nextGaussian() * 0.01F,
                                            mc.world.rand.nextFloat() * 0.025F,
                                            mc.world.rand.nextGaussian() * 0.01F,
                                            BiomeColors.getFoliageColor(mc.world, pos),
                                            Math.min(2F, 1F + mc.world.rand.nextFloat() * (excess / 30000F)),
                                            Math.min(300, 100 + mc.world.rand.nextInt(excess / 3000 + 1)),
                                            0F, false, true);
                            }
                        }
                    }
                }

                if (Helper.isHoldingItem(mc.player, ModItems.RANGE_VISUALIZER) && mc.world.getGameTime() % 5 == 0) {
                    NaturesAuraAPI.IInternalHooks inst = NaturesAuraAPI.instance();
                    inst.setParticleSpawnRange(512);
                    inst.setParticleDepth(false);
                    for (BlockPos pos : RangeVisualizer.VISUALIZED_RAILS.get(mc.world.getDimension().getType())) {
                        NaturesAuraAPI.instance().spawnMagicParticle(
                                pos.getX() + mc.world.rand.nextFloat(),
                                pos.getY() + mc.world.rand.nextFloat(),
                                pos.getZ() + mc.world.rand.nextFloat(),
                                0F, 0F, 0F, 0xe0faff, mc.world.rand.nextFloat() * 5 + 1, 100, 0F, false, true);
                    }
                    inst.setParticleDepth(true);
                    inst.setParticleSpawnRange(32);
                }

                ParticleHandler.updateParticles();

                if (Compat.baubles) {
                    // TODO baubles
                    /*IItemHandler baubles = BaublesApi.getBaublesHandler(mc.player);
                    for (int i = 0; i < baubles.getSlots(); i++) {
                        ItemStack slot = baubles.getStackInSlot(i);
                        if (!slot.isEmpty()) {
                            if (slot.getItem() instanceof AuraCache)
                                heldCache = slot;
                            else if (slot.getItem() == ModItems.EYE)
                                heldEye = slot;
                            else if (slot.getItem() == ModItems.EYE_IMPROVED)
                                heldOcular = slot;
                        }
                    }*/
                }

                for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
                    ItemStack slot = mc.player.inventory.getStackInSlot(i);
                    if (!slot.isEmpty()) {
                        if (slot.getItem() instanceof AuraCache)
                            heldCache = slot;
                        else if (slot.getItem() == ModItems.EYE && i <= 8)
                            heldEye = slot;
                        else if (slot.getItem() == ModItems.EYE_IMPROVED)
                            heldOcular = slot;
                    }
                }

                if (!heldOcular.isEmpty() && mc.world.getGameTime() % 20 == 0) {
                    SHOWING_EFFECTS.clear();
                    Helper.getAuraChunksInArea(mc.world, mc.player.getPosition(), 100,
                            chunk -> chunk.getActiveEffectIcons(mc.player, SHOWING_EFFECTS));
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        GL11.glPushMatrix();
        float partial = event.getPartialTicks();
        GL11.glTranslated(
                -mc.player.prevPosX - (mc.player.posX - mc.player.prevPosX) * partial,
                -mc.player.prevPosY - (mc.player.posY - mc.player.prevPosY) * partial,
                -mc.player.prevPosZ - (mc.player.posZ - mc.player.prevPosZ) * partial);

        if (mc.gameSettings.showDebugInfo && mc.player.isCreative() && ModConfig.client.debugWorld) {
            Map<BlockPos, Integer> spots = new HashMap<>();
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBegin(GL11.GL_QUADS);
            IAuraChunk.getSpotsInArea(mc.world, mc.player.getPosition(), 64, (pos, spot) -> {
                spots.put(pos, spot);

                GlStateManager.color4f(spot > 0 ? 0F : 1F, spot > 0 ? 1F : 0F, 0F, 0.35F);
                Helper.renderWeirdBox(pos.getX(), pos.getY(), pos.getZ(), 1, 1, 1);
            });
            GL11.glEnd();
            GL11.glPopAttrib();

            float scale = 0.03F;
            NumberFormat format = NumberFormat.getInstance();
            GlStateManager.scalef(scale, scale, scale);
            for (Map.Entry<BlockPos, Integer> spot : spots.entrySet()) {
                BlockPos pos = spot.getKey();
                GlStateManager.pushMatrix();
                GlStateManager.translated((pos.getX() + 0.1) / scale, (pos.getY() + 1) / scale, (pos.getZ() + 0.1) / scale);
                GlStateManager.rotatef(90F, 1F, 0F, 0F);
                GlStateManager.scalef(0.65F, 0.65F, 0.65F);
                mc.fontRenderer.drawString(format.format(spot.getValue()), 0, 0, 0);
                GlStateManager.popMatrix();
            }

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glPopMatrix();
        }

        if (Helper.isHoldingItem(mc.player, ModItems.RANGE_VISUALIZER)) {
            DimensionType dim = mc.world.getDimension().getType();
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBegin(GL11.GL_QUADS);
            for (BlockPos pos : RangeVisualizer.VISUALIZED_BLOCKS.get(dim)) {
                if (!mc.world.isBlockLoaded(pos))
                    continue;
                BlockState state = mc.world.getBlockState(pos);
                Block block = state.getBlock();
                if (!(block instanceof IVisualizable))
                    continue;
                this.renderVisualize((IVisualizable) block, mc.world, pos);
            }
            for (Entity entity : RangeVisualizer.VISUALIZED_ENTITIES.get(dim)) {
                if (!entity.isAlive() || !(entity instanceof IVisualizable))
                    continue;
                this.renderVisualize((IVisualizable) entity, mc.world, entity.getPosition());
            }
            GL11.glEnd();
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }

    private void renderVisualize(IVisualizable visualize, World world, BlockPos pos) {
        AxisAlignedBB box = visualize.getVisualizationBounds(world, pos);
        if (box == null)
            return;
        box = box.grow(0.05F);
        int color = visualize.getVisualizationColor(world, pos);
        GlStateManager.color4f(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 0.5F);
        Helper.renderWeirdBox(box.minX, box.minY, box.minZ, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);
    }

    @SubscribeEvent
    public void onOverlayRender(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getType() == ElementType.ALL) {
            MainWindow res = event.getWindow();
            if (mc.player != null) {
                if (!heldCache.isEmpty()) {
                    IAuraContainer container = heldCache.getCapability(NaturesAuraAPI.capAuraContainer, null).orElse(null);
                    int width = MathHelper.ceil(container.getStoredAura() / (float) container.getMaxAura() * 80);
                    int x = res.getScaledWidth() / 2 - 173 - (mc.player.getHeldItemOffhand().isEmpty() ? 0 : 29);
                    int y = res.getScaledHeight() - 8;

                    GlStateManager.pushMatrix();

                    int color = container.getAuraColor();
                    GlStateManager.color3f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
                    mc.getTextureManager().bindTexture(OVERLAYS);
                    if (width < 80)
                        AbstractGui.blit(x + width, y, width, 0, 80 - width, 6, 256, 256);
                    if (width > 0)
                        AbstractGui.blit(x, y, 0, 6, width, 6, 256, 256);

                    float scale = 0.75F;
                    GlStateManager.scalef(scale, scale, scale);
                    String s = heldCache.getDisplayName().getFormattedText();
                    mc.fontRenderer.drawString(s, (x + 80) / scale - mc.fontRenderer.getStringWidth(s), (y - 7) / scale, color);

                    GlStateManager.color3f(1F, 1F, 1F);
                    GlStateManager.popMatrix();
                }

                if (!heldEye.isEmpty() || !heldOcular.isEmpty()) {
                    GlStateManager.pushMatrix();
                    mc.getTextureManager().bindTexture(OVERLAYS);

                    int conf = ModConfig.client.auraBarLocation;
                    if (!mc.gameSettings.showDebugInfo && (conf != 2 || !(mc.currentScreen instanceof ChatScreen))) {
                        GlStateManager.color3f(83 / 255F, 160 / 255F, 8 / 255F);

                        int totalAmount = IAuraChunk.triangulateAuraInArea(mc.world, mc.player.getPosition(), 35);
                        float totalPercentage = totalAmount / (IAuraChunk.DEFAULT_AURA * 2F);
                        String text = I18n.format("info." + NaturesAura.MOD_ID + ".aura_in_area");
                        float textScale = 0.75F;

                        int startX = conf % 2 == 0 ? 3 : res.getScaledWidth() - 3 - 6;
                        int startY = conf < 2 ? 10 : (!heldOcular.isEmpty() && (totalPercentage > 1F || totalPercentage < 0) ? -26 : 0) + res.getScaledHeight() - 60;
                        float plusOffX = conf % 2 == 0 ? 7 : -1 - 6;
                        float textX = conf % 2 == 0 ? 3 : res.getScaledWidth() - 3 - mc.fontRenderer.getStringWidth(text) * textScale;
                        float textY = conf < 2 ? 3 : res.getScaledHeight() - 3 - 6;

                        int tHeight = MathHelper.ceil(MathHelper.clamp(totalPercentage, 0F, 1F) * 50);
                        int y = !heldOcular.isEmpty() && totalPercentage > 1F ? startY + 26 : startY;
                        if (tHeight < 50)
                            AbstractGui.blit(startX, y, 6, 12, 6, 50 - tHeight, 256, 256);
                        if (tHeight > 0)
                            AbstractGui.blit(startX, y + 50 - tHeight, 0, 12 + 50 - tHeight, 6, tHeight, 256, 256);

                        if (!heldOcular.isEmpty()) {
                            GlStateManager.color3f(160 / 255F, 83 / 255F, 8 / 255F);

                            int topHeight = MathHelper.ceil(MathHelper.clamp((totalPercentage - 1F) * 2F, 0F, 1F) * 25);
                            if (topHeight > 0) {
                                if (topHeight < 25)
                                    AbstractGui.blit(startX, startY, 18, 12, 6, 25 - topHeight, 256, 256);
                                AbstractGui.blit(startX, startY + 25 - topHeight, 12, 12 + 25 - topHeight, 6, topHeight, 256, 256);
                            }
                            int bottomHeight = MathHelper.floor(MathHelper.clamp((totalPercentage + 1F) * 2F - 1F, 0F, 1F) * 25);
                            if (bottomHeight < 25) {
                                AbstractGui.blit(startX, startY + 51, 18, 12, 6, 25 - bottomHeight, 256, 256);
                                if (bottomHeight > 0)
                                    AbstractGui.blit(startX, startY + 51 + 25 - bottomHeight, 12, 12 + 25 - bottomHeight, 6, bottomHeight, 256, 256);
                            }
                        }

                        int color = heldOcular.isEmpty() ? 0x53a008 : 0xa05308;
                        if (totalPercentage > (heldOcular.isEmpty() ? 1F : 1.5F))
                            mc.fontRenderer.drawString("+", startX + plusOffX, startY - 0.5F, color);
                        if (totalPercentage < (heldOcular.isEmpty() ? 0F : -0.5F))
                            mc.fontRenderer.drawString("-", startX + plusOffX, startY - 0.5F + (heldOcular.isEmpty() ? 44 : 70), color);

                        GlStateManager.pushMatrix();
                        GlStateManager.scalef(textScale, textScale, textScale);
                        mc.fontRenderer.drawString(text, textX / textScale, textY / textScale, 0x53a008);
                        GlStateManager.popMatrix();

                        if (!heldOcular.isEmpty()) {
                            float scale = 0.75F;
                            GlStateManager.pushMatrix();
                            GlStateManager.scalef(scale, scale, scale);
                            int stackX = conf % 2 == 0 ? 10 : res.getScaledWidth() - 22;
                            int stackY = conf < 2 ? 15 : res.getScaledHeight() - 55;
                            for (Tuple<ItemStack, Boolean> effect : SHOWING_EFFECTS.values()) {
                                int theX = (int) (stackX / scale);
                                int theY = (int) (stackY / scale);
                                ItemStack stack = effect.getA();
                                Helper.renderItemInGui(stack, theX, theY, 1F);
                                if (effect.getB()) {
                                    GlStateManager.disableDepthTest();
                                    mc.getTextureManager().bindTexture(OVERLAYS);
                                    AbstractGui.blit(theX, theY, 240, 0, 16, 16, 256, 256);
                                    GlStateManager.enableDepthTest();
                                }
                                stackY += 8;
                            }
                            GlStateManager.popMatrix();
                        }
                    }

                    if (mc.objectMouseOver instanceof BlockRayTraceResult) {
                        BlockPos pos = ((BlockRayTraceResult) mc.objectMouseOver).getPos();
                        if (pos != null) {
                            TileEntity tile = mc.world.getTileEntity(pos);
                            IAuraContainer container;
                            if (tile != null && (container = tile.getCapability(NaturesAuraAPI.capAuraContainer, null).orElse(null)) != null) {
                                BlockState state = mc.world.getBlockState(pos);
                                ItemStack blockStack = state.getBlock().getPickBlock(state, mc.objectMouseOver, mc.world, pos, mc.player);
                                this.drawContainerInfo(container.getStoredAura(), container.getMaxAura(), container.getAuraColor(),
                                        mc, res, 35, blockStack.getDisplayName().toString(), null);

                                if (tile instanceof TileEntityNatureAltar) {
                                    ItemStack tileStack = ((TileEntityNatureAltar) tile).getItemHandler(null).getStackInSlot(0);
                                    if (!tileStack.isEmpty()) {
                                        IAuraContainer stackCont = tileStack.getCapability(NaturesAuraAPI.capAuraContainer, null).orElse(null);
                                        if (stackCont != null) {
                                            this.drawContainerInfo(stackCont.getStoredAura(), stackCont.getMaxAura(), stackCont.getAuraColor(),
                                                    mc, res, 55, tileStack.getDisplayName().toString(), null);
                                        }
                                    }
                                }
                            } else if (tile instanceof TileEntityRFConverter) {
                                EnergyStorage storage = ((TileEntityRFConverter) tile).storage;
                                this.drawContainerInfo(storage.getEnergyStored(), storage.getMaxEnergyStored(), 0xcc4916,
                                        mc, res, 35, I18n.format("tile.naturesaura.rf_converter.name"),
                                        storage.getEnergyStored() + " / " + storage.getMaxEnergyStored() + " RF");
                            } else if (tile instanceof TileEntityGratedChute) {
                                TileEntityGratedChute chute = (TileEntityGratedChute) tile;
                                ItemStack stack = chute.getItemHandler(null).getStackInSlot(0);

                                int x = res.getScaledWidth() / 2;
                                int y = res.getScaledHeight() / 2;
                                if (stack.isEmpty())
                                    mc.fontRenderer.drawString(
                                            TextFormatting.GRAY.toString() + TextFormatting.ITALIC + I18n.format("info.naturesaura.empty"),
                                            x + 5, y - 11, 0xFFFFFF);
                                else
                                    Helper.renderItemInGui(stack, x + 2, y - 18, 1F);

                                Helper.renderItemInGui(ITEM_FRAME, x - 24, y - 24, 1F);
                                mc.getTextureManager().bindTexture(OVERLAYS);
                                int u = chute.isBlacklist ? 240 : 224;
                                GlStateManager.disableDepthTest();
                                AbstractGui.blit(x - 18, y - 18, u, 0, 16, 16, 256, 256);
                                GlStateManager.enableDepthTest();
                            }
                        }
                    }

                    GlStateManager.color3f(1F, 1F, 1F);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private void drawContainerInfo(int stored, int max, int color, Minecraft mc, MainWindow res, int yOffset, String name, String textBelow) {
        GlStateManager.color3f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);

        int x = res.getScaledWidth() / 2 - 40;
        int y = res.getScaledHeight() / 2 + yOffset;
        int width = MathHelper.ceil(stored / (float) max * 80);

        mc.getTextureManager().bindTexture(OVERLAYS);
        if (width < 80)
            AbstractGui.blit(x + width, y, width, 0, 80 - width, 6, 256, 256);
        if (width > 0)
            AbstractGui.blit(x, y, 0, 6, width, 6, 256, 256);

        mc.fontRenderer.drawString(name, x + 40 - mc.fontRenderer.getStringWidth(name) / 2F, y - 9, color);

        if (textBelow != null)
            mc.fontRenderer.drawString(textBelow, x + 40 - mc.fontRenderer.getStringWidth(textBelow) / 2F, y + 7, color);
    }
}
