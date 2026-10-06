package edu.itba.fieldops.frameworks.api;

import edu.itba.fieldops.api.catalog.PermitsController;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.identity.PermitId;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.PermitKind;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.usecase.catalog.AdministerPermits;
import edu.itba.fieldops.usecase.catalog.ConsultPermits;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermitsController.class)
class PermitsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdministerPermits permits;

    @MockitoBean
    private ConsultPermits consult;

    @Test
    void listsAndReadsPermits() throws Exception {
        UUID id = UUID.randomUUID();
        PageRequest request = new PageRequest(0, 20);
        TimePeriod validity = new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-06T00:00:00Z"));
        Permit permit = new Permit(new PermitId(id), new WorkZone("Delta"), validity, PermitKind.NIGHT);
        when(consult.permits(request)).thenReturn(new Page<>(List.of(permit), request, 1));
        when(consult.permit(new PermitId(id))).thenReturn(permit);

        mvc.perform(get("/v1/permits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].kind").value("night operation"));
        mvc.perform(get("/v1/permits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("Delta"))
                .andExpect(jsonPath("$.validity.end").value("2026-11-06T00:00:00Z"));
    }

    @Test
    void registersAPermit() throws Exception {
        UUID id = UUID.randomUUID();
        when(permits.registerPermit(any(), any(), any())).thenReturn(new PermitId(id));

        mvc.perform(post("/v1/permits").contentType(MediaType.APPLICATION_JSON).content("""
                {"kind":"zone","zone":"Delta","validity":{"start":"2026-11-01T00:00:00Z","end":"2026-11-06T00:00:00Z"}}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v1/permits/" + id));
    }

    @Test
    void aPermitWithAnInvertedPeriodIsUnprocessable() throws Exception {
        when(permits.registerPermit(any(), any(), any())).thenThrow(new InvalidValue("period end must not be before start"));

        mvc.perform(post("/v1/permits").contentType(MediaType.APPLICATION_JSON).content("""
                {"kind":"zone","zone":"Delta","validity":{"start":"2026-11-06T00:00:00Z","end":"2026-11-01T00:00:00Z"}}
                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
