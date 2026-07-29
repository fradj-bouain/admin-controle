package com.fluttiris.admincontrol.entreprise.domain;

import com.fluttiris.admincontrol.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AffectationEntrepriseChantierTest {

    private final UUID chantierId = UUID.randomUUID();

    @Test
    void une_entreprise_principale_ne_doit_pas_avoir_de_parent() {
        var affectation = AffectationEntrepriseChantier.creer(
            chantierId, UUID.randomUUID(), RoleEntreprise.PRINCIPALE, null);

        assertThat(affectation.getRole()).isEqualTo(RoleEntreprise.PRINCIPALE);
        assertThat(affectation.getAffectationParenteId()).isNull();
    }

    @Test
    void un_stt1_doit_etre_rattache_a_la_principale_du_meme_chantier() {
        var principale = AffectationEntrepriseChantier.creer(
            chantierId, UUID.randomUUID(), RoleEntreprise.PRINCIPALE, null);

        var stt1 = AffectationEntrepriseChantier.creer(
            chantierId, UUID.randomUUID(), RoleEntreprise.STT1, principale);

        assertThat(stt1.getAffectationParenteId()).isEqualTo(principale.getId());
    }

    @Test
    void un_stt1_sans_parent_est_rejete() {
        assertThatThrownBy(() ->
            AffectationEntrepriseChantier.creer(chantierId, UUID.randomUUID(), RoleEntreprise.STT1, null))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void un_stt2_dont_le_parent_est_la_principale_directement_est_rejete() {
        var principale = AffectationEntrepriseChantier.creer(
            chantierId, UUID.randomUUID(), RoleEntreprise.PRINCIPALE, null);

        // un STT2 doit obligatoirement passer par un STT1, pas directement par la Principale
        assertThatThrownBy(() ->
            AffectationEntrepriseChantier.creer(chantierId, UUID.randomUUID(), RoleEntreprise.STT2, principale))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void une_meme_entreprise_peut_avoir_des_roles_differents_sur_deux_chantiers() {
        UUID chantierA = UUID.randomUUID();
        UUID chantierB = UUID.randomUUID();
        UUID entrepriseX = UUID.randomUUID();

        // Principale sur le chantier A
        var affectationA = AffectationEntrepriseChantier.creer(chantierA, entrepriseX, RoleEntreprise.PRINCIPALE, null);

        // STT1 sur le chantier B, rattachée à une autre Principale
        var principaleB = AffectationEntrepriseChantier.creer(chantierB, UUID.randomUUID(), RoleEntreprise.PRINCIPALE, null);
        var affectationB = AffectationEntrepriseChantier.creer(chantierB, entrepriseX, RoleEntreprise.STT1, principaleB);

        assertThat(affectationA.getRole()).isEqualTo(RoleEntreprise.PRINCIPALE);
        assertThat(affectationB.getRole()).isEqualTo(RoleEntreprise.STT1);
        assertThat(affectationA.getEntrepriseId()).isEqualTo(affectationB.getEntrepriseId());
    }

    @Test
    void desactiver_une_affectation_la_rend_inactive() {
        var affectation = AffectationEntrepriseChantier.creer(
            chantierId, UUID.randomUUID(), RoleEntreprise.PRINCIPALE, null);
        assertThat(affectation.estActive()).isTrue();

        affectation.desactiver();

        assertThat(affectation.estActive()).isFalse();
    }
}
