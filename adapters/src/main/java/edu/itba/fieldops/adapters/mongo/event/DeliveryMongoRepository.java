package edu.itba.fieldops.adapters.mongo.event;

import org.springframework.data.mongodb.repository.MongoRepository;

interface DeliveryMongoRepository extends MongoRepository<DeliveryDocument, DeliveryDocument.Key> {
}
