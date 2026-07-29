package com.fluttiris.admincontrol.configuration.application;

import com.fluttiris.admincontrol.configuration.domain.ControleTiers;
import com.fluttiris.admincontrol.configuration.domain.ControleTiersRepository;
import com.fluttiris.admincontrol.configuration.domain.CorpsDeMetier;
import com.fluttiris.admincontrol.configuration.domain.CorpsDeMetierRepository;
import com.fluttiris.admincontrol.configuration.domain.Pays;
import com.fluttiris.admincontrol.configuration.domain.PaysRepository;
import com.fluttiris.admincontrol.configuration.domain.SalarieFonction;
import com.fluttiris.admincontrol.configuration.domain.SalarieFonctionRepository;
import com.fluttiris.admincontrol.configuration.domain.TypeContratSalarie;
import com.fluttiris.admincontrol.configuration.domain.TypeContratSalarieRepository;
import com.fluttiris.admincontrol.configuration.domain.TypeSalarie;
import com.fluttiris.admincontrol.configuration.domain.TypeSalarieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferenceDataService {

    private final PaysRepository paysRepository;
    private final CorpsDeMetierRepository corpsDeMetierRepository;
    private final TypeSalarieRepository typeSalarieRepository;
    private final TypeContratSalarieRepository typeContratSalarieRepository;
    private final SalarieFonctionRepository salarieFonctionRepository;
    private final ControleTiersRepository controleTiersRepository;

    public Pays creerPays(String codeIso, String nom, String zone) {
        return paysRepository.save(Pays.creer(codeIso, nom, zone));
    }

    @Transactional(readOnly = true)
    public List<Pays> listerPays() {
        return paysRepository.findAll();
    }

    public CorpsDeMetier creerCorpsDeMetier(String libelle) {
        return corpsDeMetierRepository.save(CorpsDeMetier.creer(libelle));
    }

    @Transactional(readOnly = true)
    public List<CorpsDeMetier> listerCorpsDeMetier() {
        return corpsDeMetierRepository.findAll();
    }

    public TypeSalarie creerTypeSalarie(String code, String libelle) {
        return typeSalarieRepository.save(TypeSalarie.creer(code, libelle));
    }

    @Transactional(readOnly = true)
    public List<TypeSalarie> listerTypeSalarie() {
        return typeSalarieRepository.findAll();
    }

    public TypeContratSalarie creerTypeContratSalarie(String code, String libelle) {
        return typeContratSalarieRepository.save(TypeContratSalarie.creer(code, libelle));
    }

    @Transactional(readOnly = true)
    public List<TypeContratSalarie> listerTypeContratSalarie() {
        return typeContratSalarieRepository.findAll();
    }

    public SalarieFonction creerSalarieFonction(String libelle) {
        return salarieFonctionRepository.save(SalarieFonction.creer(libelle));
    }

    @Transactional(readOnly = true)
    public List<SalarieFonction> listerSalarieFonction() {
        return salarieFonctionRepository.findAll();
    }

    public ControleTiers creerControleTiers(String nom) {
        return controleTiersRepository.save(ControleTiers.creer(nom));
    }

    @Transactional(readOnly = true)
    public List<ControleTiers> listerControleTiers() {
        return controleTiersRepository.findAll();
    }
}
