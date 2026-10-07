package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.catalog.PersonnelController;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.identity.CertificationId;
import edu.itba.fieldops.usecase.catalog.AdministerPersonnel;
import edu.itba.fieldops.usecase.catalog.ConsultPersonnel;
import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonnelController.class)
class PaginationTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdministerPersonnel personnel;

    @MockitoBean
    private ConsultPersonnel consult;

    @Test
    void defaultsToTheFirstPageOfTwentyItems() throws Exception {
        PageRequest request = new PageRequest(0, 20);
        when(consult.certifications(request)).thenReturn(new Page<>(List.of(), request, 0));

        mvc.perform(get("/v1/certifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(header().string("Link",
                        "<http://localhost/v1/certifications?page=0&size=20>; rel=\"first\", "
                                + "<http://localhost/v1/certifications?page=0&size=20>; rel=\"last\""));
    }

    @Test
    void describesAMiddlePageAndLinksItsNeighbours() throws Exception {
        PageRequest request = new PageRequest(1, 2);
        when(consult.certifications(request)).thenReturn(new Page<>(List.of(certification("Diving"), certification("Night operation")), request, 5));

        mvc.perform(get("/v1/certifications").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(header().string("Link",
                        "<http://localhost/v1/certifications?page=0&size=2>; rel=\"first\", "
                                + "<http://localhost/v1/certifications?page=0&size=2>; rel=\"prev\", "
                                + "<http://localhost/v1/certifications?page=2&size=2>; rel=\"next\", "
                                + "<http://localhost/v1/certifications?page=2&size=2>; rel=\"last\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=first", "size=0", "size=101", "size=many"})
    void rejectsAnInvalidPageQuery(String query) throws Exception {
        mvc.perform(get("/v1/certifications?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static Certification certification(String name) {
        return new Certification(new CertificationId(UUID.randomUUID()), name);
    }
}
