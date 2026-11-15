package tn.supcom.cot.iam.boundaries;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Service d'identité (iam-1.0.war). En développement il est publié sous /iam (routes : /iam/v1/auth/token...) ;
 * avec les hôtes virtuels de l'enseignant il est à la racine de iam.&lt;domaine&gt; (routes : /v1/auth/token...).
 */
@ApplicationPath("/v1")
public class IamApplication extends Application {
}
