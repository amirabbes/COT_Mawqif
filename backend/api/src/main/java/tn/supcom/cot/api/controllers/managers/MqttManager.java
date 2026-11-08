package tn.supcom.cot.api.controllers.managers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import tn.supcom.cot.api.mqtt.Topics;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client MQTT du backend (base : version du prof, avec corrections).
 * Écoute things/+/status, things/+/datum, things/+/ack et alerts/+/+ ; publie les commandes (things/<id>/cmd).
 *
 * Corrections par rapport à cot.zip :
 *  - l'identifiant client utilise bien mqtt.client.id (avant : "null#uuid") ;
 *  - on s'abonne aussi à datum et ack (avant : datum n'était jamais reçu) ;
 *  - les abonnements sont refaits à chaque (re)connexion (cleanStart = true) ;
 *  - une exception dans un message ne coupe plus le client.
 */
@Singleton
@Startup
@LocalBean
public class MqttManager implements MqttCallback {
    private static final Logger LOG = Logger.getLogger(MqttManager.class.getName());

    private final Config config = ConfigProvider.getConfig();
    private final String mqttBrokerUrl = config.getValue("mqtt.broker.url", String.class);
    private final String configuredClientId = config.getValue("mqtt.client.id", String.class);
    private final String mqttUsername = config.getValue("mqtt.broker.username", String.class);
    private final String mqttPassword = config.getValue("mqtt.broker.password", String.class);
    private final String trustStorePath = config.getValue("mqtt.broker.truststore.path", String.class);
    private final String trustStorePassword = config.getValue("mqtt.broker.truststore.password", String.class);

    private IMqttAsyncClient mqttClient;

    @Inject
    private ThingManager thingManager;
    @Inject
    private AlertManager alertManager;
    // Instance<> casse le cycle MqttManager -> CommandManager -> MqttManager.
    @Inject
    private Instance<CommandManager> commandManager;

    @PostConstruct
    public void init() {
        try {
            var ks = KeyStore.getInstance("JKS");
            try (var fis = new FileInputStream(trustStorePath)) {
                ks.load(fis, trustStorePassword.toCharArray());
            }
            var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            var tlsContext = SSLContext.getInstance("TLSv1.3");
            tlsContext.init(null, tmf.getTrustManagers(), null);

            var clientId = "%s#%s".formatted(configuredClientId, UUID.randomUUID());
            mqttClient = new MqttAsyncClient(mqttBrokerUrl, clientId, new MemoryPersistence());
            mqttClient.setCallback(this); // avant connect() : sinon connectComplete n'est pas appelé

            final var options = new MqttConnectionOptions();
            options.setCleanStart(true);
            options.setUserName(mqttUsername);
            options.setPassword(mqttPassword.getBytes(StandardCharsets.UTF_8));
            options.setAutomaticReconnect(true);
            options.setSocketFactory(tlsContext.getSocketFactory());

            mqttClient.connect(options).waitForCompletion();
        } catch (Exception e) {
            throw new RuntimeException("MQTT Client Initialisation Failed!", e);
        }
    }

    @PreDestroy
    public void cleanUp() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException ignore) {
            }
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public void publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException {
        mqttClient.publish(topic, new MqttMessage(payload, qos, retained, null));
    }

    // ---------- MqttCallback ----------

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            for (var filter : new String[]{Topics.SUB_STATUS, Topics.SUB_DATUM, Topics.SUB_ACK, Topics.SUB_ALERTS}) {
                mqttClient.subscribe(filter, 1);
            }
            LOG.info("MQTT connecté (" + serverURI + "), abonnements faits" + (reconnect ? " (reconnexion)" : ""));
        } catch (MqttException e) {
            LOG.log(Level.SEVERE, "MQTT : abonnement impossible", e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var parts = topic.split("/");
        var payload = message.getPayload();
        try {
            if (parts.length == 3 && "things".equals(parts[0])) {
                switch (parts[2]) {
                    case Topics.STATUS -> thingManager.updateStatus(parts[1], new String(payload, StandardCharsets.UTF_8));
                    case Topics.DATUM -> thingManager.updateDatum(parts[1], payload);
                    case Topics.ACK -> commandManager.get().onAck(parts[1], payload);
                    default -> LOG.fine("Topic ignoré : " + topic);
                }
            } else if (parts.length == 3 && "alerts".equals(parts[0])) {
                alertManager.onAlert(parts[1], parts[2], payload);
            } else {
                LOG.fine("Topic hors contrat ignoré : " + topic);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Message MQTT invalide sur " + topic, e);
        }
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        LOG.warning("MQTT déconnecté : " + disconnectResponse.getReasonString());
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LOG.log(Level.WARNING, "Erreur MQTT", exception);
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
    }
}
