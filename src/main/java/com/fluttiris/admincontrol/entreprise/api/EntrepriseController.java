package com.fluttiris.admincontrol.entreprise.api;

import com.fluttiris.admincontrol.entreprise.api.dto.AffectationEntrepriseChantierResponse;
import com.fluttiris.admincontrol.entreprise.api.dto.CreateEntrepriseRequest;
import com.fluttiris.admincontrol.entreprise.api.dto.EntrepriseResponse;
import com.fluttiris.admincontrol.chantier.application.ChantierService;
import com.fluttiris.admincontrol.entreprise.application.AffectationEntrepriseChantierService;
import com.fluttiris.admincontrol.entreprise.application.EntrepriseService;
import com.fluttiris.admincontrol.entreprise.domain.AffectationEntrepriseChantier;
import com.fluttiris.admincontrol.entreprise.domain.Entreprise;
import com.fluttiris.admincontrol.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entreprises")
@RequiredArgsConstructor
public class EntrepriseController {

    private final EntrepriseService entrepriseService;
    private final AffectationEntrepriseChantierService affectationEntrepriseChantierService;
    private final ChantierService chantierService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EntrepriseResponse> creer(@Valid @RequestBody CreateEntrepriseRequest request) {
        var entreprise = entrepriseService.creer(request.raisonSociale(), request.siret(), request.adresse(),
            request.adresse2(), request.adresse3(), request.codePostal(), request.ville(), request.paysId(),
            request.corpsDeMetierId(), request.telephone(), request.telephone2(), request.telephone3(),
            request.fax(), request.email(), request.email2(), request.email3(), request.formeJuridique(),
            request.siren(), request.rcsRci(), request.tvaIntra(), request.numCotisant(),
            request.responsableSignataireAgrement(), request.commentaire());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntrepriseResponse.from(entreprise));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#id)) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.entrepriseAccessibleParClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isEmpty() and @currentUser.clientId().isEmpty())")
    public EntrepriseResponse obtenir(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.obtenir(id));
    }

    // Modification de la fiche entreprise : SUPER_ADMIN (tous les champs) ou l'Entreprise
    // elle-même (coordonnées uniquement — pas les informations légales). Un compte
    // Entreprise pouvait auparavant tout modifier, puis plus rien ; ce comportement
    // intermédiaire restaure l'auto-édition des coordonnées à la demande du client
    // tout en gardant le registre légal (Siret/Siren/RCS-RCI/TVA intra) verrouillé
    // administrateur — voir le figeage ci-dessous, appliqué même si la requête tente
    // de les modifier (le champ désactivé côté UI n'est pas une garantie de sécurité).
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#id))")
    public EntrepriseResponse modifier(@PathVariable UUID id, @Valid @RequestBody CreateEntrepriseRequest request) {
        String siret = request.siret();
        String siren = request.siren();
        String rcsRci = request.rcsRci();
        String tvaIntra = request.tvaIntra();
        if (currentUser.entrepriseId().isPresent()) {
            var existante = entrepriseService.obtenir(id);
            siret = existante.getSiret();
            siren = existante.getSiren();
            rcsRci = existante.getRcsRci();
            tvaIntra = existante.getTvaIntra();
        }
        var entreprise = entrepriseService.modifier(id, request.raisonSociale(), siret, request.adresse(),
            request.adresse2(), request.adresse3(), request.codePostal(), request.ville(), request.paysId(),
            request.corpsDeMetierId(), request.telephone(), request.telephone2(), request.telephone3(),
            request.fax(), request.email(), request.email2(), request.email3(), request.formeJuridique(),
            siren, rcsRci, tvaIntra, request.numCotisant(),
            request.responsableSignataireAgrement(), request.commentaire());
        return EntrepriseResponse.from(entreprise);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EntrepriseResponse> lister() {
        // Un compte Entreprise est un tenant : il ne voit jamais le registre complet,
        // seulement sa propre fiche.
        if (currentUser.entrepriseId().isPresent()) {
            return avecChantierActuel(List.of(entrepriseService.obtenir(currentUser.entrepriseId().get())));
        }
        // Un compte Client est aussi un tenant : uniquement les entreprises affectées aux
        // chantiers qui lui ont été explicitement assignés, jamais le registre complet —
        // aucun chantier assigné = aucune entreprise visible.
        if (currentUser.clientId().isPresent()) {
            return avecChantierActuel(
                entrepriseService.listerParClient(currentUser.clientId().get(), currentUser.keycloakId()));
        }
        return avecChantierActuel(entrepriseService.lister());
    }

    private List<EntrepriseResponse> avecChantierActuel(List<Entreprise> entreprises) {
        var ids = entreprises.stream().map(Entreprise::getId).toList();
        var chantierActuelParEntreprise = entrepriseService.chantierActuelParEntreprise(ids);
        var rangActuelParEntreprise = entrepriseService.rangActuelParEntreprise(ids);
        return entreprises.stream()
            .map(e -> EntrepriseResponse.from(e, chantierActuelParEntreprise.get(e.getId()), rangActuelParEntreprise.get(e.getId())))
            .toList();
    }

    @PostMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EntrepriseResponse desactiver(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.desactiver(id));
    }

    @PostMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public EntrepriseResponse activer(@PathVariable UUID id) {
        return EntrepriseResponse.from(entrepriseService.activer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        entrepriseService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    /** Un compte Client ne doit voir, parmi TOUS les chantiers de cette entreprise, que
        ceux qui appartiennent à SON périmètre — sans ce filtre, il verrait avec quels
        AUTRES clients cette entreprise travaille (fuite d'information concurrentielle).
        Le @PreAuthorize vérifie seulement qu'il a accès à AU MOINS un chantier en commun
        (voir scopeAuthz.entrepriseAccessibleParClient) ; le filtre ci-dessous s'assure
        qu'aucun chantier hors périmètre ne fuite dans la réponse elle-même. */
    @GetMapping("/{id}/chantiers")
    @PreAuthorize("hasRole('SUPER_ADMIN') or "
        + "(@currentUser.entrepriseId().isPresent() and @currentUser.entrepriseId().get().equals(#id)) or "
        + "(@currentUser.clientId().isPresent() and @scopeAuthz.entrepriseAccessibleParClient(#id, @currentUser.clientId().get(), @currentUser.keycloakId())) or "
        + "(@currentUser.entrepriseId().isEmpty() and @currentUser.clientId().isEmpty())")
    public List<AffectationEntrepriseChantierResponse> listerChantiers(@PathVariable UUID id) {
        List<AffectationEntrepriseChantier> affectations = affectationEntrepriseChantierService.listerParEntreprise(id);
        if (currentUser.clientId().isPresent()) {
            List<UUID> chantierIdsAccessibles = chantierService.listerIdsAccessiblesPourClient(
                currentUser.clientId().get(), currentUser.keycloakId());
            affectations = affectations.stream().filter(a -> chantierIdsAccessibles.contains(a.getChantierId())).toList();
        }
        return affectations.stream().map(AffectationEntrepriseChantierResponse::from).toList();
    }
}
