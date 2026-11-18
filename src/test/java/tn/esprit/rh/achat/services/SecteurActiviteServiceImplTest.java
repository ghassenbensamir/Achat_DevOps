package tn.esprit.rh.achat.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rh.achat.entities.SecteurActivite;
import tn.esprit.rh.achat.repositories.SecteurActiviteRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Module Secteur d'activité : FR-SEC-01..05 (CRUD sans règle complémentaire dans le SRS). */
@ExtendWith(MockitoExtension.class)
class SecteurActiviteServiceImplTest {

    @Mock
    private SecteurActiviteRepository secteurActiviteRepository;

    @InjectMocks
    private SecteurActiviteServiceImpl secteurService;

    @Test
    @DisplayName("FR-SEC-01: add a sector")
    void addSecteur_isSaved() {
        SecteurActivite sa = new SecteurActivite();
        sa.setLibelleSecteurActivite("Informatique");

        SecteurActivite result = secteurService.addSecteurActivite(sa);

        assertSame(sa, result);
        verify(secteurActiviteRepository).save(sa);
    }

    @Test
    @DisplayName("FR-SEC-02: retrieve all sectors")
    void retrieveAllSecteurs_returnsAll() {
        List<SecteurActivite> list = Arrays.asList(new SecteurActivite(), new SecteurActivite());
        when(secteurActiviteRepository.findAll()).thenReturn(list);

        assertEquals(2, secteurService.retrieveAllSecteurActivite().size());
    }

    @Test
    @DisplayName("FR-SEC-03: retrieve a sector by id")
    void retrieveSecteur_found() {
        SecteurActivite sa = new SecteurActivite();
        sa.setIdSecteurActivite(1L);
        when(secteurActiviteRepository.findById(1L)).thenReturn(Optional.of(sa));

        assertEquals(1L, secteurService.retrieveSecteurActivite(1L).getIdSecteurActivite());
    }

    @Test
    @DisplayName("FR-SEC-03: retrieve an unknown sector returns null")
    void retrieveSecteur_notFound() {
        when(secteurActiviteRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(secteurService.retrieveSecteurActivite(99L));
    }

    @Test
    @DisplayName("FR-SEC-04: update a sector")
    void updateSecteur_isSaved() {
        SecteurActivite sa = new SecteurActivite();
        sa.setIdSecteurActivite(1L);
        sa.setLibelleSecteurActivite("Modifié");

        SecteurActivite result = secteurService.updateSecteurActivite(sa);

        assertEquals("Modifié", result.getLibelleSecteurActivite());
        verify(secteurActiviteRepository).save(sa);
    }

    @Test
    @DisplayName("FR-SEC-05: delete a sector")
    void deleteSecteur_isDeleted() {
        secteurService.deleteSecteurActivite(1L);

        verify(secteurActiviteRepository).deleteById(1L);
    }
}