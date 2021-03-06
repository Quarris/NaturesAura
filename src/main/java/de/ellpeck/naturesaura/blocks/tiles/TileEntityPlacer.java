package de.ellpeck.naturesaura.blocks.tiles;

import de.ellpeck.naturesaura.Helper;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.blocks.ModBlocks;
import de.ellpeck.naturesaura.items.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class TileEntityPlacer extends TileEntityImpl implements ITickableTileEntity {

    public TileEntityPlacer(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void tick() {
        if (!this.world.isRemote && this.world.getGameTime() % 15 == 0) {
            if (this.redstonePower > 0)
                return;
            TileEntity tileUp = this.world.getTileEntity(this.pos.up());
            if (tileUp == null)
                return;
            IItemHandler handler = tileUp.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).orElse(null);
            if (handler == null)
                return;
            List<ItemFrameEntity> frames = Helper.getAttachedItemFrames(this.world, this.pos);
            if (frames.isEmpty())
                return;

            List<BlockPos> validPositions = new ArrayList<>();
            int range = 5;
            for (int x = -range; x <= range; x++)
                for (int y = -range; y <= range; y++)
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = this.pos.add(x, y, z);
                        if (!this.framesContain(frames, pos, this.world.getBlockState(pos)))
                            continue;

                        BlockPos up = pos.up();
                        BlockState state = this.world.getBlockState(up);
                        // TODO maybe allow the placer to place on more than air?
                        if (state.isAir(this.world, up))
                            validPositions.add(up);
                    }
            if (validPositions.isEmpty())
                return;

            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.extractItem(i, 1, true);
                if (stack.isEmpty())
                    continue;

                BlockPos pos = validPositions.get(this.world.rand.nextInt(validPositions.size()));
                ItemStack left = this.tryPlace(stack.copy(), pos);
                if (ItemStack.areItemStacksEqual(stack, left))
                    continue;

                handler.extractItem(i, 1, false);
                BlockPos spot = IAuraChunk.getHighestSpot(this.world, this.pos, 10, this.pos);
                IAuraChunk.getAuraChunk(this.world, spot).drainAura(spot, 1000);

                // TODO particles
                //  PacketHandler.sendToAllAround(this.world, this.pos, 32, new PacketParticles(pos.getX(), pos.getY(), pos.getZ(), 9));

                return;
            }
        }
    }

    private boolean framesContain(List<ItemFrameEntity> frames, BlockPos pos, BlockState state) {
        ItemStack stack = state.getBlock().getItem(this.world, pos, state);
        if (stack.isEmpty())
            return false;

        for (ItemFrameEntity frame : frames) {
            ItemStack frameStack = frame.getDisplayedItem();
            if (frameStack.isEmpty())
                continue;
            if (Helper.areItemsEqual(stack, frameStack, false))
                return true;

            if (state.getBlock() == Blocks.FARMLAND && frameStack.getItem() == ModItems.FARMING_STENCIL)
                return true;
        }
        return false;
    }

    private ItemStack tryPlace(ItemStack stack, BlockPos pos) {
        if (this.handleSpecialCases(stack, pos))
            return stack;

        if (!(this.world instanceof ServerWorld))
            return stack;

        FakePlayer fake = FakePlayerFactory.getMinecraft((ServerWorld) this.world);
        fake.inventory.mainInventory.set(fake.inventory.currentItem, stack);
        fake.interactionManager.processRightClick(fake, this.world, fake.getHeldItemMainhand(), Hand.MAIN_HAND);
        return fake.getHeldItemMainhand().copy();
    }

    private boolean handleSpecialCases(ItemStack stack, BlockPos pos) {
        // TODO placer special cases
      /*  if (stack.getItem() == Items.REDSTONE)
            if (Blocks.REDSTONE_WIRE.canPlaceBlockAt(this.world, pos))
                this.world.setBlockState(pos, Blocks.REDSTONE_WIRE.getDefaultState());
            else
                return false;
        else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.GOLD_POWDER))
            if (ModBlocks.GOLD_POWDER.canPlaceBlockAt(this.world, pos))
                this.world.setBlockState(pos, ModBlocks.GOLD_POWDER.getDefaultState());
            else
                return false;
        else if (stack.getItem() instanceof IPlantable) {
            IPlantable plantable = (IPlantable) stack.getItem();
            BlockState plant = plantable.getPlant(this.world, pos);
            if (!plant.getBlock().canPlaceBlockAt(this.world, pos))
                return false;
            BlockState state = this.world.getBlockState(pos);
            if (!state.getBlock().isAir(state, this.world, pos))
                return false;
            this.world.setBlockState(pos, plant);
        } else*/
            return false;

        /*stack.shrink(1);
        return true;*/
    }
}
