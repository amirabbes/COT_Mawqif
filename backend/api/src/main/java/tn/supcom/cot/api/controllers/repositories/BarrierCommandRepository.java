package tn.supcom.cot.api.controllers.repositories;

import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.supcom.cot.api.entities.BarrierCommand;

import java.util.Date;

@Dependent
public class BarrierCommandRepository extends AbstractMongoRepository<BarrierCommand> {

    @Inject
    public BarrierCommandRepository(MongoClient client) {
        super(client, "barrier_commands");
    }

    @Override
    protected BarrierCommand fromDocument(Document document) {
        var command = new BarrierCommand();
        restoreBaseFields(command, document);
        command.setThingId(document.getString("thingId"));
        command.setParkingId(document.getString("parkingId"));
        command.setCommand(document.getString("command"));
        command.setReason(document.getString("reason"));
        command.setStatus(document.getString("status"));
        var issuedAt = document.getDate("issuedAt");
        command.setIssuedAt(issuedAt == null ? null : issuedAt.toInstant());
        var expiresAt = document.getDate("expiresAt");
        command.setExpiresAt(expiresAt == null ? null : expiresAt.toInstant());
        command.setSig(document.getString("sig"));
        return command;
    }

    @Override
    protected Document toDocument(BarrierCommand command) {
        return baseDocument(command)
                .append("thingId", command.getThingId())
                .append("parkingId", command.getParkingId())
                .append("command", command.getCommand())
                .append("reason", command.getReason())
                .append("status", command.getStatus())
                .append("issuedAt", command.getIssuedAt() == null ? null : Date.from(command.getIssuedAt()))
                .append("expiresAt", command.getExpiresAt() == null ? null : Date.from(command.getExpiresAt()))
                .append("sig", command.getSig());
    }
}
