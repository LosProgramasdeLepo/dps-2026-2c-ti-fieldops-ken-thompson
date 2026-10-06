package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.catalog.PersonnelController;
import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.domain.identity.PersonId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.usecase.catalog.ConsultPersonnel;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonnelController.class)
class PersonnelControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdministerPersonnel personnel;

    @MockitoBean
    private ConsultPersonnel consult;

    @Test
    void registersACertification() throws Exception {
        UUID id = UUID.randomUUID();
        when(personnel.registerCertification("Sampling")).thenReturn(new CertificationId(id));

        mvc.perform(post("/v1/certifications").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sampling\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/certifications/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void rejectsABlankCertificationName() throws Exception {
        mvc.perform(post("/v1/certifications").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsAnUnknownProperty() throws Exception {
        mvc.perform(post("/v1/certifications").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sampling\",\"extra\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registersAPerson() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID certificationId = UUID.randomUUID();
        when(personnel.registerPerson(eq("Ada"), any(), any())).thenReturn(new PersonId(personId));

        mvc.perform(post("/v1/people").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Ada","certifications":["%s"],"availability":{"periods":[{"start":"2026-11-01T00:00:00Z","end":"2026-11-06T00:00:00Z"}]}}
                """.formatted(certificationId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/people/" + personId));
    }

    @Test
    void certifiesAPerson() throws Exception {
        mvc.perform(put("/v1/people/{id}/certifications/{certificationId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void listsCertifications() throws Exception {
        UUID id = UUID.randomUUID();
        PageRequest request = new PageRequest(0, 20);
        when(consult.certifications(request)).thenReturn(new Page<>(List.of(new Certification(new CertificationId(id), "Diving")), request, 1));

        mvc.perform(get("/v1/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Diving"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void readsAPersonWithCertificationsAndAvailability() throws Exception {
        UUID id = UUID.randomUUID();
        Certification sampling = new Certification(new CertificationId(UUID.randomUUID()), "Sampling");
        TimePeriod period = new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-06T00:00:00Z"));
        when(consult.person(new PersonId(id))).thenReturn(new Person(new PersonId(id), "Ada", List.of(sampling), new Availability(List.of(period))));

        mvc.perform(get("/v1/people/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.certifications[0].name").value("Sampling"))
                .andExpect(jsonPath("$.availability.periods[0].start").value("2026-11-01T00:00:00Z"));
    }

    @Test
    void anUnknownPersonIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(consult.person(new PersonId(id))).thenThrow(new UnknownResource("person", id.toString()));

        mvc.perform(get("/v1/people/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void aDuplicateCertificationIsUnprocessable() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidValue("person already holds certification"))
                .when(personnel).certify(any(), any());

        mvc.perform(put("/v1/people/{id}/certifications/{certificationId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
