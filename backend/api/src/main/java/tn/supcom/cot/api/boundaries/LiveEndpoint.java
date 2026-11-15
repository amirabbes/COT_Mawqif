package tn.supcom.cot.api.boundaries;

import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import tn.supcom.cot.api.controllers.managers.WebSocketManager;

import java.io.IOException;

/** ws://localhost:8081/api/ws/{driver|admin} : le serveur envoie, le client écoute. */
@ServerEndpoint("/ws/{view}")
public class LiveEndpoint {

    @Inject
    private WebSocketManager manager;
    @Inject
    private tn.supcom.cot.core.security.TokenVerifier iam;

    @OnOpen
    public void onOpen(Session session, @PathParam("view") String view) throws IOException {
        if (!WebSocketManager.DRIVER.equals(view) && !WebSocketManager.ADMIN.equals(view)) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "vue inconnue : " + view));
            return;
        }
        if (WebSocketManager.ADMIN.equals(view) && iam.enabled()) {
            var tokens = session.getRequestParameterMap().get("access_token");
            var claims = tokens == null || tokens.isEmpty() ? java.util.Optional.<tn.supcom.cot.core.security.JwtService.Claims>empty()
                    : iam.authenticate(tokens.get(0));
            if (claims.isEmpty() || !claims.get().hasAnyRole(tn.supcom.cot.core.security.Role.OPERATOR, tn.supcom.cot.core.security.Role.ADMIN)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "authentification requise"));
                return;
            }
        }
        manager.register(view, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("view") String view) {
        manager.unregister(view, session);
    }
}
