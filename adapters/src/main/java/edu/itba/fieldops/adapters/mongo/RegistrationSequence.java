package edu.itba.fieldops.adapters.mongo;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.SessionSynchronization;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Profile(PersistenceProfiles.MONGO)
public class RegistrationSequence {
    private final MongoTemplate outsideTransactions;

    public RegistrationSequence(MongoDatabaseFactory databases) {
        this.outsideTransactions = new MongoTemplate(Objects.requireNonNull(databases, "databases"));
        this.outsideTransactions.setSessionSynchronization(SessionSynchronization.NEVER);
    }

    public long next(String collection) {
        Counter counter = outsideTransactions.findAndModify(
                Query.query(Criteria.where("_id").is(collection)),
                new Update().inc("value", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Counter.class
        );
        return Objects.requireNonNull(counter, "registration counter").value();
    }

    @Document(collection = "registration_sequences")
    record Counter(@Id String id, long value) {
    }
}
