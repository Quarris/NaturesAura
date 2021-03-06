package de.ellpeck.naturesaura.blocks.tiles;

import de.ellpeck.naturesaura.api.NaturesAuraAPI;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TileEntityMossGenerator extends TileEntityImpl implements ITickableTileEntity {

    public TileEntityMossGenerator(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void tick() {
        if (!this.world.isRemote) {
            if (this.world.getGameTime() % 20 != 0)
                return;

            List<BlockPos> possibleOffsets = new ArrayList<>();
            int range = 2;
            for (int x = -range; x <= range; x++)
                for (int y = -range; y <= range; y++)
                    for (int z = -range; z <= range; z++) {
                        BlockPos offset = this.pos.add(x, y, z);
                        BlockState state = this.world.getBlockState(offset);
                        if (NaturesAuraAPI.BOTANIST_PICKAXE_CONVERSIONS.inverse().containsKey(state))
                            possibleOffsets.add(offset);
                    }

            if (possibleOffsets.isEmpty())
                return;
            BlockPos offset = possibleOffsets.get(this.world.rand.nextInt(possibleOffsets.size()));
            BlockState state = this.world.getBlockState(offset);
            BlockState result = NaturesAuraAPI.BOTANIST_PICKAXE_CONVERSIONS.inverse().get(state);

            int toAdd = 7500;
            if (this.canGenerateRightNow(35, toAdd)) {
                while (toAdd > 0) {
                    BlockPos spot = IAuraChunk.getLowestSpot(this.world, this.pos, 35, this.pos);
                    toAdd -= IAuraChunk.getAuraChunk(this.world, spot).storeAura(spot, toAdd);
                }

                // TODO particles
                /*PacketHandler.sendToAllAround(this.world, this.pos, 32,
                        new PacketParticles(offset.getX(), offset.getY(), offset.getZ(), 23));*/
            }

            this.world.playEvent(2001, offset, Block.getStateId(state));
            this.world.setBlockState(offset, result);
        }
    }

    @Override
    public boolean wantsLimitRemover() {
        return true;
    }
}
