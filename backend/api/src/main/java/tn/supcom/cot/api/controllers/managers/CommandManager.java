package tn.supcom.cot.api.controllers.managers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import org.eclipse.paho.mqttv5.common.MqttException;
import tn.supcom.cot.api.boundaries.Dtos.CommandDto;
import tn.supcom.cot.api.controllers.repositories.BarrierCommandRepository;
import tn.supcom.cot.api.controllers.repositories.ThingRepository;
import tn.supcom.cot.api.entities.BarrierCommand;
import tn.supcom.cot.api.entities.Thing;
import tn.supcom.cot.api.mqtt.Topics;
import tn.supcom.cot.core.utilities.UUIDv7;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/** Commandes signées vers la barrière (things/<barrière>/cmd) et leurs accusés (things/<barrière>/ack). */
@ApplicationScoped
public class CommandManager {
    private static final Logger LOG = Logger.getLogger(CommandManager.class.getName());
    private static final Duration TTL = Duration.ofSeconds(10);

    @Inject
    private MqttManager mqttManager;
    @Inject
    private BarrierCommandRepository commandRepository;
    @Inject
    private ThingRepository thingRepository;
    @Inject
    private ParkingManager parkingManager;
    @Inject
    private CommandSigner signer;
    @Inject
    private WebSocketManager webSocketManager;

    /** Ouverture manuelle (POST /parkings/{id}/barrier/open). Seul le backend peut appeler ceci. */
    public BarrierCommand openBarrier(String parkingId, String reason) throws MqttException {
        parkingManager.find(parkingId); // 404 si inconnu

        var cmd = new BarrierCommand();
        cmd.setId(UUIDv7.generate().toString());
        cmd.setThingId(barrierThingId(parkingId));
        cmd.setParkingId(parkingId);
        cmd.setReason(reason);
        cmd.setIssuedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        cmd.setExpiresAt(cmd.getIssuedAt().plus(TTL));
        cmd.setSig(signer.sign(cmd.getId(), cmd.getCommand(), parkingId, cmd.getIssuedAt(), cmd.getExpiresAt()));
        commandRepository.save(cmd);

        var message = Json.createObjectBuilder()
                .add("cmdId", cmd.getId())
                .add("command", cmd.getCommand())
                .add("parking", parkingId)
                .add("reason", reason)
                .add("issuedAt", cmd.getIssuedAt().toString())
                .add("expiresAt", cmd.getExpiresAt().toString())
                .add("sig", cmd.getSig())
                .build().toString();
        mqttManager.publish(Topics.cmd(cmd.getThingId()), message.getBytes(StandardCharsets.UTF_8), 1, false);
        return cmd;
    }

    /** things/<thingId>/ack : {"cmdId", "result": "executed"|"rejected"}. */
    public void onAck(String thingId, byte[] payload) {
        var json = Json.createReader(new StringReader(new String(payload, StandardCharsets.UTF_8))).readObject();
        var cmdId = json.getString("cmdId", null);
        var cmd = cmdId == null ? null : commandRepository.findById(cmdId).orElse(null);
        if (cmd == null || !thingId.equals(cmd.getThingId())) {
            LOG.warning("ack ignoré (commande inconnue) : " + cmdId);
            return;
        }
        if (BarrierCommand.SENT.equals(cmd.getStatus())) {
            cmd.setStatus("executed".equals(json.getString("result", "")) ? BarrierCommand.EXECUTED : BarrierCommand.REJECTED);
            commandRepository.save(Versions.bump(cmd));
        }
        webSocketManager.broadcast("command_ack", CommandDto.from(cmd), WebSocketManager.ADMIN);
    }

    private String barrierThingId(String parkingId) {
        return thingRepository.findAll().stream()
                .filter(t -> Thing.BARRIER.equals(t.getType()) && parkingId.equals(t.getParkingId()))
                .map(Thing::getId)
                .findFirst()
                .orElse(Topics.BARRIER_THING);
    }
}
