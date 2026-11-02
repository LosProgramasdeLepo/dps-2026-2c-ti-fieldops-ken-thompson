package edu.itba.fieldops.adapters.mongo.tracking;

import org.springframework.data.mongodb.repository.MongoRepository;

interface RunMongoRepository extends MongoRepository<RunDocument, String> {
}
