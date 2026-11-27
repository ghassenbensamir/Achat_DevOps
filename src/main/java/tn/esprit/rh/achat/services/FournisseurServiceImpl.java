package tn.esprit.rh.achat.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.rh.achat.entities.DetailFournisseur;
import tn.esprit.rh.achat.entities.Fournisseur;
import tn.esprit.rh.achat.entities.SecteurActivite;
import tn.esprit.rh.achat.repositories.DetailFournisseurRepository;
import tn.esprit.rh.achat.repositories.FournisseurRepository;
import tn.esprit.rh.achat.repositories.ProduitRepository;
import tn.esprit.rh.achat.repositories.SecteurActiviteRepository;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FournisseurServiceImpl implements IFournisseurService {

	private final FournisseurRepository fournisseurRepository;
	private final  DetailFournisseurRepository detailFournisseurRepository;
	private final  ProduitRepository produitRepository;
	private final SecteurActiviteRepository secteurActiviteRepository;

	@Override
	public List<Fournisseur> retrieveAllFournisseurs() {
		List<Fournisseur> fournisseurs = (List<Fournisseur>) fournisseurRepository.findAll();
		for (Fournisseur fournisseur : fournisseurs) {
			log.info(" fournisseur : " + fournisseur);
		}
		return fournisseurs;
	}


	@Override
	public Fournisseur addFournisseur(Fournisseur f ) {
		// NEW-SUP-01: Check code uniqueness
		if (f.getCode() != null && fournisseurRepository.existsFournisseurByCode(f.getCode())) {
			throw new IllegalArgumentException("Un fournisseur avec ce code existe déjà : " + f.getCode());
		}

		DetailFournisseur df = new DetailFournisseur(); // Slave
		df.setDateDebutCollaboration(new Date());
		f.setDetailFournisseur(df);
		fournisseurRepository.save(f);
		return f;
	}
	
	private DetailFournisseur  saveDetailFournisseur(Fournisseur f){
		DetailFournisseur df = f.getDetailFournisseur();
		detailFournisseurRepository.save(df);
		return df;
	}

	public Fournisseur updateFournisseur(Fournisseur f) {
		DetailFournisseur df = saveDetailFournisseur(f);
		f.setDetailFournisseur(df);	
		fournisseurRepository.save(f);
		return f;
	}

	@Override
	public void deleteFournisseur(Long fournisseurId) {
		fournisseurRepository.deleteById(fournisseurId);

	}

	@Override
	public Fournisseur retrieveFournisseur(Long fournisseurId) {

		Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId).orElse(null);
		return fournisseur;
	}

	@Override
	public void assignSecteurActiviteToFournisseur(Long idSecteurActivite, Long idFournisseur) {
		// NEW-SUP-02: Verify both supplier and activity sector exist
		Fournisseur fournisseur = fournisseurRepository.findById(idFournisseur)
				.orElseThrow(() -> new IllegalArgumentException("Fournisseur introuvable avec l'id : " + idFournisseur));
		SecteurActivite secteurActivite = secteurActiviteRepository.findById(idSecteurActivite)
				.orElseThrow(() -> new IllegalArgumentException("Secteur d'activité introuvable avec l'id : " + idSecteurActivite));

		fournisseur.getSecteurActivites().add(secteurActivite);
		fournisseurRepository.save(fournisseur);
	}

	

}