package org.zuzukov.bank_rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.zuzukov.bank_rest.dto.card.*;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.service.CardService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @Operation(
            summary = "Создать карту (ADMIN)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CardCreateDto.class),
                            examples = @ExampleObject(value = """
                                {
                                  "ownerEmail": "john.doe@example.com",
                                  "cardNumber": "4111111111111111",
                                  "expiryMonth": 12,
                                  "expiryYear": 2028,
                                  "initialBalance": 1500.00
                                }
                                """)
                    )
            ),
            responses = @ApiResponse(responseCode = "200", description = "Карта создана")
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<CardDto> create(@Valid @RequestBody CardCreateDto dto) {
        return ResponseEntity.ok(cardService.adminCreate(dto));
    }

    @Operation(
            summary = "Перевод между картами пользователя",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = TransferRequestDto.class),
                            examples = @ExampleObject(value = """
                                {
                                  "fromCardId": "6d7b9cb3-8d0f-4c15-aaa9-5a7b88dc4b5a",
                                  "toCardId": "ad9eaa2e-34b4-4d55-b6d1-66b14f8f902a",
                                  "amount": 250.50
                                }
                                """)
                    )
            ),
            responses = @ApiResponse(responseCode = "200", description = "Перевод выполнен")
    )
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(Principal principal, @Valid @RequestBody TransferRequestDto dto) {
        cardService.transferBetweenOwn(principal.getName(), dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Список карт пользователя (пагинация)")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<CardDto>> listOwn(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CardStatus status
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cardService.userListOwn(principal.getName(), status, pageable));
    }

    @Operation(summary = "Пользователь запрашивает блокировку карты")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/{id}/request-block")
    public ResponseEntity<Void> requestBlock(Principal principal, @PathVariable UUID id) {
        cardService.userRequestBlock(principal.getName(), id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Админ блокирует карту (ADMIN)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable UUID id) {
        cardService.adminBlock(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Активировать карту (ADMIN)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        cardService.adminActivate(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить карту (ADMIN)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Поиск карт (ADMIN)")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<Page<CardDto>> adminList(
            @RequestParam(required = false) String ownerEmail,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String last4,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cardService.adminSearch(ownerEmail, status, last4, pageable));
    }
    @Operation(summary = "Посмотреть общий баланс")
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @GetMapping("/balance/total")
    public ResponseEntity<BigDecimal> getTotalBalance(Authentication authentication) {
        String email = authentication.getName();
        BigDecimal total = cardService.getUserTotalBalance(email);
        return ResponseEntity.ok(total);
    }
}
