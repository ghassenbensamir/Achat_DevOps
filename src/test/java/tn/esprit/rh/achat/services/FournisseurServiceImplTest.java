package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.DetailFournisseur;
import tn.esprit.rh.achat.entities.Fournisseur;
import tn.esprit.rh.achat.entities.SecteurActivite;
import tn.esprit.rh.achat.repositories.DetailFournisseurRepository;
import tn.esprit.rh.achat.repositories.FournisseurRepository;
import tn.esprit.rh.achat.repositories.ProduitRepository;
import tn.esprit.rh.achat.repositories.SecteurActiviteRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Module Fournisseur : FR-SUP-*, NEW-SUP-01, NEW-SUP-02.
 *
 * Hypothèse : FournisseurRepository expose
 *     boolean existsByCode(String code);
 * (le "code" est utilisé comme identifiant métier unique).
 */
@ExtendWith(MockitoExtension.class)
class FournisseurServiceImplTest {

    @Mock
    private FournisseurRepository fournisseurRepository;
    @Mock
    private DetailFournisseurRepository detailFournisseurRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private SecteurActiviteRepository secteurActiviteRepository;

    @InjectMocks
    private FournisseurServiceImpl fournisseurService;

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-SUP: add a supplier creates its DetailFournisseur and saves it")
    void addFournisseur_createsDetailAndSaves() {
        Fournisseur f = buildFournisseur("F-001", "Fournisseur A");

        Fournisseur result = fournisseurService.addFournisseur(f);

        assertNotNull(result.getDetailFournisseur());
        assertNotNull(result.getDetailFournisseur().getDateDebutCollaboration());
        verify(fournisseurRepository).save(f);
    }

    @Test
    @DisplayName("FR-SUP: retrieve all suppliers")
    void retrieveAllFournisseurs_returnsAll() {
        List<Fournisseur> list = Arrays.asList(new Fournisseur(), new Fournisseur());
        when(fournisseurRepository.findAll()).thenReturn(list);

        assertEquals(2, fournisseurService.retrieveAllFournisseurs().size());
    }

    @Test
    @DisplayName("FR-SUP: retrieve a supplier by id")
    void retrieveFournisseur_found() {
        Fournisseur f = buildFournisseur("F-001", "A");
        f.setIdFournisseur(1L);
        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(f));

        assertEquals(1L, fournisseurService.retrieveFournisseur(1L).getIdFournisseur());
    }

    @Test
    @DisplayName("FR-SUP: retrieve an unknown supplier returns null")
    void retrieveFournisseur_notFound() {
        when(fournisseurRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(fournisseurService.retrieveFournisseur(99L));
    }

    @Test
    @DisplayName("FR-SUP: update saves the detail then the supplier")
    void updateFournisseur_savesDetailAndSupplier() {
        Fournisseur f = buildFournisseur("F-001", "A modifié");
        f.setIdFournisseur(1L);
        DetailFournisseur df = new DetailFournisseur();
        f.setDetailFournisseur(df);

        Fournisseur result = fournisseurService.updateFournisseur(f);

        assertSame(f, result);
        verify(detailFournisseurRepository).save(df);
        verify(fournisseurRepository).save(f);
    }

    @Test
    @DisplayName("FR-SUP: delete a supplier")
    void deleteFournisseur_isDeleted() {
        fournisseurService.deleteFournisseur(1L);

        verify(fournisseurRepository).deleteById(1L);
    }

    // ------------------------------------------------------------------
    // NEW-SUP-01 : unicité de l'identifiant métier
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-SUP-01: adding a supplier whose code already exists is refused")
    void addFournisseur_duplicateCode_isRefused() {
        Fournisseur f = buildFournisseur("F-001", "Doublon");
        when(fournisseurRepository.existsFournisseurByCode(("F-001"))).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> fournisseurService.addFournisseur(f));

        verify(fournisseurRepository, never()).save(any(Fournisseur.class));
    }

    @Test
    @DisplayName("NEW-SUP-01: adding a supplier with a new code is accepted")
    void addFournisseur_uniqueCode_isAccepted() {
        Fournisseur f = buildFournisseur("F-NEW", "Nouveau");
        when(fournisseurRepository.existsFournisseurByCode("F-NEW")).thenReturn(false);

        assertNotNull(fournisseurService.addFournisseur(f));
        verify(fournisseurRepository).save(f);
    }

    // ------------------------------------------------------------------
    // NEW-SUP-02 : affectation d'un secteur d'activité
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-SUP-02: assigning an existing sector to an existing supplier is saved")
    void assignSecteur_bothExist_isSaved() {
        Fournisseur f = buildFournisseur("F-001", "A");
        f.setIdFournisseur(1L);
        f.setSecteurActivites(new HashSet<SecteurActivite>());
        SecteurActivite sa = new SecteurActivite();
        sa.setIdSecteurActivite(2L);

        when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(f));
        when(secteurActiviteRepository.findById(2L)).thenReturn(Optional.of(sa));

        fournisseurService.assignSecteurActiviteToFournisseur(2L, 1L);

        assertTrue(f.getSecteurActivites().contains(sa));
        verify(fournisseurRepository).save(f);
    }

    @Test
    @DisplayName("NEW-SUP-02: assigning to a non-existent supplier is refused")
    void assignSecteur_fournisseurNotFound_isRefused() {
        when(fournisseurRepository.findById(99L)).thenReturn(Optional.empty());
        lenient().when(secteurActiviteRepository.findById(2L))
                .thenReturn(Optional.of(new SecteurActivite()));

        assertThrows(IllegalArgumentException.class,
                () -> fournisseurService.assignSecteurActiviteToFournisseur(2L, 99L));

        verify(fournisseurRepository, never()).save(any(Fournisseur.class));
    }

    @Test
    @DisplayName("NEW-SUP-02: assigning a non-existent sector is refused")
    void assignSecteur_secteurNotFound_isRefused() {
        Fournisseur f = buildFournisseur("F-001", "A");
        f.setIdFournisseur(1L);
        f.setSecteurActivites(new HashSet<SecteurActivite>());
        lenient().when(fournisseurRepository.findById(1L)).thenReturn(Optional.of(f));
        when(secteurActiviteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> fournisseurService.assignSecteurActiviteToFournisseur(99L, 1L));

        assertTrue(f.getSecteurActivites().isEmpty());
        verify(fournisseurRepository, never()).save(any(Fournisseur.class));
    }

    // ------------------------------------------------------------------
    private Fournisseur buildFournisseur(String code, String libelle) {
        Fournisseur f = new Fournisseur();
        f.setCode(code);
        f.setLibelle(libelle);
        return f;
    }
}