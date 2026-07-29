package com.fluttiris.admincontrol.messagerie.application.champs;

import com.fluttiris.admincontrol.chantier.domain.ChantierRepository;
import com.fluttiris.admincontrol.messagerie.domain.ChampSurveillable;
import com.fluttiris.admincontrol.messagerie.domain.EvenementDetecte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChantierControleAVenirChamp implements ChampSurveillable {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ChantierRepository chantierRepository;

    @Override
    public String getId() {
        return "CHANTIER_DATE_PROCHAIN_CONTROLE";
    }

    @Override
    public String getEntiteLibelle() {
        return "Chantier";
    }

    @Override
    public String getChampLibelle() {
        return "Date du prochain contrôle";
    }

    @Override
    public List<EvenementDetecte> rechercher(LocalDate dateCible) {
        return chantierRepository.findByDateProchainControle(dateCible).stream()
            .map(chantier -> {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("DATE", dateCible.format(FORMAT_DATE));
                placeholders.put("CHANTIER_NOM", chantier.getNom());
                return new EvenementDetecte(chantier.getId(), chantier.getId(), placeholders);
            })
            .toList();
    }
}
