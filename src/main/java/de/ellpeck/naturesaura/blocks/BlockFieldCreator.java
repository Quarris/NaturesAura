package de.ellpeck.naturesaura.blocks;

import de.ellpeck.naturesaura.NaturesAura;
import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.blocks.tiles.ModTileEntities;
import de.ellpeck.naturesaura.blocks.tiles.TileEntityFieldCreator;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

public class BlockFieldCreator extends BlockContainerImpl {
    public BlockFieldCreator() {
        super("field_creator", ModTileEntities.FIELD_CREATOR, ModBlocks.prop(Material.ROCK).hardnessAndResistance(2F).sound(SoundType.STONE));
    }

    @Override
    public boolean onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileEntityFieldCreator) {
            if (!worldIn.isRemote) {
                String key = NaturesAura.MOD_ID + ":field_creator_pos";
                CompoundNBT compound = player.getPersistentData();
                if (!player.isSneaking() && compound.contains(key)) {
                    BlockPos stored = BlockPos.fromLong(compound.getLong(key));
                    TileEntityFieldCreator creator = (TileEntityFieldCreator) tile;
                    if (!pos.equals(stored)) {
                        if (creator.isCloseEnough(stored)) {
                            TileEntity otherTile = worldIn.getTileEntity(stored);
                            if (otherTile instanceof TileEntityFieldCreator) {
                                creator.connectionOffset = stored.subtract(pos);
                                creator.isMain = true;
                                creator.sendToClients();

                                TileEntityFieldCreator otherCreator = (TileEntityFieldCreator) otherTile;
                                otherCreator.connectionOffset = pos.subtract(stored);
                                otherCreator.isMain = false;
                                otherCreator.sendToClients();

                                compound.remove(key);
                                player.sendStatusMessage(new TranslationTextComponent("info." + NaturesAura.MOD_ID + ".connected"), true);
                            } else
                                player.sendStatusMessage(new TranslationTextComponent("info." + NaturesAura.MOD_ID + ".stored_pos_gone"), true);
                        } else
                            player.sendStatusMessage(new TranslationTextComponent("info." + NaturesAura.MOD_ID + ".too_far"), true);
                    } else
                        player.sendStatusMessage(new TranslationTextComponent("info." + NaturesAura.MOD_ID + ".same_position"), true);
                } else {
                    compound.putLong(key, pos.toLong());
                    player.sendStatusMessage(new TranslationTextComponent("info." + NaturesAura.MOD_ID + ".stored_pos"), true);
                }
            }
            return true;
        } else
            return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileEntityFieldCreator) {
            TileEntityFieldCreator creator = (TileEntityFieldCreator) tile;
            if (creator.isCharged) {
                BlockPos connected = creator.getConnectedPos();
                if (connected != null)
                    NaturesAuraAPI.instance().spawnParticleStream(
                            pos.getX() + 0.25F + rand.nextFloat() * 0.5F,
                            pos.getY() + 0.25F + rand.nextFloat() * 0.5F,
                            pos.getZ() + 0.25F + rand.nextFloat() * 0.5F,
                            connected.getX() + 0.25F + rand.nextFloat() * 0.5F,
                            connected.getY() + 0.25F + rand.nextFloat() * 0.5F,
                            connected.getZ() + 0.25F + rand.nextFloat() * 0.5F,
                            0.65F, 0x4245f4, 1F
                    );
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isSolid(BlockState state) {
        return false;
    }
}
