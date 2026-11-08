package tn.supcom.cot.api.boundaries;

import tn.supcom.cot.api.entities.Alert;
import tn.supcom.cot.api.entities.BarrierCommand;
import tn.supcom.cot.api.entities.Parking;
import tn.supcom.cot.api.entities.ParkingSlot;
import tn.supcom.cot.api.entities.Thing;

import java.time.Instant;

/**
 * Objets échangés par l'API REST et le WebSocket.
 * Ils reprennent exactement les schémas de docs/contracts/openapi.yaml.
 */
public final class Dtos {
    private Dtos() {}

    public record ParkingDto(String id, String name, Double lat, Double lon, int totalSlots,
                             int freeSlots, String barrierState, Instant updatedAt) {
        public static ParkingDto from(Parking p) {
            return new ParkingDto(p.getId(), p.getName(), p.getLat(), p.getLon(), p.getTotalSlots(),
                    p.getFreeSlots(), p.getBarrierState(), p.getUpdatedAt());
        }
    }

    public record SlotDto(String id, String parkingId, int number, String state, String plate, Instant updatedAt) {
        public static SlotDto from(ParkingSlot s) {
            return new SlotDto(s.getId(), s.getParkingId(), s.getNumber(), s.getState(), s.getPlate(), s.getUpdatedAt());
        }
    }

    public record ThingDto(String id, String type, String parkingId, String state, Instant lastSeen, String lastDatum) {
        public static ThingDto from(Thing t) {
            return new ThingDto(t.getId(), t.getType(), t.getParkingId(), t.getState(), t.getLastSeen(), t.getLastDatum());
        }
    }

    public record AlertDto(String id, String parkingId, String type, String status, Integer slot, String plate,
                           String message, Instant createdAt, Instant acknowledgedAt) {
        public static AlertDto from(Alert a) {
            return new AlertDto(a.getId(), a.getParkingId(), a.getType(), a.getStatus(), a.getSlot(), a.getPlate(),
                    a.getMessage(), a.getCreatedAt(), a.getAcknowledgedAt());
        }
    }

    public record CommandDto(String id, String thingId, String command, String status, Instant issuedAt, Instant expiresAt) {
        public static CommandDto from(BarrierCommand c) {
            return new CommandDto(c.getId(), c.getThingId(), c.getCommand(), c.getStatus(), c.getIssuedAt(), c.getExpiresAt());
        }
    }

    public record OpenBarrierRequest(String reason) {}

    /** Erreur "problem+json" (RFC 9457). */
    public record Problem(String type, String title, int status, String detail) {
        public static Problem of(int status, String title, String detail) {
            return new Problem("about:blank", title, status, detail);
        }
    }

    /** Enveloppe des messages WebSocket (voir docs/contracts/README.md). */
    public record Envelope(String type, Object data, Instant ts) {
        public static Envelope of(String type, Object data) {
            return new Envelope(type, data, Instant.now());
        }
    }
}
