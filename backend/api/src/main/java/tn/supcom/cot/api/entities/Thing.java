package tn.supcom.cot.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import tn.supcom.cot.core.entities.NosqlEntity;

import java.time.Instant;

/**
 * Objet connecté (caméra, capteur, barrière, passerelle, place).
 * L'identifiant est le {@code thingId} du contrat MQTT (ex. "camera-entree").
 */
@Entity("things")
public class Thing extends NosqlEntity {

    // Valeurs du contrat (OpenAPI : ThingType / ThingState)
    public static final String CAMERA = "camera";
    public static final String DISTANCE_SENSOR = "distance_sensor";
    public static final String BARRIER = "barrier";
    public static final String GATEWAY = "gateway";
    public static final String ONLINE = "online";
    public static final String OFFLINE = "offline";

    @Column private String type;
    @Column private String parkingId;
    @Column private String state = OFFLINE;
    @Column private Instant lastSeen;
    @Column private String lastDatum;   // dernier message "datum" tel quel (JSON)

    /** Déduit le type d'après le nom de l'objet (convention du simulateur). */
    public static String typeFromId(String thingId) {
        if (thingId.startsWith("camera")) return CAMERA;
        if (thingId.startsWith("capteur") || thingId.startsWith("place")) return DISTANCE_SENSOR;
        if (thingId.startsWith("barriere")) return BARRIER;
        if (thingId.startsWith("passerelle")) return GATEWAY;
        return DISTANCE_SENSOR;
    }

    public static Thing of(String thingId) {
        var t = new Thing();
        t.setId(thingId);
        t.type = typeFromId(thingId);
        return t;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getParkingId() { return parkingId; }
    public void setParkingId(String parkingId) { this.parkingId = parkingId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    public String getLastDatum() { return lastDatum; }
    public void setLastDatum(String lastDatum) { this.lastDatum = lastDatum; }
}
