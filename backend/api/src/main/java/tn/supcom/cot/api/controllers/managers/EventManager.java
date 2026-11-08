package tn.supcom.cot.api.controllers.managers;

import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.paho.mqttv5.common.MqttException;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Singleton
@LocalBean
@Startup
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@ApplicationScoped
public class EventManager {
    @Inject
    @MessageEvent(MessageEvent.Type.WEBSOCKET)
    private Event<JsonObject>   websocketEvent;
    @Inject
    @MessageEvent(MessageEvent.Type.MQTT)
    private Event<JsonObject> mqttEvent;

    //For Transaction Management
    @Resource
    private SessionContext sessionContext;

    @Inject
    private MqttManager mqttManager;


    public void publishFromWebSocket(JsonObject message,String view) throws MqttException {
        var msg = Json.createObjectBuilder(message).add("view",view).build();
        websocketEvent.fireAsync(msg);
    }

    public void publishFromMqtt(byte[] payload) throws MqttException {
        var msg = Json.createReader(new StringReader(new String(payload, StandardCharsets.UTF_8))).readObject();
        mqttEvent.fireAsync(msg);
    }

    public void consumeWebSocket(@ObservesAsync @MessageEvent(MessageEvent.Type.WEBSOCKET) JsonObject message) throws MqttException {
        //business logic
    }

    public void consumeMqtt(@ObservesAsync @MessageEvent(MessageEvent.Type.MQTT) JsonObject message) throws MqttException {
        //business logic
    }

}

