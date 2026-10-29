package fr.guezrou.ecommerce.order.event.publisher;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.guezrou.ecommerce.db.repository.OutboxEventEntity;
import fr.guezrou.ecommerce.db.repository.OutboxEventRepository;
import fr.guezrou.ecommerce.order.event.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {

        List<OutboxEventEntity> events = outboxEventRepository.findTop100ByPublishedFalseOrderByIdAsc();

        for (OutboxEventEntity event : events) {

            try {
                Object message = deserializeEvent(event);

                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), message).get(10, TimeUnit.SECONDS);

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));

                outboxEventRepository.save(event);

                log.info("Événement Outbox publié : id={}, type={}, topic={}, key={}", event.getId(),
                        event.getEventType(), event.getTopic(), event.getMessageKey());

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Publication Outbox interrompue : id={}", event.getId(), exception);
                return;

            } catch (Exception exception) {
                log.error("Échec de publication Outbox : id={}, type={}. L'événement restera en attente.",
                        event.getId(), event.getEventType(), exception);
            }
        }
    }

    private Object deserializeEvent(OutboxEventEntity event) throws JsonProcessingException {

        if ("OrderCreated".equals(event.getEventType())) {
            return objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
        }

        throw new IllegalArgumentException("Type d'événement Outbox non supporté : " + event.getEventType());
    }
}