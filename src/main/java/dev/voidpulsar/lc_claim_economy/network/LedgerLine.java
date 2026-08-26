package dev.voidpulsar.lc_claim_economy.network;

import dev.voidpulsar.lc_claim_economy.data.LcClaimEconomySavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Wire form of {@link LcClaimEconomySavedData.LedgerEntry} for the client History screen. */
public record LedgerLine(long timestamp, LcClaimEconomySavedData.LedgerKind kind, long copperDelta, String detailKey) {
    private static final LcClaimEconomySavedData.LedgerKind[] KINDS = LcClaimEconomySavedData.LedgerKind.values();

    public static LedgerLine from(LcClaimEconomySavedData.LedgerEntry entry) {
        return new LedgerLine(entry.timestamp(), entry.kind(), entry.copperDelta(), entry.detail());
    }

    public static final StreamCodec<FriendlyByteBuf, LedgerLine> STREAM_CODEC = StreamCodec.of(
            (buffer, line) -> {
                buffer.writeLong(line.timestamp);
                buffer.writeVarInt(line.kind.ordinal());
                buffer.writeLong(line.copperDelta);
                buffer.writeUtf(line.detailKey);
            },
            buffer -> new LedgerLine(
                    buffer.readLong(),
                    KINDS[buffer.readVarInt()],
                    buffer.readLong(),
                    buffer.readUtf()
            )
    );
}
