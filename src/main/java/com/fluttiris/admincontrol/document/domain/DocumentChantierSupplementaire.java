package com.fluttiris.admincontrol.document.domain;

import com.fluttiris.admincontrol.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Un type de document demandé EN PLUS sur un chantier précis, au-delà des types
 * obligatoires globaux ({@link TypeDocument#isObligatoire()}) qui s'appliquent déjà
 * partout sans qu'aucune ligne ne soit nécessaire ici — voir la validation du modèle
 * (analyse "Documents obligatoires par chantier") : cette table ne porte aucun booléen,
 * sa seule fonction est une liste d'ajout. Documents applicables sur un chantier =
 * {types obligatoire=true} ∪ {types listés ici pour ce chantier}.
 */
@Entity
@Table(name = "document_chantier_supplementaire")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChantierSupplementaire extends Auditable {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "type_document_id", nullable = false)
    private UUID typeDocumentId;

    @Column(name = "chantier_id", nullable = false)
    private UUID chantierId;

    public static DocumentChantierSupplementaire creer(UUID typeDocumentId, UUID chantierId) {
        DocumentChantierSupplementaire regle = new DocumentChantierSupplementaire();
        regle.typeDocumentId = typeDocumentId;
        regle.chantierId = chantierId;
        return regle;
    }
}
