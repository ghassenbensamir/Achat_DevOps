package tn.esprit.rh.achat.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProduitExceptionHandlerTest {
    private final ProduitExceptionHandler handler = new ProduitExceptionHandler();

    @Test
    @DisplayName("NFR-05 / NEW-PROD-01: validation failure is returned as HTTP 400 with its message")
    void handleIllegalArgument_returnsBadRequestWithMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Le prix du produit ne peut pas être négatif : -10.0"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Le prix du produit ne peut pas être négatif : -10.0",
                response.getBody().get("error"));
    }

    @Test
    @DisplayName("NFR-05: a null message still yields HTTP 400 with a default message")
    void handleIllegalArgument_nullMessage_usesDefault() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalArgument(new IllegalArgumentException());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Requête invalide", response.getBody().get("error"));
    }
}