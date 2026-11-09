package edu.itba.fieldops.adapters.mongo.notification;

import org.springframework.data.mongodb.repository.MongoRepository;

interface NotificationMongoRepository extends MongoRepository<NotificationDocument, NotificationDocument.Key> {
}
