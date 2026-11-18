package tn.esprit.rh.achat.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.rh.achat.entities.CategorieProduit;
import tn.esprit.rh.achat.repositories.CategorieProduitRepository;

import java.util.List;

@Service
public class CategorieProduitServiceImpl implements ICategorieProduitService {

	@Autowired
	CategorieProduitRepository categorieProduitRepository;

	@Override
	public List<CategorieProduit> retrieveAllCategorieProduits() {
		return categorieProduitRepository.findAll();
	}

	@Override
	public CategorieProduit addCategorieProduit(CategorieProduit cp) {
		// NEW-CAT-01: Check code uniqueness
		if (cp.getCodeCategorie() != null
				&& categorieProduitRepository.existsCategorieProduitByCodeCategorie(cp.getCodeCategorie())) {
			throw new IllegalArgumentException("Une catégorie avec ce code existe déjà : " + cp.getCodeCategorie());
		}
		return categorieProduitRepository.save(cp);
	}

	@Override
	public void deleteCategorieProduit(Long id) {
		// NEW-CAT-02: Prevent deletion if linked to products
		CategorieProduit cp = categorieProduitRepository.findById(id).orElse(null);
		if (cp != null && cp.getProduits() != null && !cp.getProduits().isEmpty()) {
			throw new IllegalArgumentException("Impossible de supprimer une catégorie utilisée par des produits.");
		}
		categorieProduitRepository.deleteById(id);
	}

	@Override
	public CategorieProduit updateCategorieProduit(CategorieProduit cp) {
		return categorieProduitRepository.save(cp);
	}

	@Override
	public CategorieProduit retrieveCategorieProduit(Long id) {
		return categorieProduitRepository.findById(id).orElse(null);
	}
}