package de.ellpeck.naturesaura.blocks.tiles;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import de.ellpeck.naturesaura.blocks.BlockFurnaceHeater;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class TileEntityFurnaceHeater extends TileEntityImpl implements ITickableTileEntity {

    public boolean isActive;

    public TileEntityFurnaceHeater(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void tick() {
        if (!this.world.isRemote && this.world.getGameTime() % 5 == 0) {
            boolean did = false;

            Direction facing = this.world.getBlockState(this.pos).get(BlockFurnaceHeater.FACING);
            BlockPos tilePos = this.pos.offset(facing.getOpposite());
            TileEntity tile = this.world.getTileEntity(tilePos);
            if (tile instanceof FurnaceTileEntity) {
                FurnaceTileEntity furnace = (FurnaceTileEntity) tile;
                if (isReady(furnace)) {
                    // TODO furnace heater
                   /* int time = furnace.getField(0);
                    if (time <= 0)
                        FurnaceBlock.setState(true, this.world, furnace.getPos());
                    furnace.setField(0, 200);
                    //if set higher than 199, it'll never finish because the furnace does ++ and then ==
                    furnace.setField(2, Math.min(199, furnace.getField(2) + 5));*/

                    BlockPos spot = IAuraChunk.getHighestSpot(this.world, this.pos, 20, this.pos);
                    IAuraChunk chunk = IAuraChunk.getAuraChunk(this.world, spot);
                    //chunk.drainAura(spot, MathHelper.ceil((200 - time) * 16.6F));
                    did = true;

                    if (this.world.getGameTime() % 15 == 0) {
                        // TODO particles
                        /*PacketHandler.sendToAllAround(this.world, this.pos, 32, new PacketParticleStream(
                                this.pos.getX() + (float) this.world.rand.nextGaussian() * 5F,
                                this.pos.getY() + 1 + this.world.rand.nextFloat() * 5F,
                                this.pos.getZ() + (float) this.world.rand.nextGaussian() * 5F,
                                tilePos.getX() + this.world.rand.nextFloat(),
                                tilePos.getY() + this.world.rand.nextFloat(),
                                tilePos.getZ() + this.world.rand.nextFloat(),
                                this.world.rand.nextFloat() * 0.07F + 0.07F, IAuraType.forWorld(this.world).getColor(), this.world.rand.nextFloat() + 0.5F
                        ));*/
                    }
                }
            }

            if (did != this.isActive) {
                this.isActive = did;
                this.sendToClients();
            }
        }
    }

    private static boolean isReady(FurnaceTileEntity furnace) {
        if (!furnace.getStackInSlot(1).isEmpty())
            return false;

        ItemStack input = furnace.getStackInSlot(0);
        if (!input.isEmpty()) {
            /*ItemStack output = FurnaceRecipes.instance().getSmeltingResult(input);
            if (output.isEmpty())*/
            return false;

            /*ItemStack currOutput = furnace.getStackInSlot(2);
            return currOutput.isEmpty() ||
                    Helper.areItemsEqual(currOutput, output, true) && currOutput.getCount() + output.getCount() <= output.getMaxStackSize();*/
        } else
            return false;
    }

    @Override
    public void writeNBT(CompoundNBT compound, SaveType type) {
        super.writeNBT(compound, type);

        if (type == SaveType.SYNC)
            compound.putBoolean("active", this.isActive);
    }

    @Override
    public void readNBT(CompoundNBT compound, SaveType type) {
        super.readNBT(compound, type);

        if (type == SaveType.SYNC)
            this.isActive = compound.getBoolean("active");
    }
}
