package edu.itba.fieldops.adapters.mongo;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Profile(PersistenceProfiles.MONGO)
public class RegistrationSequence {
    private final MongoOperations operations;

    public RegistrationSequence(MongoOperations operations) {
        this.operations = Objects.requireNonNull(operations, "operations");
    }

    public long next(String collection) {
        Counter counter = operations.findAndModify(
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
