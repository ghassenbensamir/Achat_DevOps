
package tn.esprit.rh.achat.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tn.esprit.rh.achat.entities.*;
import tn.esprit.rh.achat.repositories.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FactureServiceImpl implements IFactureService {


	private final FactureRepository factureRepository;

	private final OperateurRepository operateurRepository;

	private final DetailFactureRepository detailFactureRepository;


	private final FournisseurRepository fournisseurRepository;

	private final ProduitRepository produitRepository;


	private final ReglementServiceImpl reglementService;


	// ==================== RETRIEVE ALL FACTURES ====================

	@Override
	public List<Facture> retrieveAllFactures() {
		List<Facture> factures = new ArrayList<>();

		factureRepository.findAll().forEach(factures::add);

		log.info("Retrieved {} invoices", factures.size());

		return factures;
	}


	// ==================== ADD FACTURE ====================

	@Override
	public Facture addFacture(Facture facture) {

		// 1. Validate invoice and its details
		validateFacture(facture);

		// 2. Calculate amounts if details exist
		Set<DetailFacture> details = facture.getDetailsFacture();

		if (details != null && !details.isEmpty()) {
			calculateFactureAmounts(facture, details);
		}

		// 3. Save invoice
		return factureRepository.save(facture);
	}


	// Validate invoice and all its details
	private void validateFacture(Facture facture) {

		if (facture == null) {
			throw new IllegalArgumentException(
					"Invoice cannot be null"
			);
		}

		Set<DetailFacture> details = facture.getDetailsFacture();

		if (details == null) {
			return;
		}

		for (DetailFacture detail : details) {
			validateDetail(detail);
		}
	}


	// Validate an individual invoice detail
	private void validateDetail(DetailFacture detail) {

		if (detail == null) {
			throw new IllegalArgumentException(
					"Invoice detail cannot be null"
			);
		}

		if (detail.getQteCommandee() <= 0) {
			throw new IllegalArgumentException(
					"Quantity must be greater than zero"
			);
		}

		float discount = detail.getPourcentageRemise();

		if (discount < 0 || discount > 100) {
			throw new IllegalArgumentException(
					"Discount must be between 0 and 100"
			);
		}

		if (detail.getProduit() == null ||
				detail.getProduit().getIdProduit() == null) {

			throw new IllegalArgumentException(
					"Product must be specified"
			);
		}
	}


	// Calculate total invoice amount and total discount
	private void calculateFactureAmounts(
			Facture facture,
			Set<DetailFacture> details) {

		float totalAmount = 0f;
		float totalDiscount = 0f;

		for (DetailFacture detail : details) {

			calculateDetailAmounts(detail);

			totalAmount += detail.getPrixTotalDetail();
			totalDiscount += detail.getMontantRemise();
		}

		facture.setMontantFacture(totalAmount);
		facture.setMontantRemise(totalDiscount);
	}


	// Calculate amounts for one detail and save it
	private void calculateDetailAmounts(DetailFacture detail) {

		Produit produit = getProduit(detail);

		float grossAmount =
				detail.getQteCommandee() * produit.getPrix();

		float discountAmount =
				grossAmount * detail.getPourcentageRemise() / 100f;

		float netAmount =
				grossAmount - discountAmount;

		detail.setMontantRemise(discountAmount);
		detail.setPrixTotalDetail(netAmount);

		detailFactureRepository.save(detail);
	}


	// Retrieve the product associated with a detail
	private Produit getProduit(DetailFacture detail) {

		Long produitId = detail.getProduit().getIdProduit();

		return produitRepository.findById(produitId)
				.orElseThrow(() -> new IllegalArgumentException(
						"Product not found: " + produitId
				));
	}


	// ==================== CANCEL FACTURE ====================

	@Override
	public void cancelFacture(Long factureId) {

		Facture facture = factureRepository.findById(factureId)
				.orElseThrow(() -> new IllegalArgumentException(
						"Invoice not found: " + factureId
				));

		if (Boolean.TRUE.equals(facture.getArchivee())) {
			throw new IllegalArgumentException(
					"Invoice is already cancelled"
			);
		}

		facture.setArchivee(true);

		factureRepository.save(facture);
	}


	// ==================== RETRIEVE FACTURE ====================

	@Override
	public Facture retrieveFacture(Long factureId) {

		Facture facture = factureRepository
				.findById(factureId)
				.orElse(null);

		log.info("Retrieved invoice: {}", facture);

		return facture;
	}


	// ==================== FACTURES BY FOURNISSEUR ====================

	@Override
	public List<Facture> getFacturesByFournisseur(
			Long idFournisseur) {

		Fournisseur fournisseur = fournisseurRepository
				.findById(idFournisseur)
				.orElseThrow(() -> new IllegalArgumentException(
						"Supplier not found: " + idFournisseur
				));

		if (fournisseur.getFactures() == null) {
			return new ArrayList<>();
		}

		return new ArrayList<>(fournisseur.getFactures());
	}


	// ==================== ASSIGN OPERATEUR ====================

	@Override
	public void assignOperateurToFacture(
			Long idOperateur,
			Long idFacture) {

		// Retrieve invoice
		Facture facture = factureRepository
				.findById(idFacture)
				.orElseThrow(() -> new IllegalArgumentException(
						"Invoice not found: " + idFacture
				));

		// Retrieve operator
		Operateur operateur = operateurRepository
				.findById(idOperateur)
				.orElseThrow(() -> new IllegalArgumentException(
						"Operator not found: " + idOperateur
				));

		// Check invoice status
		if (Boolean.TRUE.equals(facture.getArchivee())) {
			throw new IllegalArgumentException(
					"Cannot assign an operator to a cancelled invoice"
			);
		}

		// Initialize collection if necessary
		if (operateur.getFactures() == null) {
			operateur.setFactures(new HashSet<Facture>());
		}

		// Assign invoice to operator
		operateur.getFactures().add(facture);

		operateurRepository.save(operateur);
	}


	// ==================== POURCENTAGE RECOUVREMENT ====================

	@Override
	public float pourcentageRecouvrement(
			Date startDate,
			Date endDate) {

		float totalFactures =
				factureRepository.getTotalFacturesEntreDeuxDates(
						startDate,
						endDate
				);

		float totalRecouvrement =
				reglementService.getChiffreAffaireEntreDeuxDate(
						startDate,
						endDate
				);

		if (totalFactures == 0f) {
			return 0f;
		}

		return (totalRecouvrement / totalFactures) * 100f;
	}

}
