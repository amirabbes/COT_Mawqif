package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;

import java.time.Instant;

/** Commande envoyée à un objet sur {@code things/<thingId>/cmd} (trace d'audit incluse). */
@Entity("barrier_commands")
public class BarrierCommand extends NosqlEntity {

    public static final String OPEN_BARRIER = "open_barrier";
    public static final String SENT = "sent";
    public static final String EXECUTED = "executed";
    public static final String REJECTED = "rejected";
    public static final String EXPIRED = "expired";

    @Column private String thingId;
    @Column private String parkingId;
    @Column private String command = OPEN_BARRIER;
    @Column private String reason;
    @Column private String status = SENT;
    @Column private Instant issuedAt;
    @Column private Instant expiresAt;
    @Column private String sig;

    public String getThingId() { return thingId; }
    public void setThingId(String thingId) { this.thingId = thingId; }
    public String getParkingId() { return parkingId; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getSig() { return sig; }
    public void setSig(String sig) { this.sig = sig; }
}
