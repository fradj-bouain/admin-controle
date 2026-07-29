package com.fluttiris.admincontrol.messagerie.application;

import com.fluttiris.admincontrol.messagerie.domain.ChampSurveillable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Point d'accès unique aux ChampSurveillable disponibles : Spring injecte ici
 * automatiquement tous les beans qui implémentent l'interface, donc la liste
 * s'étend simplement en ajoutant une nouvelle implémentation @Component.
 */
@Component
@RequiredArgsConstructor
public class ChampSurveillableRegistry {

    private final List<ChampSurveillable> champs;

    public List<ChampSurveillable> tous() {
        return champs;
    }

    public Optional<ChampSurveillable> parId(String id) {
        return champs.stream().filter(c -> c.getId().equals(id)).findFirst();
    }
}
