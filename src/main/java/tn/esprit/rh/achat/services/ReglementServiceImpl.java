
package tn.esprit.rh.achat.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.entities.Reglement;
import tn.esprit.rh.achat.repositories.FactureRepository;
import tn.esprit.rh.achat.repositories.ReglementRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ReglementServiceImpl implements IReglementService {

	@Autowired
	FactureRepository factureRepository;

	@Autowired
	ReglementRepository reglementRepository;


	// ==================== RETRIEVE ALL REGLEMENTS ====================

	@Override
	public List<Reglement> retrieveAllReglements() {

		List<Reglement> reglements = new ArrayList<>();

		reglementRepository.findAll().forEach(reglements::add);

		return reglements;
	}


	// ==================== ADD REGLEMENT ====================

	@Override
	public Reglement addReglement(Reglement reglement) {

		// 1. Validate payment
		validateReglement(reglement);

		// 2. If an invoice is associated, calculate remaining amount
		if (reglement.getFacture() != null) {
			processReglementWithFacture(reglement);
		}

		// 3. Save payment
		return reglementRepository.save(reglement);
	}


	// Validate payment
	private void validateReglement(Reglement reglement) {

		if (reglement == null) {
			throw new IllegalArgumentException(
					"Payment cannot be null"
			);
		}

		if (reglement.getMontantPaye() <= 0) {
			throw new IllegalArgumentException(
					"Payment amount must be greater than zero"
			);
		}
	}


	// Process payment associated with an invoice
	private void processReglementWithFacture(Reglement reglement) {

		// Retrieve the existing invoice
		Facture facture = getFacture(reglement);

		// Calculate the remaining amount before this payment
		float montantRestant = calculateMontantRestant(facture);

		// Check whether the payment exceeds the remaining amount
		validatePaymentAmount(
				reglement.getMontantPaye(),
				montantRestant
		);

		// Update remaining amount after payment
		float nouveauMontantRestant =
				montantRestant - reglement.getMontantPaye();

		reglement.setMontantRestant(
				Math.max(0f, nouveauMontantRestant)
		);
	}


	// Retrieve invoice from database
	private Facture getFacture(Reglement reglement) {

		Long factureId = reglement.getFacture().getIdFacture();

		if (factureId == null) {
			throw new IllegalArgumentException(
					"Invoice ID cannot be null"
			);
		}

		return factureRepository.findById(factureId)
				.orElseThrow(() -> new IllegalArgumentException(
						"Invoice not found: " + factureId
				));
	}


	// Calculate remaining amount before the new payment
	private float calculateMontantRestant(Facture facture) {

		Long factureId = facture.getIdFacture();

		// Retrieve payments already associated with this invoice
		List<Reglement> existingReglements =
				reglementRepository.retrieveReglementByFacture(factureId);

		// Calculate total amount already paid
		float totalPaye = calculateTotalPaye(existingReglements);

		// Remaining = invoice amount - existing payments
		return Math.max(
				0f,
				facture.getMontantFacture() - totalPaye
		);
	}


	// Calculate sum of existing payments
	private float calculateTotalPaye(List<Reglement> reglements) {

		if (reglements == null || reglements.isEmpty()) {
			return 0f;
		}

		float totalPaye = 0f;

		for (Reglement reglement : reglements) {
			totalPaye += reglement.getMontantPaye();
		}

		return totalPaye;
	}


	// Reject payment if it exceeds remaining amount
	private void validatePaymentAmount(
			float montantPaye,
			float montantRestant) {

		if (montantPaye > montantRestant) {
			throw new IllegalArgumentException(
					"Payment exceeds the remaining invoice amount"
			);
		}
	}


	// ==================== RETRIEVE REGLEMENT ====================

	@Override
	public Reglement retrieveReglement(Long id) {

		return reglementRepository.findById(id)
				.orElse(null);
	}


	// ==================== RETRIEVE REGLEMENTS BY FACTURE ====================

	@Override
	public List<Reglement> retrieveReglementByFacture(Long idFacture) {

		return reglementRepository.retrieveReglementByFacture(idFacture);
	}


	// ==================== CHIFFRE AFFAIRE ====================

	@Override
	public float getChiffreAffaireEntreDeuxDate(
			Date startDate,
			Date endDate) {

		return reglementRepository.getChiffreAffaireEntreDeuxDate(
				startDate,
				endDate
		);
	}

}
