package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.CategorieProduit;
import tn.esprit.rh.achat.entities.Produit;
import tn.esprit.rh.achat.repositories.CategorieProduitRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Module Catégorie : FR-CAT-01..05, NEW-CAT-01, NEW-CAT-02.
 *
 * Hypothèse : CategorieProduitRepository expose
 *     boolean existsByCodeCategorie(String codeCategorie);
 */
@ExtendWith(MockitoExtension.class)
class CategorieProduitServiceImplTest {

    @Mock
    private CategorieProduitRepository categorieProduitRepository;

    @InjectMocks
    private CategorieProduitServiceImpl categorieService;

    // ------------------------------------------------------------------
    // CRUD (FR-CAT-01..05)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-CAT-01: add a category")
    void addCategorie_validData_isSaved() {
        CategorieProduit cp = buildCategorie("C-001", "Informatique");
        when(categorieProduitRepository.save(cp)).thenReturn(cp);

        CategorieProduit result = categorieService.addCategorieProduit(cp);

        assertNotNull(result);
        assertEquals("C-001", result.getCodeCategorie());
        verify(categorieProduitRepository).save(cp);
    }

    @Test
    @DisplayName("FR-CAT-02: retrieve all categories")
    void retrieveAllCategories_returnsAll() {
        List<CategorieProduit> list = Arrays.asList(new CategorieProduit(), new CategorieProduit());
        when(categorieProduitRepository.findAll()).thenReturn(list);

        assertEquals(2, categorieService.retrieveAllCategorieProduits().size());
        verify(categorieProduitRepository).findAll();
    }

    @Test
    @DisplayName("FR-CAT-03: retrieve a category by id")
    void retrieveCategorie_found() {
        CategorieProduit cp = buildCategorie("C-001", "Informatique");
        cp.setIdCategorieProduit(1L);
        when(categorieProduitRepository.findById(1L)).thenReturn(Optional.of(cp));

        CategorieProduit result = categorieService.retrieveCategorieProduit(1L);

        assertEquals(1L, result.getIdCategorieProduit());
    }

    @Test
    @DisplayName("FR-CAT-03: retrieve an unknown category returns null")
    void retrieveCategorie_notFound() {
        when(categorieProduitRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(categorieService.retrieveCategorieProduit(99L));
    }

    @Test
    @DisplayName("FR-CAT-04: update a category")
    void updateCategorie_isSaved() {
        CategorieProduit cp = buildCategorie("C-001", "Libellé modifié");
        cp.setIdCategorieProduit(1L);
        when(categorieProduitRepository.save(cp)).thenReturn(cp);

        CategorieProduit result = categorieService.updateCategorieProduit(cp);

        assertEquals("Libellé modifié", result.getLibelleCategorie());
        verify(categorieProduitRepository).save(cp);
    }

    @Test
    @DisplayName("FR-CAT-05: delete a category that is not used")
    void deleteCategorie_unused_isDeleted() {
        CategorieProduit cp = buildCategorie("C-001", "Informatique");
        cp.setIdCategorieProduit(1L);
        cp.setProduits(new HashSet<Produit>());
        when(categorieProduitRepository.findById(1L)).thenReturn(Optional.of(cp));

        categorieService.deleteCategorieProduit(1L);

        verify(categorieProduitRepository).deleteById(1L);
    }

    // ------------------------------------------------------------------
    // NEW-CAT-01 : unicité du code catégorie
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-CAT-01: adding a category whose code already exists is refused")
    void addCategorie_duplicateCode_isRefused() {
        CategorieProduit cp = buildCategorie("C-001", "Doublon");
        when(categorieProduitRepository.existsCategorieProduitByCodeCategorie("C-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categorieService.addCategorieProduit(cp));

        verify(categorieProduitRepository, never()).save(any(CategorieProduit.class));
    }

    @Test
    @DisplayName("NEW-CAT-01: adding a category with a new code is accepted")
    void addCategorie_uniqueCode_isAccepted() {
        CategorieProduit cp = buildCategorie("C-NEW", "Nouvelle");
        when(categorieProduitRepository.existsCategorieProduitByCodeCategorie("C-NEW")).thenReturn(false);
        when(categorieProduitRepository.save(cp)).thenReturn(cp);

        assertSame(cp, categorieService.addCategorieProduit(cp));
    }

    // ------------------------------------------------------------------
    // NEW-CAT-02 : protection d'une catégorie utilisée par des produits
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-CAT-02: deleting a category used by products is refused")
    void deleteCategorie_usedByProducts_isRefused() {
        CategorieProduit cp = buildCategorie("C-001", "Informatique");
        cp.setIdCategorieProduit(1L);
        cp.setProduits(new HashSet<Produit>(Arrays.asList(new Produit())));
        when(categorieProduitRepository.findById(1L)).thenReturn(Optional.of(cp));

        assertThrows(IllegalArgumentException.class,
                () -> categorieService.deleteCategorieProduit(1L));

        verify(categorieProduitRepository, never()).deleteById(any(Long.class));
    }

    // ------------------------------------------------------------------
    private CategorieProduit buildCategorie(String code, String libelle) {
        CategorieProduit cp = new CategorieProduit();
        cp.setCodeCategorie(code);
        cp.setLibelleCategorie(libelle);
        return cp;
    }
}