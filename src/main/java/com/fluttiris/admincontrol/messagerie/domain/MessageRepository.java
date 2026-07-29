package com.fluttiris.admincontrol.messagerie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByDestinataireTypeAndDestinataireIdOrderByCreatedAtDesc(DestinataireType type, UUID destinataireId);

    List<Message> findByExpediteurUtilisateurIdOrderByCreatedAtDesc(UUID expediteurId);
}
