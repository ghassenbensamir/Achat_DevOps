package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.entities.Reglement;
import tn.esprit.rh.achat.repositories.FactureRepository;
import tn.esprit.rh.achat.repositories.ReglementRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Module Règlement : FR-REG-01..05, NEW-REG-01..04.
 *
 * Règle de calcul retenue :
 *   montantRestant(facture) = montantFacture - somme(montantPaye des règlements existants)
 * Un règlement est refusé si montantPaye <= 0 ou montantPaye > montantRestant.
 * Après enregistrement : reglement.montantRestant = montantRestant - montantPaye (jamais < 0).
 * Un règlement sans facture associée reste accepté (NEW-REG-04 ne s'applique que s'il y en a une).
 */
@ExtendWith(MockitoExtension.class)
class ReglementServiceImplTest {

    @Mock
    private FactureRepository factureRepository;
    @Mock
    private ReglementRepository reglementRepository;

    @InjectMocks
    private ReglementServiceImpl reglementService;

    // ------------------------------------------------------------------
    // Consultation (FR-REG-02..05)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-REG-02: retrieve all payments")
    void retrieveAllReglements_returnsAll() {
        List<Reglement> list = Arrays.asList(new Reglement(), new Reglement());
        when(reglementRepository.findAll()).thenReturn(list);

        assertEquals(2, reglementService.retrieveAllReglements().size());
    }

    @Test
    @DisplayName("FR-REG-03: retrieve a payment by id")
    void retrieveReglement_found() {
        Reglement r = new Reglement();
        r.setIdReglement(1L);
        when(reglementRepository.findById(1L)).thenReturn(Optional.of(r));

        assertEquals(1L, reglementService.retrieveReglement(1L).getIdReglement());
    }

    @Test
    @DisplayName("FR-REG-03: retrieve an unknown payment returns null")
    void retrieveReglement_notFound() {
        when(reglementRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(reglementService.retrieveReglement(99L));
    }

    @Test
    @DisplayName("FR-REG-04: retrieve the payments of an invoice")
    void retrieveReglementByFacture_returnsItsPayments() {
        List<Reglement> list = Arrays.asList(new Reglement(), new Reglement());
        when(reglementRepository.retrieveReglementByFacture(1L)).thenReturn(list);

        assertEquals(2, reglementService.retrieveReglementByFacture(1L).size());
    }

    @Test
    @DisplayName("FR-REG-05: revenue between two dates comes from the repository")
    void getChiffreAffaire_returnsRepositoryValue() {
        Date start = new Date(0L);
        Date end = new Date();
        when(reglementRepository.getChiffreAffaireEntreDeuxDate(start, end)).thenReturn(1234.5f);

        assertEquals(1234.5f, reglementService.getChiffreAffaireEntreDeuxDate(start, end), 0.001f);
    }

    // ------------------------------------------------------------------
    // FR-REG-01 : ajout d'un règlement valide
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-REG-01: a valid payment without invoice is saved")
    void addReglement_validWithoutFacture_isSaved() {
        Reglement r = buildReglement(100f, null);
        when(reglementRepository.save(r)).thenReturn(r);

        Reglement result = reglementService.addReglement(r);

        assertSame(r, result);
        verify(reglementRepository).save(r);
    }

    @Test
    @DisplayName("FR-REG-01 / NEW-REG-02: a valid payment on an invoice is saved and the remaining amount is updated")
    void addReglement_validWithFacture_isSavedAndRemainingUpdated() {
        // facture 1000, déjà payé 300, nouveau paiement 200 -> reste 500
        Facture facture = buildFacture(1L, 1000f);
        Reglement r = buildReglement(200f, facture);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));
        when(reglementRepository.retrieveReglementByFacture(1L))
                .thenReturn(Collections.singletonList(buildReglement(300f, facture)));
        when(reglementRepository.save(r)).thenReturn(r);

        Reglement result = reglementService.addReglement(r);

        assertEquals(500f, result.getMontantRestant(), 0.001f);
        verify(reglementRepository).save(r);
    }

    // ------------------------------------------------------------------
    // NEW-REG-01 : montant payé négatif ou nul
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-REG-01: a payment of 0 is refused")
    void addReglement_zeroAmount_isRefused() {
        Reglement r = buildReglement(0f, null);

        assertThrows(IllegalArgumentException.class, () -> reglementService.addReglement(r));

        verify(reglementRepository, never()).save(any(Reglement.class));
    }

    @Test
    @DisplayName("NEW-REG-01: a negative payment is refused")
    void addReglement_negativeAmount_isRefused() {
        Reglement r = buildReglement(-50f, null);

        assertThrows(IllegalArgumentException.class, () -> reglementService.addReglement(r));

        verify(reglementRepository, never()).save(any(Reglement.class));
    }

    // ------------------------------------------------------------------
    // NEW-REG-03 : dépassement du montant restant
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-REG-03: a payment above the remaining amount is refused")
    void addReglement_exceedsRemaining_isRefused() {
        // facture 1000, déjà payé 900 -> reste 100 ; paiement de 200 refusé
        Facture facture = buildFacture(1L, 1000f);
        Reglement r = buildReglement(200f, facture);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));
        when(reglementRepository.retrieveReglementByFacture(1L))
                .thenReturn(Collections.singletonList(buildReglement(900f, facture)));

        assertThrows(IllegalArgumentException.class, () -> reglementService.addReglement(r));

        verify(reglementRepository, never()).save(any(Reglement.class));
    }

    @Test
    @DisplayName("NEW-REG-02 (boundary): paying exactly the remaining amount is accepted and leaves 0, never negative")
    void addReglement_exactRemaining_isAcceptedWithZeroRemaining() {
        Facture facture = buildFacture(1L, 1000f);
        Reglement r = buildReglement(100f, facture);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(facture));
        when(reglementRepository.retrieveReglementByFacture(1L))
                .thenReturn(Collections.singletonList(buildReglement(900f, facture)));
        when(reglementRepository.save(r)).thenReturn(r);

        Reglement result = reglementService.addReglement(r);

        assertEquals(0f, result.getMontantRestant(), 0.001f);
        assertTrue(result.getMontantRestant() >= 0f);
    }

    // ------------------------------------------------------------------
    // NEW-REG-04 : la facture associée doit exister
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-REG-04: a payment linked to a non-existent invoice is refused")
    void addReglement_factureNotFound_isRefused() {
        Facture facture = buildFacture(99L, 1000f);
        Reglement r = buildReglement(100f, facture);
        when(factureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> reglementService.addReglement(r));

        verify(reglementRepository, never()).save(any(Reglement.class));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Facture buildFacture(Long id, float montant) {
        Facture f = new Facture();
        f.setIdFacture(id);
        f.setMontantFacture(montant);
        return f;
    }

    private Reglement buildReglement(float montantPaye, Facture facture) {
        Reglement r = new Reglement();
        r.setMontantPaye(montantPaye);
        r.setFacture(facture);
        return r;
    }
}