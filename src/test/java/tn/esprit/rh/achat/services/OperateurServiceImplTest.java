package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.Operateur;
import tn.esprit.rh.achat.repositories.OperateurRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Module Opérateur : FR-OP-01..05 (CRUD sans règle complémentaire dans le SRS). */
@ExtendWith(MockitoExtension.class)
class OperateurServiceImplTest {

    @Mock
    private OperateurRepository operateurRepository;

    @InjectMocks
    private OperateurServiceImpl operateurService;

    @Test
    @DisplayName("FR-OP-01: add an operator")
    void addOperateur_isSaved() {
        Operateur o = new Operateur();
        o.setNom("Ben Ali");

        Operateur result = operateurService.addOperateur(o);

        assertSame(o, result);
        verify(operateurRepository).save(o);
    }

    @Test
    @DisplayName("FR-OP-02: retrieve all operators")
    void retrieveAllOperateurs_returnsAll() {
        List<Operateur> list = Arrays.asList(new Operateur(), new Operateur());
        when(operateurRepository.findAll()).thenReturn(list);

        assertEquals(2, operateurService.retrieveAllOperateurs().size());
    }

    @Test
    @DisplayName("FR-OP-03: retrieve an operator by id")
    void retrieveOperateur_found() {
        Operateur o = new Operateur();
        o.setIdOperateur(1L);
        when(operateurRepository.findById(1L)).thenReturn(Optional.of(o));

        assertEquals(1L, operateurService.retrieveOperateur(1L).getIdOperateur());
    }

    @Test
    @DisplayName("FR-OP-03: retrieve an unknown operator returns null")
    void retrieveOperateur_notFound() {
        when(operateurRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(operateurService.retrieveOperateur(99L));
    }

    @Test
    @DisplayName("FR-OP-04: update an operator")
    void updateOperateur_isSaved() {
        Operateur o = new Operateur();
        o.setIdOperateur(1L);
        o.setNom("Modifié");

        Operateur result = operateurService.updateOperateur(o);

        assertEquals("Modifié", result.getNom());
        verify(operateurRepository).save(o);
    }

    @Test
    @DisplayName("FR-OP-05: delete an operator")
    void deleteOperateur_isDeleted() {
        operateurService.deleteOperateur(1L);

        verify(operateurRepository).deleteById(1L);
    }
}