package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.catalog.EquipmentController;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.InstrumentKind;
import edu.itba.fieldops.domain.shared.Passengers;
import edu.itba.fieldops.domain.shared.Stock;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import edu.itba.fieldops.usecase.catalog.ConsultEquipment;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentController.class)
class EquipmentControllerTest {
    private static final String AVAILABILITY = "{\"periods\":[{\"start\":\"2026-11-01T00:00:00Z\",\"end\":\"2026-11-06T00:00:00Z\"}]}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdministerEquipment equipment;

    @MockitoBean
    private ConsultEquipment consult;

    @Test
    void registersAVehicleAnInstrumentAndAConsumable() throws Exception {
        UUID vehicle = UUID.randomUUID();
        UUID instrument = UUID.randomUUID();
        UUID consumable = UUID.randomUUID();
        when(equipment.registerVehicle(any(), any())).thenReturn(new VehicleId(vehicle));
        when(equipment.registerInstrument(any(), any())).thenReturn(new InstrumentId(instrument));
        when(equipment.registerConsumable(any(), any())).thenReturn(new ConsumableId(consumable));

        mvc.perform(post("/v1/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capacity\":4,\"availability\":" + AVAILABILITY + "}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/vehicles/" + vehicle));
        mvc.perform(post("/v1/instruments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"lighting\",\"availability\":" + AVAILABILITY + "}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/instruments/" + instrument));
        mvc.perform(post("/v1/consumables").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vials\",\"stock\":5}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/consumables/" + consumable));
    }

    @Test
    void anUnknownVehicleInThePathIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new UnknownResource("vehicle", id.toString())).when(equipment).changeAvailability(eq(new VehicleId(id)), any());

        mvc.perform(put("/v1/vehicles/{id}/availability", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":" + AVAILABILITY + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsEachKindOfEquipment() throws Exception {
        UUID vehicle = UUID.randomUUID();
        UUID instrument = UUID.randomUUID();
        UUID consumable = UUID.randomUUID();
        PageRequest request = new PageRequest(0, 20);
        when(consult.vehicles(request)).thenReturn(page(new Vehicle(new VehicleId(vehicle), new Passengers(4), Availability.always()), request));
        when(consult.instruments(request)).thenReturn(page(new Instrument(new InstrumentId(instrument), InstrumentKind.LIGHTING, Availability.always()), request));
        when(consult.consumables(request)).thenReturn(page(new Consumable(new ConsumableId(consumable), "Vials", new Stock(5)), request));

        mvc.perform(get("/v1/vehicles")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(vehicle.toString()));
        mvc.perform(get("/v1/instruments")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(instrument.toString()));
        mvc.perform(get("/v1/consumables")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(consumable.toString()));
    }

    @Test
    void readsAVehicleAnInstrumentAndAConsumable() throws Exception {
        UUID vehicle = UUID.randomUUID();
        UUID instrument = UUID.randomUUID();
        UUID consumable = UUID.randomUUID();
        when(consult.vehicle(new VehicleId(vehicle))).thenReturn(new Vehicle(new VehicleId(vehicle), new Passengers(4), Availability.always()));
        when(consult.instrument(new InstrumentId(instrument))).thenReturn(new Instrument(new InstrumentId(instrument), InstrumentKind.LIGHTING, Availability.always()));
        when(consult.consumable(new ConsumableId(consumable))).thenReturn(new Consumable(new ConsumableId(consumable), "Vials", new Stock(5)));

        mvc.perform(get("/v1/vehicles/{id}", vehicle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(4))
                .andExpect(jsonPath("$.availability.periods[0].end").value("9999-12-31T00:00:00Z"));
        mvc.perform(get("/v1/instruments/{id}", instrument))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("lighting"));
        mvc.perform(get("/v1/consumables/{id}", consumable))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vials"))
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void changesStock() throws Exception {
        mvc.perform(put("/v1/consumables/{id}/stock", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":3}"))
                .andExpect(status().isNoContent());
    }

    private static <T> Page<T> page(T item, PageRequest request) {
        return new Page<>(List.of(item), request, 1);
    }
}
