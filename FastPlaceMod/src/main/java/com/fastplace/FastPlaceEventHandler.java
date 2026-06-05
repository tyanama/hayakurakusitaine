package com.fastplace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastPlaceEventHandler {

    // 最大連続設置数
    private static final int MAX_BLOCKS = 64;

    // プレイヤーUUID -> セッション
    private final Map<UUID, PlaceSession> sessions = new HashMap<>();

    // ---- 右クリックでセッション開始 ----
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();

        // サーバーサイドのみ処理
        if (level.isClientSide()) return;

        // しゃがんでいない場合はスキップ（通常の設置に任せる）
        if (!player.isShiftKeyDown()) return;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof BlockItem)) return;

        // 既にセッション中なら何もしない
        if (sessions.containsKey(player.getUUID())) return;

        // セッション開始（実際の設置はバニラに任せ、セッションだけ記録）
        PlaceSession session = new PlaceSession(held);
        BlockHitResult hit = (BlockHitResult) event.getHitVec();
        BlockPos placedAt = hit.getBlockPos().relative(hit.getDirection());
        session.addPlaced(placedAt);
        sessions.put(player.getUUID(), session);

        FastPlaceMod.LOGGER.debug("{} がFastPlaceセッション開始", player.getName().getString());
    }

    // ---- 毎tick: しゃがみ+右クリック押しっぱなしで連続設置 ----
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide()) return;

        UUID uuid = player.getUUID();
        PlaceSession session = sessions.get(uuid);

        // セッションがないかしゃがんでいない場合は終了
        if (session == null) return;
        if (!player.isShiftKeyDown()) {
            endSession(uuid, player);
            return;
        }
        if (!session.isActive()) {
            endSession(uuid, player);
            return;
        }
        if (session.getCount() >= MAX_BLOCKS) {
            endSession(uuid, player);
            return;
        }

        // プレイヤーが見ているブロックを取得
        HitResult hit = player.pick(5.0, 0, false);
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());

        // 既に設置済みならスキップ
        if (session.hasPlaced(targetPos)) return;

        // 設置しようとしている場所が空気かどうか確認
        BlockState existing = level.getBlockState(targetPos);
        if (!existing.canBeReplaced()) return;

        // メインハンドのアイテムを確認
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof BlockItem blockItem)) return;

        // 同じブロックアイテムかチェック
        if (!ItemStack.isSameItem(held, session.getItemStack())) {
            endSession(uuid, player);
            return;
        }

        // ブロックを設置
        BlockPlaceContext ctx = new BlockPlaceContext(
                player, InteractionHand.MAIN_HAND, held,
                new BlockHitResult(blockHit.getLocation(), blockHit.getDirection(), targetPos, false)
        );

        BlockState stateToPlace = blockItem.getBlock().getStateForPlacement(ctx);
        if (stateToPlace == null) return;

        boolean placed = level.setBlockAndUpdate(targetPos, stateToPlace);
        if (!placed) return;

        // インベントリ消費（サバイバルモードのみ）
        if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()) {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                endSession(uuid, player);
                return;
            }
        }

        session.addPlaced(targetPos);

        // 設置音を再生
        var soundType = stateToPlace.getSoundType();
        level.playSound(null, targetPos,
                soundType.getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);
    }

    // ---- セッション終了 ----
    private void endSession(UUID uuid, Player player) {
        PlaceSession session = sessions.remove(uuid);
        if (session != null) {
            FastPlaceMod.LOGGER.debug("{} のセッション終了: {}ブロック設置",
                    player.getName().getString(), session.getCount());
        }
    }
}
