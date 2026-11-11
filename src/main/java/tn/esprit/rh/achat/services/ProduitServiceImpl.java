package tn.esprit.rh.achat.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tn.esprit.rh.achat.entities.Produit;
import tn.esprit.rh.achat.entities.Stock;
import tn.esprit.rh.achat.repositories.CategorieProduitRepository;
import tn.esprit.rh.achat.repositories.ProduitRepository;
import tn.esprit.rh.achat.repositories.StockRepository;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Slf4j
public class ProduitServiceImpl implements IProduitService {

	@Autowired
	ProduitRepository produitRepository;
	@Autowired
	StockRepository stockRepository;
	@Autowired
	CategorieProduitRepository categorieProduitRepository;

	@Override
	public List<Produit> retrieveAllProduits() {
		List<Produit> produits = (List<Produit>) produitRepository.findAll();
		for (Produit produit : produits) {
			log.info(" Produit : " + produit);
		}
		return produits;
	}

	// FR-PROD-01, NEW-PROD-01, NEW-PROD-02, NEW-PROD-03
	@Override
	public Produit addProduit(Produit p) {
		validatePrice(p);

		if (p.getCodeProduit() != null
				&& produitRepository.findByCodeProduit(p.getCodeProduit())!=null) {
			throw new IllegalArgumentException(
					"Un produit avec le code '" + p.getCodeProduit() + "' existe déjà");
		}

		validateCategorieExists(p);

		return saveOrRefuseDuplicate(p);
	}

	// FR-PROD-04, NEW-PROD-01
	@Override
	public Produit updateProduit(Produit p) {
		validatePrice(p);
		validateCategorieExists(p);
		return saveOrRefuseDuplicate(p);
	}

	/** NEW-PROD-02: the DB unique constraint is the final guard against concurrent duplicates. */
	private Produit saveOrRefuseDuplicate(Produit p) {
		try {
			return produitRepository.save(p);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException(
					"Impossible d'enregistrer le produit (code '" + p.getCodeProduit()
							+ "' déjà utilisé ou contrainte d'intégrité violée)", e);
		}
	}

	

	@Override
	public void deleteProduit(Long produitId) {
		produitRepository.deleteById(produitId);
	}


	@Override
	public Produit retrieveProduit(Long produitId) {
		Produit produit = produitRepository.findById(produitId).orElse(null);
		log.info("produit :" + produit);
		return produit;
	}

	@Override
	public void assignProduitToStock(Long idProduit, Long idStock) {
		Produit produit = produitRepository.findById(idProduit)
				.orElseThrow(() -> new IllegalArgumentException(
						"Produit introuvable : id=" + idProduit));

		Stock stock = stockRepository.findById(idStock)
				.orElseThrow(() -> new IllegalArgumentException(
						"Stock introuvable : id=" + idStock));

		produit.setStock(stock);
		produitRepository.save(produit);
	}



    //helpers
	/** NEW-PROD-01: a negative price is refused (0 is accepted). */
	private void validatePrice(Produit p) {
		if (p.getPrix() < 0) {
			throw new IllegalArgumentException(
					"Le prix du produit ne peut pas être négatif : " + p.getPrix());
		}
	}

	/** NEW-PROD-03: if a category is linked, it must exist. */
	private void validateCategorieExists(Produit p) {
		if (p.getCategorieProduit() != null
				&& p.getCategorieProduit().getIdCategorieProduit() != null
				&& !categorieProduitRepository.existsById(
				p.getCategorieProduit().getIdCategorieProduit())) {
			throw new IllegalArgumentException(
					"Catégorie introuvable : id="
							+ p.getCategorieProduit().getIdCategorieProduit());
		}
	}

}