package edu.itba.fieldops.adapters.mongo.catalog;

import org.springframework.data.mongodb.repository.MongoRepository;

interface PermitMongoRepository extends MongoRepository<PermitDocument, String> {
}
