package com.fluttiris.admincontrol.messagerie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByDestinataireTypeAndDestinataireIdOrderByCreatedAtDesc(DestinataireType type, UUID destinataireId);

    // Historique des messages pour UN chantier précis (voir MessageService.historique) — même
    // principe que les documents/l'historique de documents : un message composé depuis le
    // contexte d'un chantier ne doit apparaître, une fois filtré, que dans l'historique de CE
    // chantier-là, pas mélangé avec ceux des autres chantiers de la même entreprise/du même client.
    List<Message> findByDestinataireTypeAndDestinataireIdAndChantierIdOrderByCreatedAtDesc(
        DestinataireType type, UUID destinataireId, UUID chantierId);

    List<Message> findByExpediteurUtilisateurIdOrderByCreatedAtDesc(UUID expediteurId);
}
