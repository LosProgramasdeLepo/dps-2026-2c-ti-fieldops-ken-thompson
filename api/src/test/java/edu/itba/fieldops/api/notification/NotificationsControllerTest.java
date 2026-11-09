package edu.itba.fieldops.api.notification;

import edu.itba.fieldops.domain.event.EventType;
import edu.itba.fieldops.domain.identity.EventId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.notification.Notification;
import edu.itba.fieldops.domain.shared.Page;
import edu.itba.fieldops.domain.shared.PageRequest;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.usecase.notification.ConsultNotifications;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationsController.class)
class NotificationsControllerTest {
    private static final Instant SENT_AT = Instant.parse("2026-11-01T08:00:00Z");
    private static final PersonId ADA = new PersonId(UUID.randomUUID());

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ConsultNotifications notifications;

    @Test
    void listsTheInboxOfAPerson() throws Exception {
        EventId event = new EventId(UUID.randomUUID());
        ExpeditionId plan = new ExpeditionId(UUID.randomUUID());
        PageRequest request = new PageRequest(0, 20);
        when(notifications.of(ADA, request)).thenReturn(new Page<>(
                List.of(new Notification(event, ADA, new EventType("ExpeditionApproved"), plan, SENT_AT)), request, 1
        ));

        mvc.perform(get("/v1/people/{id}/notifications", ADA.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventId").value(event.value().toString()))
                .andExpect(jsonPath("$.items[0].type").value("ExpeditionApproved"))
                .andExpect(jsonPath("$.items[0].expeditionId").value(plan.value().toString()))
                .andExpect(jsonPath("$.items[0].sentAt").value("2026-11-01T08:00:00Z"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void passesThePageItWasAskedFor() throws Exception {
        PageRequest request = new PageRequest(2, 5);
        when(notifications.of(ADA, request)).thenReturn(new Page<>(List.of(), request, 11));

        mvc.perform(get("/v1/people/{id}/notifications?page=2&size=5", ADA.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(header().exists("Link"));
    }

    @Test
    void theInboxOfAnUnknownPersonIs404() throws Exception {
        UUID ghost = UUID.randomUUID();
        when(notifications.of(new PersonId(ghost), new PageRequest(0, 20)))
                .thenThrow(new UnknownResource("person", ghost.toString()));

        mvc.perform(get("/v1/people/{id}/notifications", ghost)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "size=many"})
    void rejectsAnInvalidPage(String query) throws Exception {
        mvc.perform(get("/v1/people/{id}/notifications?" + query, ADA.value())).andExpect(status().isBadRequest());
    }
}
