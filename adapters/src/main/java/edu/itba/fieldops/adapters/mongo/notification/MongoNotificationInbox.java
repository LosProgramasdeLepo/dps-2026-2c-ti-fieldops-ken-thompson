package edu.itba.fieldops.adapters.mongo.notification;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.mongo.RegistrationSequence;
import edu.itba.fieldops.adapters.notification.NotificationInbox;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
@Profile(PersistenceProfiles.MONGO)
@Transactional(readOnly = true)
public class MongoNotificationInbox implements NotificationInbox {
    private static final String RECIPIENT = "id.personId";

    private final NotificationMongoRepository notifications;
    private final MongoOperations operations;
    private final RegistrationSequence registrations;

    MongoNotificationInbox(
            NotificationMongoRepository notifications,
            MongoOperations operations,
            RegistrationSequence registrations
    ) {
        this.notifications = Objects.requireNonNull(notifications, "notifications");
        this.operations = Objects.requireNonNull(operations, "operations");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        createIndexes();
    }

    @Override
    @Transactional
    public void keep(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        if (!notifications.existsById(NotificationDocument.keyOf(notification))) {
            notifications.save(NotificationDocument.of(notification, registrations.next(NotificationDocument.COLLECTION)));
        }
    }

    @Override
    public Page<Notification> of(PersonId recipient, PageRequest request) {
        Criteria ofRecipient = Criteria.where(RECIPIENT).is(Objects.requireNonNull(recipient, "recipient").value().toString());
        long total = operations.count(Query.query(ofRecipient), NotificationDocument.class);
        Query page = Query.query(ofRecipient)
                .with(Sort.by(Sort.Direction.DESC, "registrationOrder"))
                .skip(request.offset())
                .limit(request.size());
        return new Page<>(
                operations.find(page, NotificationDocument.class).stream().map(NotificationDocument::toDomain).toList(),
                request,
                total
        );
    }

    private void createIndexes() {
        operations.indexOps(NotificationDocument.class).createIndex(new Index()
                .on("_id.personId", Sort.Direction.ASC)
                .on("registrationOrder", Sort.Direction.DESC)
                .named("inbox"));
    }
}
