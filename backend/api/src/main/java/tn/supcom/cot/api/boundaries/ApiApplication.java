package tn.supcom.cot.api.boundaries;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** WildFly publie api.war sous /api : les routes REST sont donc sous /api/v1 (voir openapi.yaml). */
@ApplicationPath("/v1")
public class ApiApplication extends Application {
}
