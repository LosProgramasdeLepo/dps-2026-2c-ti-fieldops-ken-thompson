package edu.itba.fieldops.adapters.mongo.catalog;

import org.springframework.data.mongodb.repository.MongoRepository;

interface PersonMongoRepository extends MongoRepository<PersonDocument, String> {
}
