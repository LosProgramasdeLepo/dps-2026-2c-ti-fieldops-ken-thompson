package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.PersonName;
import edu.itba.fieldops.domain.expedition.ExpeditionCharter;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.domain.usecase.expedition.DraftExpedition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cloud.config.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles(PersistenceProfiles.MEMORY)
class DynamicConfigurationTest {
    private static final TimePeriod WEEK = new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-08T00:00:00Z"));

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ConfigurableApplicationContext context;
    @Autowired
    private AdministerPersonnel personnel;
    @Autowired
    private DraftExpedition drafts;

    @AfterEach
    void restoreDefaults() {
        change(Map.of(
                "fieldops.features.assignment-suggestions", "true",
                "fieldops.maintenance.enabled", "false"
        ));
    }

    @Test
    void assignmentSuggestionsAreTurnedOffAndBackOnWithoutRestarting() throws Exception {
        ExpeditionId expedition = draft();
        mvc.perform(get("/v1/expeditions/{id}/assignment-suggestions", expedition.value())).andExpect(status().isOk());

        change(Map.of("fieldops.features.assignment-suggestions", "false"));

        mvc.perform(get("/v1/expeditions/{id}/assignment-suggestions", expedition.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Assignment suggestions are disabled"));

        change(Map.of("fieldops.features.assignment-suggestions", "true"));

        mvc.perform(get("/v1/expeditions/{id}/assignment-suggestions", expedition.value())).andExpect(status().isOk());
    }

    @Test
    void maintenanceRejectsTheApiButNotActuatorUntilItIsTurnedOff() throws Exception {
        change(Map.of(
                "fieldops.maintenance.enabled", "true",
                "fieldops.maintenance.message", "Migrating the expedition database",
                "fieldops.maintenance.retry-after-seconds", "120"
        ));

        mvc.perform(get("/v1/people"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "120"))
                .andExpect(jsonPath("$.detail").value("Migrating the expedition database"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());

        change(Map.of("fieldops.maintenance.enabled", "false"));

        mvc.perform(get("/v1/people")).andExpect(status().isOk());
    }

    private ExpeditionId draft() {
        PersonId ada = personnel.registerPerson(new PersonName("Ada"), List.of(), Availability.always());
        return drafts.draft(new ExpeditionCharter(
                List.of(new Objective("Map the wetland")),
                WEEK,
                List.of(new WorkZone("Delta")),
                List.of(ada),
                List.of()
        ));
    }

    private void change(Map<String, String> properties) {
        TestPropertyValues.of(properties).applyTo(context);
        context.publishEvent(new EnvironmentChangeEvent(context, properties.keySet()));
    }
}
