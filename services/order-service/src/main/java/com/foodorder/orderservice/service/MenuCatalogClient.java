package com.foodorder.orderservice.service;

import com.foodorder.orderservice.dto.MenuFoodPayload;
import com.foodorder.orderservice.dto.MenuPriceQuotePayload;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class MenuCatalogClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient menuWebClient;

    public MenuFoodPayload fetchFood(Long menuItemId) {
        try {
            MenuFoodPayload food = menuWebClient
                    .get()
                    .uri("/foods/{id}", menuItemId)
                    .retrieve()
                    .bodyToMono(MenuFoodPayload.class)
                    .block(TIMEOUT);
            if (food == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from menu service");
            }
            return food;
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item not found: " + menuItemId);
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Menu service error: " + e.getStatusCode().value());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Menu service unavailable");
        }
    }

    public MenuPriceQuotePayload quoteFoodPrice(Long menuItemId, String size) {
        String resolvedSize = Optional.ofNullable(size).filter(s -> !s.isBlank()).orElse("M");
        try {
            MenuPriceQuotePayload quote = menuWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/foods/{id}/price")
                            .queryParam("size", resolvedSize)
                            .build(menuItemId))
                    .retrieve()
                    .bodyToMono(MenuPriceQuotePayload.class)
                    .block(TIMEOUT);
            if (quote == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty price quote from menu service");
            }
            return quote;
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item not found: " + menuItemId);
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Menu service error: " + e.getStatusCode().value());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Menu service unavailable");
        }
    }

    public MenuPriceQuotePayload quoteComboPrice(Long comboId) {
        try {
            MenuPriceQuotePayload quote = menuWebClient
                    .get()
                    .uri("/combos/{id}/price", comboId)
                    .retrieve()
                    .bodyToMono(MenuPriceQuotePayload.class)
                    .block(TIMEOUT);
            if (quote == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty price quote from menu service");
            }
            return quote;
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo not found: " + comboId);
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Menu service error: " + e.getStatusCode().value());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Menu service unavailable");
        }
    }
}
