package io.github.makc.aquacompat.mixin;

import com.teammetallurgy.aquaculture.block.FarmlandMoistBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FarmlandMoistBlock.class)
public class FarmlandMoistBlockMixin {
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        if (facing != Direction.UP) {
            return TriState.FALSE;
        }

        var plantBlock = plant.getBlock();
        if (plant.is(BlockTags.MAINTAINS_FARMLAND)
            || plantBlock instanceof CropBlock
            || plantBlock instanceof StemBlock
            || plantBlock instanceof AttachedStemBlock
            || plantBlock instanceof PitcherCropBlock
            || plantBlock instanceof NetherWartBlock
            || plantBlock instanceof SugarCaneBlock
            || plantBlock instanceof CactusBlock
            || plantBlock == Blocks.BAMBOO
            || plantBlock == Blocks.BAMBOO_SAPLING
            || plantBlock instanceof BushBlock) {
            return TriState.TRUE;
        }

        return TriState.DEFAULT;
    }
}
