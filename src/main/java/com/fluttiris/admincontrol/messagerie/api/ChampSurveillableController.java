package com.fluttiris.admincontrol.messagerie.api;

import com.fluttiris.admincontrol.messagerie.api.dto.ChampSurveillableResponse;
import com.fluttiris.admincontrol.messagerie.application.ChampSurveillableRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogue des sources de données que l'automatisation messagerie peut
 * surveiller (voir ChampSurveillableRegistry). Sert le formulaire de règle
 * d'automatisation : le choix d'événement déclencheur n'est plus figé côté
 * frontend, il reflète ce que le backend sait réellement détecter.
 */
@RestController
@RequestMapping("/api/v1/champs-surveillables")
@RequiredArgsConstructor
public class ChampSurveillableController {

    private final ChampSurveillableRegistry champSurveillableRegistry;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ChampSurveillableResponse> lister() {
        return champSurveillableRegistry.tous().stream().map(ChampSurveillableResponse::from).toList();
    }
}
