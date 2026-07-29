package com.fluttiris.admincontrol.entreprise.application;

import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.entreprise.domain.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    public Entreprise creer(String raisonSociale, String siret, String adresse, String adresse2, String adresse3,
                             String codePostal, String ville, UUID paysId, UUID corpsDeMetierId, String telephone,
                             String telephone2, String telephone3, String fax, String email, String email2,
                             String email3, String formeJuridique, String siren, String rcsRci, String tvaIntra,
                             String numCotisant, String responsableSignataireAgrement, String commentaire) {
        Entreprise entreprise = Entreprise.creer(raisonSociale, siret, adresse, adresse2, adresse3, codePostal,
            ville, paysId, corpsDeMetierId, telephone, telephone2, telephone3, fax, email, email2, email3,
            formeJuridique, siren, rcsRci, tvaIntra, numCotisant, responsableSignataireAgrement, commentaire);
        return entrepriseRepository.save(entreprise);
    }

    public Entreprise modifier(UUID id, String raisonSociale, String siret, String adresse, String adresse2,
                                String adresse3, String codePostal, String ville, UUID paysId, UUID corpsDeMetierId,
                                String telephone, String telephone2, String telephone3, String fax, String email,
                                String email2, String email3, String formeJuridique, String siren, String rcsRci,
                                String tvaIntra, String numCotisant, String responsableSignataireAgrement,
                                String commentaire) {
        Entreprise entreprise = obtenir(id);
        entreprise.modifier(raisonSociale, siret, adresse, adresse2, adresse3, codePostal, ville, paysId,
            corpsDeMetierId, telephone, telephone2, telephone3, fax, email, email2, email3, formeJuridique, siren,
            rcsRci, tvaIntra, numCotisant, responsableSignataireAgrement, commentaire);
        return entreprise;
    }

    @Transactional(readOnly = true)
    public Entreprise obtenir(UUID id) {
        return entrepriseRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Entreprise", id));
    }

    @Transactional(readOnly = true)
    public List<Entreprise> lister() {
        return entrepriseRepository.findAll();
    }

    public Entreprise desactiver(UUID id) {
        Entreprise entreprise = obtenir(id);
        entreprise.desactiver();
        return entreprise;
    }

    public Entreprise activer(UUID id) {
        Entreprise entreprise = obtenir(id);
        entreprise.activer();
        return entreprise;
    }

    public void supprimer(UUID id) {
        Entreprise entreprise = obtenir(id);
        entreprise.supprimer();
    }
}
