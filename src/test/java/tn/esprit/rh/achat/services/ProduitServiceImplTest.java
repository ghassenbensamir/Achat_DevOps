package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rh.achat.entities.Produit;
import tn.esprit.rh.achat.entities.Stock;
import tn.esprit.rh.achat.repositories.CategorieProduitRepository;
import tn.esprit.rh.achat.repositories.ProduitRepository;
import tn.esprit.rh.achat.repositories.StockRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceImplTest {

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CategorieProduitRepository categorieProduitRepository;

    @InjectMocks
    private ProduitServiceImpl produitService;

    @Test
    void retrieveAllProduits() {
        List<Produit> produits = Arrays.asList(
                new Produit(),
                new Produit()
        );

        when(produitRepository.findAll()).thenReturn(produits);

        List<Produit> result = produitService.retrieveAllProduits();

        assertEquals(2, result.size());
        assertSame(produits, result);
        verify(produitRepository).findAll();
    }

    @Test
    @DisplayName("NEW-PROD-01: adding a product with a valid data saves it")
    void addProduit() {
        Produit produit = new Produit();
        produit.setLibelleProduit("Ordinateur");

        when(produitRepository.save(produit)).thenReturn(produit);

        Produit result = produitService.addProduit(produit);

        assertNotNull(result);
        assertSame(produit, result);
        verify(produitRepository).save(produit);
    }

    @Test
    void deleteProduit() {
        long id = 1L;

        produitService.deleteProduit(id);

        verify(produitRepository).deleteById(id);

    }

    @Test
    void updateProduit() {
        Produit produit = new Produit();
        produit.setIdProduit(1L);
        produit.setLibelleProduit("Ordinateur modifié");

        when(produitRepository.save(produit)).thenReturn(produit);

        Produit result = produitService.updateProduit(produit);

        assertNotNull(result);
        assertEquals("Ordinateur modifié", result.getLibelleProduit());
        verify(produitRepository).save(produit);
    }

    @Test
    void retrieveProduit() {
        Long id = 1L;
        Produit produit = new Produit();
        produit.setIdProduit(id);

        when(produitRepository.findById(id))
                .thenReturn(Optional.of(produit));

        Produit result = produitService.retrieveProduit(id);

        assertNotNull(result);
        assertEquals(id, result.getIdProduit());
        verify(produitRepository).findById(id);
    }

    @Test
    @DisplayName("FR-PROD-01: Fetch inexistant product ")
    void testRetrieveProduitNotFound() {
        Long id = 99L;

        when(produitRepository.findById(id))
                .thenReturn(Optional.empty());

        Produit result = produitService.retrieveProduit(id);

        assertNull(result);
        verify(produitRepository).findById(id);
    }
    @Test
    @DisplayName("FR-PROD-06: assigning a product to an existing stock saves the association")
    void assignProduitToStock() {
        Long idProduit = 1L;
        Long idStock = 2L;

        Produit produit = new Produit();
        produit.setIdProduit(idProduit);
        Stock stock = new Stock();
        stock.setIdStock(idStock);

        when(produitRepository.findById(idProduit)).thenReturn(Optional.of(produit));
        when(stockRepository.findById(idStock)).thenReturn(Optional.of(stock));

        produitService.assignProduitToStock(idProduit, idStock);

        assertSame(stock, produit.getStock());
        verify(produitRepository).save(produit);
    }

    @Test
    @DisplayName("NEW-PROD-01: adding a product with a negative price is refused")
    void addProduit_negativePrice_isRefused() {
        Produit produit =  new  Produit();
        produit.setIdProduit(2L);
        produit.setPrix(-10f);
        produit.setLibelleProduit("Souris");

        assertThrows(IllegalArgumentException.class,
                () -> produitService.addProduit(produit));

        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    @DisplayName("NEW-PROD-01: updating a product with a negative price is refused")
    void updateProduit_negativePrice_isRefused() {
        Produit produit = new Produit();
        produit.setPrix(-10f);
        produit.setLibelleProduit("Souris");
        produit.setIdProduit(1L);

        assertThrows(IllegalArgumentException.class,
                () -> produitService.updateProduit(produit));

        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    @DisplayName("NEW-PROD-01 (boundary): a price of 0 is not negative, so it is accepted")
    void addProduit_zeroPrice_isAccepted() {
        Produit produit = new Produit();
        produit.setPrix(0f);
        produit.setLibelleProduit("Souris");
        produit.setIdProduit(1L);
        when(produitRepository.save(produit)).thenReturn(produit);

        Produit result = produitService.addProduit(produit);

        assertNotNull(result);
        verify(produitRepository).save(produit);
    }

    @Test
    @DisplayName("NEW-PROD-02: adding a product whose code already exists is refused")
    void addProduit_duplicateCode_isRefused() {
        Produit produit =new  Produit();
        produit.setPrix(900f);
        produit.setLibelleProduit("Autre ordinateur");
        produit.setIdProduit(5L);
        produit.setCodeProduit("2-L");
        when(produitRepository.findByCodeProduit("2-L")).thenReturn(produit);

        assertThrows(IllegalArgumentException.class,
                () -> produitService.addProduit(produit));

        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    @DisplayName("NEW-PROD-03: assigning to a non-existent stock is refused")
    void assignProduitToStock_stockNotFound_isRefused() {
        Long idProduit = 1L;
        Long idStock = 99L;

        Produit produit = new Produit();
        produit.setIdProduit(idProduit);

        lenient().when(produitRepository.findById(idProduit)).thenReturn(Optional.of(produit));
        when(stockRepository.findById(idStock)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> produitService.assignProduitToStock(idProduit, idStock));

        assertNull(produit.getStock());
        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    @DisplayName("NEW-PROD-03: assigning a non-existent product to a stock is refused")
    void assignProduitToStock_produitNotFound_isRefused() {
        Long idProduit = 99L;
        Long idStock = 2L;

        when(produitRepository.findById(idProduit)).thenReturn(Optional.empty());
        lenient().when(stockRepository.findById(idStock)).thenReturn(Optional.of(new Stock()));

        assertThrows(IllegalArgumentException.class,
                () -> produitService.assignProduitToStock(idProduit, idStock));

        verify(produitRepository, never()).save(any(Produit.class));
    }
}