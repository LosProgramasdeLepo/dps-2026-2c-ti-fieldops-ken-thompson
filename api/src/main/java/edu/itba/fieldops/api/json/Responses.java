package edu.itba.fieldops.api.json;

import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.UUID;

public final class Responses {
    private Responses() {
    }

    public static ResponseEntity<ResourceIdResponse> created(String collection, UUID id) {
        return ResponseEntity.created(URI.create(collection + "/" + id)).body(new ResourceIdResponse(id));
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
