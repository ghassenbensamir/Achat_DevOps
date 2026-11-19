package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.Produit;
import tn.esprit.rh.achat.entities.Stock;
import tn.esprit.rh.achat.repositories.StockRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Module Stock : FR-STK-01..05, NEW-STK-01, NEW-STK-02, NEW-STK-04.
 * NEW-STK-03 (exposition REST) se teste dans un test de contrôleur.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    // ------------------------------------------------------------------
    // CRUD (FR-STK-01..05)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("FR-STK-01: add a stock with valid data")
    void addStock_validData_isSaved() {
        Stock stock = buildStock("Stock A", 10, 5);
        when(stockRepository.save(stock)).thenReturn(stock);

        Stock result = stockService.addStock(stock);

        assertSame(stock, result);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("FR-STK-02: retrieve all stocks")
    void retrieveAllStocks_returnsAll() {
        List<Stock> stocks = Arrays.asList(buildStock("A", 1, 1), buildStock("B", 2, 1));
        when(stockRepository.findAll()).thenReturn(stocks);

        List<Stock> result = stockService.retrieveAllStocks();

        assertEquals(2, result.size());
        verify(stockRepository).findAll();
    }

    @Test
    @DisplayName("FR-STK-03: retrieve a stock by id")
    void retrieveStock_found() {
        Stock stock = buildStock("A", 1, 1);
        stock.setIdStock(1L);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        Stock result = stockService.retrieveStock(1L);

        assertNotNull(result);
        assertEquals(1L, result.getIdStock());
    }

    @Test
    @DisplayName("FR-STK-03: retrieve an unknown stock returns null")
    void retrieveStock_notFound() {
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(stockService.retrieveStock(99L));
    }

    @Test
    @DisplayName("FR-STK-04: update a stock")
    void updateStock_validData_isSaved() {
        Stock stock = buildStock("A modifié", 20, 5);
        stock.setIdStock(1L);
        when(stockRepository.save(stock)).thenReturn(stock);

        Stock result = stockService.updateStock(stock);

        assertEquals("A modifié", result.getLibelleStock());
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("FR-STK-05: delete an empty stock")
    void deleteStock_emptyStock_isDeleted() {
        Stock stock = buildStock("Vide", 0, 0);
        stock.setIdStock(1L);
        stock.setProduits(new HashSet<Produit>());
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        stockService.deleteStock(1L);

        verify(stockRepository).deleteById(1L);
    }

    // ------------------------------------------------------------------
    // NEW-STK-01 : quantités négatives refusées
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-STK-01: creating a stock with a negative quantity is refused")
    void addStock_negativeQte_isRefused() {
        Stock stock = buildStock("A", -1, 5);

        assertThrows(IllegalArgumentException.class, () -> stockService.addStock(stock));

        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("NEW-STK-01: creating a stock with a negative minimum quantity is refused")
    void addStock_negativeQteMin_isRefused() {
        Stock stock = buildStock("A", 10, -1);

        assertThrows(IllegalArgumentException.class, () -> stockService.addStock(stock));

        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("NEW-STK-01: updating a stock with a negative quantity is refused")
    void updateStock_negativeQte_isRefused() {
        Stock stock = buildStock("A", -5, 5);
        stock.setIdStock(1L);

        assertThrows(IllegalArgumentException.class, () -> stockService.updateStock(stock));

        verify(stockRepository, never()).save(any(Stock.class));
    }

    @Test
    @DisplayName("NEW-STK-01 (boundary): quantities equal to 0 are accepted")
    void addStock_zeroQuantities_isAccepted() {
        Stock stock = buildStock("A", 0, 0);
        when(stockRepository.save(stock)).thenReturn(stock);

        assertNotNull(stockService.addStock(stock));
        verify(stockRepository).save(stock);
    }

    // ------------------------------------------------------------------
    // NEW-STK-02 : détection des stocks faibles
    // (le filtrage qte < qteMin est dans la requête JPQL du repository :
    //  à valider par un test @DataJpaTest, pas par un mock)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-STK-02: the status message lists each low stock with its quantities")
    void retrieveStatusStock_lowStocks_messageContainsEachStock() {
        Stock s1 = buildStock("Stock A", 5, 10);
        Stock s2 = buildStock("Stock B", 1, 4);
        when(stockRepository.retrieveStatusStock()).thenReturn(Arrays.asList(s1, s2));

        String message = stockService.retrieveStatusStock();

        assertTrue(message.contains("le stock Stock A a une quantité de 5"));
        assertTrue(message.contains("quantité minimale a ne pas dépasser de 10"));
        assertTrue(message.contains("le stock Stock B a une quantité de 1"));
        assertTrue(message.contains("quantité minimale a ne pas dépasser de 4"));
    }

    @Test
    @DisplayName("NEW-STK-02: no low stock gives an empty message")
    void retrieveStatusStock_noLowStock_emptyMessage() {
        when(stockRepository.retrieveStatusStock()).thenReturn(Collections.<Stock>emptyList());

        assertEquals("", stockService.retrieveStatusStock());
    }

    // ------------------------------------------------------------------
    // NEW-STK-04 : suppression d'un stock qui contient encore des produits
    // (règle retenue ici : refus. À confirmer, cf. SRS 4.8)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NEW-STK-04: deleting a stock that still contains products is refused")
    void deleteStock_withProducts_isRefused() {
        Stock stock = buildStock("Plein", 10, 5);
        stock.setIdStock(1L);
        stock.setProduits(new HashSet<Produit>(Arrays.asList(new Produit())));
        when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

        assertThrows(IllegalArgumentException.class, () -> stockService.deleteStock(1L));

        verify(stockRepository, never()).deleteById(any(Long.class));
    }

    // ------------------------------------------------------------------
    private Stock buildStock(String libelle, int qte, int qteMin) {
        Stock stock = new Stock();
        stock.setLibelleStock(libelle);
        stock.setQte(qte);
        stock.setQteMin(qteMin);
        return stock;
    }
}