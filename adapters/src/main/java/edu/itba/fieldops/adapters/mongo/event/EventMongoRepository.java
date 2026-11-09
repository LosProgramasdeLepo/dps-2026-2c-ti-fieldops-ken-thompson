package edu.itba.fieldops.adapters.mongo.event;

import org.springframework.data.mongodb.repository.MongoRepository;

interface EventMongoRepository extends MongoRepository<EventDocument, String> {
}
