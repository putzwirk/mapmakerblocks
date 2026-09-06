package com.putzwirk.mapmakerblocks.block;

import com.putzwirk.mapmakerblocks.WirelessSignManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WirelessRedstoneSignBlock extends SignBlock {

    public static final BooleanProperty POWERED = Properties.POWERED;

    public WirelessRedstoneSignBlock(Settings settings) {
        super(settings, WoodType.MANGROVE);
        setDefaultState(getStateManager().getDefaultState()
                .with(ROTATION, 0)
                .with(WATERLOGGED, false)
                .with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient) {
            WirelessSignManager.get().onNeighborUpdate((ServerWorld) world, pos);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient) {
            WirelessSignManager.get().onNeighborUpdate((ServerWorld) world, pos);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult result = super.onUse(state, world, pos, player, hand, hit);
        if (!world.isClient) {
            WirelessSignManager.get().onNeighborUpdate((ServerWorld) world, pos);
        }
        return result;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, BlockEntityType.SIGN, SignBlockEntity::tick);
    }

    @Override
    public void appendTooltip(net.minecraft.item.ItemStack stack,
                              @Nullable BlockView world,
                              java.util.List<net.minecraft.text.Text> tooltip,
                              net.minecraft.client.item.TooltipContext options) {
        tooltip.add(net.minecraft.text.Text.translatable("item.mmblocks.wireless_redstone_sign.desc")
                .formatted(net.minecraft.util.Formatting.GRAY));
    }
}