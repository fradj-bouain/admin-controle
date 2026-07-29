package com.fluttiris.admincontrol.client.application;

import com.fluttiris.admincontrol.client.domain.Client;
import com.fluttiris.admincontrol.client.domain.ClientRepository;
import com.fluttiris.admincontrol.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public Client creer(String raisonSociale, String adresse, String adresse2, String adresse3, String codePostal,
                         String ville, UUID paysId, String telephone, String telephone2, String telephone3,
                         String fax, String email, String email2, String email3, String formeJuridique,
                         String siren, String siret, String rcsRci, String tvaIntra, String numCotisant,
                         String responsableSignataireAgrement) {
        Client client = Client.creer(raisonSociale, adresse, adresse2, adresse3, codePostal, ville, paysId,
            telephone, telephone2, telephone3, fax, email, email2, email3, formeJuridique, siren, siret,
            rcsRci, tvaIntra, numCotisant, responsableSignataireAgrement);
        return clientRepository.save(client);
    }

    public Client modifier(UUID id, String raisonSociale, String adresse, String adresse2, String adresse3,
                            String codePostal, String ville, UUID paysId, String telephone, String telephone2,
                            String telephone3, String fax, String email, String email2, String email3,
                            String formeJuridique, String siren, String siret, String rcsRci, String tvaIntra,
                            String numCotisant, String responsableSignataireAgrement) {
        Client client = obtenir(id);
        client.modifier(raisonSociale, adresse, adresse2, adresse3, codePostal, ville, paysId, telephone,
            telephone2, telephone3, fax, email, email2, email3, formeJuridique, siren, siret, rcsRci, tvaIntra,
            numCotisant, responsableSignataireAgrement);
        return client;
    }

    @Transactional(readOnly = true)
    public Client obtenir(UUID id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client", id));
    }

    @Transactional(readOnly = true)
    public List<Client> lister() {
        return clientRepository.findAll();
    }

    public Client desactiver(UUID id) {
        Client client = obtenir(id);
        client.desactiver();
        return client;
    }

    public Client activer(UUID id) {
        Client client = obtenir(id);
        client.activer();
        return client;
    }

    public void supprimer(UUID id) {
        Client client = obtenir(id);
        client.supprimer();
    }
}
