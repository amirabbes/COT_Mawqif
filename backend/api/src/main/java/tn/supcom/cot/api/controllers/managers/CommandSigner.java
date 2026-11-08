package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Signe les commandes de barrière : sig = base64url(HMAC-SHA256(clé, "cmdId|command|parking|issuedAt|expiresAt")).
 * Même formule que dans docs/contracts/mqtt-contract.md : la barrière recalcule et compare.
 */
@ApplicationScoped
public class CommandSigner {

    private final byte[] key = ConfigProvider.getConfig()
            .getValue("command.signing.key", String.class)
            .getBytes(StandardCharsets.UTF_8);

    public String sign(String cmdId, String command, String parking, Instant issuedAt, Instant expiresAt) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            var data = String.join("|", cmdId, command, parking, issuedAt.toString(), expiresAt.toString());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponible", e);
        }
    }

    public boolean verify(String cmdId, String command, String parking, Instant issuedAt, Instant expiresAt, String sig) {
        var expected = sign(cmdId, command, parking, issuedAt, expiresAt);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }
}
