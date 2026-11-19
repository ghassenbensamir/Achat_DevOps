package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.DetailFacture;
import tn.esprit.rh.achat.entities.Facture;
import tn.esprit.rh.achat.entities.Fournisseur;
import tn.esprit.rh.achat.entities.Operateur;
import tn.esprit.rh.achat.entities.Produit;
import tn.esprit.rh.achat.repositories.DetailFactureRepository;
import tn.esprit.rh.achat.repositories.FactureRepository;
import tn.esprit.rh.achat.repositories.FournisseurRepository;
import tn.esprit.rh.achat.repositories.OperateurRepository;
import tn.esprit.rh.achat.repositories.ProduitRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Module Facture : FR-INV-01..07, NEW-INV-01..05.
 *
 * Conventions retenues (à confirmer, cf. SRS 4.8) :
 *  - toute violation de règle métier lève IllegalArgumentException ;
 *  - une facture déjà annulée ne peut pas être annulée une 2e fois (refus) ;
 *  - on ne peut pas affecter un opérateur à une facture annulée (NEW-INV-05).
 *
 * Calcul attendu par détail : total = qte * prix ; remise = total * % / 100 ;
 * prixTotalDetail = total - remise. Facture : somme des détails.
 */
@ExtendWith(MockitoExtension.class)
class FactureServiceImplTest {

    @Mock
    private FactureRepository factureRepository;
    @Mock
    private OperateurRepository operateurRepository;
    @Mock
    private DetailFactureRepository detailFactureRepository;
    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private ReglementServiceImpl reglementService;

    @InjectMocks
    private FactureServiceImpl factureService;

    // ------------------------------------------------------------------
    // CRUD / consultation
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-INV-01: add an invoice without details saves it")
    void addFacture_withoutDetails_isSaved() {
        Facture f = new Facture();
        when(factureRepository.save(f)).thenReturn(f);

        Facture result = factureService.addFacture(f);

        assertSame(f, result);
        verify(factureRepository).save(f);
    }

    @Test
    @DisplayName("FR-INV-02: retrieve all invoices")
    void retrieveAllFactures_returnsAll() {
        List<Facture> list = Arrays.asList(new Facture(), new Facture());
        when(factureRepository.findAll()).thenReturn(list);

        assertEquals(2, factureService.retrieveAllFactures().size());
    }

    @Test
    @DisplayName("FR-INV-03: retrieve an invoice by id")
    void retrieveFacture_found() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));

        assertEquals(1L, factureService.retrieveFacture(1L).getIdFacture());
    }

    @Test
    @DisplayName("FR-INV-03: retrieve an unknown invoice returns null")
    void retrieveFacture_notFound() {
        when(factureRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(factureService.retrieveFacture(99L));
    }

    @Test
    @DisplayName("FR-INV-04: retrieve the invoices of a supplier")
    void getFacturesByFournisseur_returnsItsInvoices() {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setIdFournisseur(1L);
        fournisseur.setFactures(new HashSet<Facture>(Arrays.asList(new Facture(), new Facture())));
        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(fournisseur));

        List<Facture> result = factureService.getFacturesByFournisseur(1L);

        assertEquals(2, result.size());
    }

    // ------------------------------------------------------------------
    // FR-INV-06 / NEW-INV-04 / NEW-INV-05 : affectation d'un opérateur
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-INV-06: assigning an existing operator to an existing invoice is saved")
    void assignOperateur_bothExist_isSaved() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        f.setArchivee(false);
        Operateur o = new Operateur();
        o.setIdOperateur(2L);
        o.setFactures(new HashSet<Facture>());
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));
        when(operateurRepository.findById(2L)).thenReturn(Optional.of(o));

        factureService.assignOperateurToFacture(2L, 1L);

        assertTrue(o.getFactures().contains(f));
        verify(operateurRepository).save(o);
    }

    @Test
    @DisplayName("NEW-INV-04: assigning an operator to a non-existent invoice is refused")
    void assignOperateur_factureNotFound_isRefused() {
        when(factureRepository.findById(99L)).thenReturn(Optional.empty());
        lenient().when(operateurRepository.findById(2L)).thenReturn(Optional.of(new Operateur()));

        assertThrows(IllegalArgumentException.class,
                () -> factureService.assignOperateurToFacture(2L, 99L));

        verify(operateurRepository, never()).save(any(Operateur.class));
    }

    @Test
    @DisplayName("NEW-INV-04: assigning a non-existent operator to an invoice is refused")
    void assignOperateur_operateurNotFound_isRefused() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        f.setArchivee(false);
        lenient().when(factureRepository.findById(1L)).thenReturn(Optional.of(f));
        when(operateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> factureService.assignOperateurToFacture(99L, 1L));

        verify(operateurRepository, never()).save(any(Operateur.class));
    }

    @Test
    @DisplayName("NEW-INV-05: assigning an operator to a cancelled invoice is refused")
    void assignOperateur_cancelledFacture_isRefused() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        f.setArchivee(true);
        Operateur o = new Operateur();
        o.setIdOperateur(2L);
        o.setFactures(new HashSet<Facture>());
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));
        lenient().when(operateurRepository.findById(2L)).thenReturn(Optional.of(o));

        assertThrows(IllegalArgumentException.class,
                () -> factureService.assignOperateurToFacture(2L, 1L));

        assertTrue(o.getFactures().isEmpty());
        verify(operateurRepository, never()).save(any(Operateur.class));
    }

    // ------------------------------------------------------------------
    // NEW-INV-01 : calcul automatique du montant
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-INV-01: invoice amounts are computed from several details (qty, price, discount)")
    void addFacture_severalDetails_computesAmounts() {
        // détail 1 : 2 x 100 = 200, remise 10% = 20  -> 180
        // détail 2 : 1 x  50 =  50, remise  0% =  0  ->  50
        // facture  : montant = 230, remise = 20
        DetailFacture d1 = buildDetail(10L, 2, 10);
        DetailFacture d2 = buildDetail(20L, 1, 0);
        when(produitRepository.findById(10L)).thenReturn(Optional.of(buildProduit(10L, 100f)));
        when(produitRepository.findById(20L)).thenReturn(Optional.of(buildProduit(20L, 50f)));

        Facture f = new Facture();
        f.setDetailsFacture(new HashSet<DetailFacture>(Arrays.asList(d1, d2)));
        when(factureRepository.save(f)).thenReturn(f);

        Facture result = factureService.addFacture(f);

        assertEquals(230f, result.getMontantFacture(), 0.001f);
        assertEquals(20f, result.getMontantRemise(), 0.001f);
        assertEquals(180f, d1.getPrixTotalDetail(), 0.001f);
        assertEquals(20f, d1.getMontantRemise(), 0.001f);
        assertEquals(50f, d2.getPrixTotalDetail(), 0.001f);
        verify(detailFactureRepository, times(2)).save(any(DetailFacture.class));
        verify(factureRepository).save(f);
    }

    // ------------------------------------------------------------------
    // NEW-INV-02 : cohérence des détails (quantité, remise)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-INV-02: a detail with a quantity <= 0 is refused")
    void addFacture_nonPositiveQuantity_isRefused() {
        lenient().when(produitRepository.findById(10L)).thenReturn(Optional.of(buildProduit(10L, 100f)));
        Facture f = factureWith(buildDetail(10L, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> factureService.addFacture(f));

        verify(factureRepository, never()).save(any(Facture.class));
    }

    @Test
    @DisplayName("NEW-INV-02: a detail with a discount above 100% is refused")
    void addFacture_discountAbove100_isRefused() {
        lenient().when(produitRepository.findById(10L)).thenReturn(Optional.of(buildProduit(10L, 100f)));
        Facture f = factureWith(buildDetail(10L, 1, 101));

        assertThrows(IllegalArgumentException.class, () -> factureService.addFacture(f));

        verify(factureRepository, never()).save(any(Facture.class));
    }

    @Test
    @DisplayName("NEW-INV-02: a detail with a negative discount is refused")
    void addFacture_negativeDiscount_isRefused() {
        lenient().when(produitRepository.findById(10L)).thenReturn(Optional.of(buildProduit(10L, 100f)));
        Facture f = factureWith(buildDetail(10L, 1, -5));

        assertThrows(IllegalArgumentException.class, () -> factureService.addFacture(f));

        verify(factureRepository, never()).save(any(Facture.class));
    }

    // ------------------------------------------------------------------
    // FR-INV-05 / NEW-INV-03 : annulation
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-INV-05: cancelling an active invoice archives it")
    void cancelFacture_active_isArchived() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        f.setArchivee(false);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));

        factureService.cancelFacture(1L);

        assertTrue(Boolean.TRUE.equals(f.getArchivee()));
        verify(factureRepository).save(f);
    }

    @Test
    @DisplayName("NEW-INV-03: cancelling an already cancelled invoice is refused")
    void cancelFacture_alreadyCancelled_isRefused() {
        Facture f = new Facture();
        f.setIdFacture(1L);
        f.setArchivee(true);
        when(factureRepository.findById(1L)).thenReturn(Optional.of(f));

        assertThrows(IllegalArgumentException.class, () -> factureService.cancelFacture(1L));

        verify(factureRepository, never()).save(any(Facture.class));
    }

    @Test
    @DisplayName("NEW-INV-03: cancelling a non-existent invoice is refused (no phantom invoice)")
    void cancelFacture_notFound_isRefused() {
        when(factureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> factureService.cancelFacture(99L));

        verify(factureRepository, never()).save(any(Facture.class));
    }

    // ------------------------------------------------------------------
    // FR-INV-07 : pourcentage de recouvrement
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-INV-07: recovery percentage = payments / invoices * 100")
    void pourcentageRecouvrement_computesPercentage() {
        Date start = new Date(0L);
        Date end = new Date();
        when(factureRepository.getTotalFacturesEntreDeuxDates(start, end)).thenReturn(200f);
        when(reglementService.getChiffreAffaireEntreDeuxDate(start, end)).thenReturn(100f);

        float result = factureService.pourcentageRecouvrement(start, end);

        assertEquals(50f, result, 0.001f);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Produit buildProduit(Long id, float prix) {
        Produit p = new Produit();
        p.setIdProduit(id);
        p.setPrix(prix);
        return p;
    }

    private DetailFacture buildDetail(Long produitId, int qte, int remise) {
        Produit p = new Produit();
        p.setIdProduit(produitId);
        DetailFacture d = new DetailFacture();
        d.setProduit(p);
        d.setQteCommandee(qte);
        d.setPourcentageRemise(remise);
        return d;
    }

    private Facture factureWith(DetailFacture... details) {
        Facture f = new Facture();
        f.setDetailsFacture(new HashSet<DetailFacture>(Arrays.asList(details)));
        return f;
    }
}