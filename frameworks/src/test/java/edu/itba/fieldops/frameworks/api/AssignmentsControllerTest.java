package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.expedition.AssignmentsController;
import edu.itba.fieldops.domain.expedition.InvalidAssignment;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.identity.ActivityId;
import edu.itba.fieldops.domain.identity.ExpeditionId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.usecase.expedition.AssignResources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignmentsController.class)
class AssignmentsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AssignResources assignments;

    @Test
    void assignsAPersonAndAPermit() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        mvc.perform(post("/v1/expeditions/{id}/assignments", expeditionId).contentType(MediaType.APPLICATION_JSON).content("""
                {"type":"PERSON","activityId":"%s","personId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNoContent());
        mvc.perform(put("/v1/expeditions/{id}/permits/{permitId}", expeditionId, UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsSuggestions() throws Exception {
        UUID expeditionId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        when(assignments.suggest(new ExpeditionId(expeditionId)))
                .thenReturn(List.of(new PersonAssignment(new ActivityId(activityId), new PersonId(personId))));

        mvc.perform(get("/v1/expeditions/{id}/assignment-suggestions", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PERSON"))
                .andExpect(jsonPath("$[0].activityId").value(activityId.toString()))
                .andExpect(jsonPath("$[0].personId").value(personId.toString()));
    }

    @Test
    void aRepeatedAssignmentIsUnprocessable() throws Exception {
        doThrow(new InvalidAssignment("assignment already filed")).when(assignments).addAssignment(any(), any());

        mvc.perform(post("/v1/expeditions/{id}/assignments", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("""
                {"type":"PERSON","activityId":"%s","personId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }
}
