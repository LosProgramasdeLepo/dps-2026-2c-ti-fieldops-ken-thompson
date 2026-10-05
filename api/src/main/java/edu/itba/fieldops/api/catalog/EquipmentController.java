package edu.itba.fieldops.api.catalog;

import edu.itba.fieldops.api.catalog.CatalogRequests.AvailabilityChangeRequest;
import edu.itba.fieldops.api.catalog.CatalogRequests.RegisterConsumableRequest;
import edu.itba.fieldops.api.catalog.CatalogRequests.RegisterInstrumentRequest;
import edu.itba.fieldops.api.catalog.CatalogRequests.RegisterVehicleRequest;
import edu.itba.fieldops.api.catalog.CatalogRequests.StockChangeRequest;
import edu.itba.fieldops.api.json.Periods;
import edu.itba.fieldops.api.json.ResourceIdResponse;
import edu.itba.fieldops.api.json.Responses;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public final class EquipmentController {
    private final AdministerEquipment equipment;

    public EquipmentController(AdministerEquipment equipment) {
        this.equipment = equipment;
    }

    @PostMapping("/v1/vehicles")
    public ResponseEntity<ResourceIdResponse> registerVehicle(@Valid @RequestBody RegisterVehicleRequest request) {
        VehicleId id = equipment.registerVehicle(new Passengers(request.capacity()), Periods.toAvailability(request.availability()));
        return Responses.created("/v1/vehicles", id.value());
    }

    @PostMapping("/v1/instruments")
    public ResponseEntity<ResourceIdResponse> registerInstrument(@Valid @RequestBody RegisterInstrumentRequest request) {
        InstrumentId id = equipment.registerInstrument(new InstrumentKind(request.kind()), Periods.toAvailability(request.availability()));
        return Responses.created("/v1/instruments", id.value());
    }

    @PostMapping("/v1/consumables")
    public ResponseEntity<ResourceIdResponse> registerConsumable(@Valid @RequestBody RegisterConsumableRequest request) {
        ConsumableId id = equipment.registerConsumable(request.name(), new Stock(request.stock()));
        return Responses.created("/v1/consumables", id.value());
    }

    @PutMapping("/v1/vehicles/{id}/availability")
    public ResponseEntity<Void> changeVehicleAvailability(@PathVariable UUID id, @Valid @RequestBody AvailabilityChangeRequest request) {
        equipment.changeAvailability(new VehicleId(id), Periods.toAvailability(request.availability()));
        return Responses.noContent();
    }

    @PutMapping("/v1/instruments/{id}/availability")
    public ResponseEntity<Void> changeInstrumentAvailability(@PathVariable UUID id, @Valid @RequestBody AvailabilityChangeRequest request) {
        equipment.changeAvailability(new InstrumentId(id), Periods.toAvailability(request.availability()));
        return Responses.noContent();
    }

    @PutMapping("/v1/consumables/{id}/stock")
    public ResponseEntity<Void> changeStock(@PathVariable UUID id, @Valid @RequestBody StockChangeRequest request) {
        equipment.changeStock(new ConsumableId(id), new Stock(request.stock()));
        return Responses.noContent();
    }
}
