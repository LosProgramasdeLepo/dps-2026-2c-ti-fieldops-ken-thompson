package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.catalog.EquipmentController;
import edu.itba.fieldops.domain.identity.ConsumableId;
import edu.itba.fieldops.domain.identity.InstrumentId;
import edu.itba.fieldops.domain.identity.VehicleId;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.catalog.AdministerEquipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquipmentController.class)
class EquipmentControllerTest {
    private static final String AVAILABILITY = "{\"periods\":[{\"start\":\"2026-11-01T00:00:00Z\",\"end\":\"2026-11-06T00:00:00Z\"}]}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdministerEquipment equipment;

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
    void changesStock() throws Exception {
        mvc.perform(put("/v1/consumables/{id}/stock", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":3}"))
                .andExpect(status().isNoContent());
    }
}
