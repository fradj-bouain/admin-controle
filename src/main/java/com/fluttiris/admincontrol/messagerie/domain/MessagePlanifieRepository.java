package com.fluttiris.admincontrol.messagerie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessagePlanifieRepository extends JpaRepository<MessagePlanifie, UUID> {

    List<MessagePlanifie> findByStatutAndDateEnvoiPrevueLessThanEqualOrderByDateEnvoiPrevue(StatutMessagePlanifie statut,
                                                                                              Instant dateEnvoiPrevue);

    boolean existsByRegleIdAndSourceEntityId(UUID regleId, UUID sourceEntityId);

    List<MessagePlanifie> findAllByOrderByCreatedAtDesc();

    List<MessagePlanifie> findBySourceEntityIdInOrderByCreatedAtDesc(List<UUID> sourceEntityIds);
}
