package edu.itba.fieldops.adapters.mongo.catalog;

import org.springframework.data.mongodb.repository.MongoRepository;

interface ConsumableMongoRepository extends MongoRepository<ConsumableDocument, String> {
}
