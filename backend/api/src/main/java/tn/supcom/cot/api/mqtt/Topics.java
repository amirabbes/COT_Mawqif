package tn.supcom.cot.api.mqtt;

/**
 * Topics MQTT du contrat (docs/contracts/mqtt-contract.md).
 * Un seul endroit pour les noms : si le contrat change, on change ici.
 */
public final class Topics {
    private Topics() {}

    // Abonnements du backend
    public static final String SUB_STATUS = "things/+/status";
    public static final String SUB_DATUM  = "things/+/datum";
    public static final String SUB_ACK    = "things/+/ack";
    public static final String SUB_ALERTS = "alerts/+/+";

    // Dernier segment des topics "things/<thingId>/<action>"
    public static final String STATUS = "status";
    public static final String DATUM  = "datum";
    public static final String ACK    = "ack";
    public static final String CMD    = "cmd";

    // Valeurs du champ "event" des messages datum
    public static final String EVENT_VEHICLE_ENTERED = "vehicle_entered";
    public static final String EVENT_VEHICLE_EXITED  = "vehicle_exited";
    public static final String EVENT_SLOT_STATE      = "slot_state";
    public static final String EVENT_DISTANCE        = "distance_reading";
    public static final String EVENT_BARRIER_STATE   = "barrier_state";

    /** Objet qui reçoit les commandes de barrière. */
    public static final String BARRIER_THING = "barriere";

    public static String cmd(String thingId) {
        return "things/" + thingId + "/" + CMD;
    }

    public static String alert(String parkingId, String type) {
        return "alerts/" + parkingId + "/" + type;
    }
}
