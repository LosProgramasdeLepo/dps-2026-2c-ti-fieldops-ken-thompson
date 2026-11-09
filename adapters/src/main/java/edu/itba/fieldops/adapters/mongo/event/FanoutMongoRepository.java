package edu.itba.fieldops.adapters.mongo.event;

import org.springframework.data.mongodb.repository.MongoRepository;

interface FanoutMongoRepository extends MongoRepository<FanoutDocument, String> {
}
