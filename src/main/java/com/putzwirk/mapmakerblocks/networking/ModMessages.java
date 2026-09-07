package com.putzwirk.mapmakerblocks.networking;

import com.putzwirk.mapmakerblocks.Mapmakerblocks;
import com.putzwirk.mapmakerblocks.block.RedstoneTimerBlock;
import com.putzwirk.mapmakerblocks.block.entity.RedstoneTimerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ModMessages {

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(RedstoneTimerUpdatePayload.ID, (server, player, handler, buf, responseSender) -> {
            RedstoneTimerUpdatePayload payload = RedstoneTimerUpdatePayload.read(buf);

            server.execute(() -> {
                ServerWorld world = player.getServerWorld();
                BlockPos pos = payload.pos();

                if (!(world.getBlockEntity(pos) instanceof RedstoneTimerBlockEntity blockEntity)) {
                    return;
                }

                BlockState state = world.getBlockState(pos);

                blockEntity.setSeconds(payload.seconds());
                blockEntity.setRandomDelay(payload.randomDelay());
                blockEntity.setOnSeconds(payload.onSeconds());
                blockEntity.setRandomOnSeconds(payload.randomOnSeconds());
                blockEntity.setLoop(payload.loop());
                blockEntity.setEnabled(payload.enabled());
                blockEntity.setAutonomousStart(payload.autonomousStart());

                blockEntity.resetRuntime();

                if (state.getBlock() instanceof RedstoneTimerBlock timerBlock) {

                    if (state.get(RedstoneTimerBlock.OUTPUT_POWERED)) {
                        BlockState offState = state.with(RedstoneTimerBlock.OUTPUT_POWERED, false);

                        world.setBlockState(pos, offState, Block.NOTIFY_ALL);
                        world.updateNeighbors(pos, timerBlock);
                    }

                    if (payload.enabled() && payload.autonomousStart()) {
                        timerBlock.startTimer(
                                world,
                                pos,
                                world.getBlockState(pos)
                        );
                    }
                }
            });
        });
    }
}
