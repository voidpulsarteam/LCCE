package dev.voidpulsar.lc_claim_economy.network;

import dev.voidpulsar.lc_claim_economy.LcClaimEconomy;
import dev.voidpulsar.lc_claim_economy.client.ClientLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record SyncLedgerPayload(List<LedgerLine> entries) implements CustomPacketPayload {
    public static final Type<SyncLedgerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LcClaimEconomy.MOD_ID, "sync_ledger"));
    public static final StreamCodec<FriendlyByteBuf, SyncLedgerPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entries.size());
                for (LedgerLine line : payload.entries) {
                    LedgerLine.STREAM_CODEC.encode(buffer, line);
                }
            },
            buffer -> {
                int count = buffer.readVarInt();
                List<LedgerLine> entries = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    entries.add(LedgerLine.STREAM_CODEC.decode(buffer));
                }
                return new SyncLedgerPayload(entries);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncLedgerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLedger.update(payload.entries()));
    }
}
