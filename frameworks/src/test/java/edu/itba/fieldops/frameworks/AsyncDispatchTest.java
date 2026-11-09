package edu.itba.fieldops.frameworks;

import edu.itba.fieldops.adapters.PersistenceProfiles;
import edu.itba.fieldops.adapters.messaging.EventConsumer;
import edu.itba.fieldops.domain.event.DeliveryStatus;
import edu.itba.fieldops.domain.event.DomainEvent;
import edu.itba.fieldops.domain.event.IncidentReported;
import edu.itba.fieldops.domain.event.RecordedEvent;
import edu.itba.fieldops.domain.identity.IncidentId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "fieldops.events.dispatcher.enabled=true",
        "fieldops.events.dispatch-delay=20ms"
})
@ActiveProfiles(PersistenceProfiles.MEMORY)
@AutoConfigureMockMvc
@Import({UseCaseFixture.FixedTime.class, AsyncDispatchTest.Blocking.class})
class AsyncDispatchTest extends UseCaseFixture {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private BlockingConsumer blocking;

    @AfterEach
    void release() {
        blocking.release.countDown();
    }

    @Test
    @Timeout(30)
    void theRequestAnswersWhileTheConsumerIsStillBlocked() throws Exception {
        Sampling sampling = runningSampling();

        String location = mvc.perform(post("/v1/expeditions/{id}/incidents", sampling.expeditionId().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"storm on site\",\"activityId\":\"" + sampling.activityId().value() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        IncidentId incidentId = new IncidentId(UUID.fromString(location.substring(location.lastIndexOf('/') + 1)));

        assertTrue(blocking.entered.await(5, TimeUnit.SECONDS));
        assertAll(
                () -> assertEquals(1, blocking.release.getCount()),
                () -> assertTrue(incidents.incident(sampling.expeditionId(), incidentId).proposal().isEmpty())
        );

        blocking.release.countDown();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertEquals(
                java.util.List.of(DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERED),
                incidents.incident(sampling.expeditionId(), incidentId).processing().stream().map(delivery -> delivery.status()).toList()
        ));
        assertTrue(incidents.incident(sampling.expeditionId(), incidentId).proposal().isPresent());
    }

    static final class BlockingConsumer implements EventConsumer {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public String name() {
            return "a-blocking-consumer";
        }

        @Override
        public boolean subscribesTo(DomainEvent event) {
            return event instanceof IncidentReported;
        }

        @Override
        public void consume(RecordedEvent event) {
            entered.countDown();
            try {
                if (!release.await(20, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the test never released the consumer");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Blocking {
        @Bean
        BlockingConsumer blockingConsumer() {
            return new BlockingConsumer();
        }
    }
}
