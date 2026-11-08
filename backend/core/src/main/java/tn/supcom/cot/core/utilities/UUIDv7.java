package tn.supcom.cot.core.utilities;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

public final class UUIDv7 {
    private UUIDv7() {}
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Clock CLOCK = Clock.systemUTC();
    public static UUID generate() {
        var timestamp = CLOCK.millis();
        var msb = (timestamp & 0xFFFFFFFFL) << 16;
        msb |= 0x7000L;
        msb |= (RANDOM.nextLong() & 0x0FFFL);
        var lsb = RANDOM.nextLong();
        lsb &= 0x3FFFFFFFFFFFFFFFL;
        lsb |= 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
