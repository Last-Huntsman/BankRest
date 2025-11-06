package org.zuzukov.bank_rest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zuzukov.bank_rest.dto.card.*;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.service.CardService;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<CardDto> create(@Valid @RequestBody CardCreateDto dto) {
        return ResponseEntity.ok(cardService.adminCreate(dto));
    }

    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(Principal principal, @Valid @RequestBody TransferRequestDto dto) {
        cardService.transferBetweenOwn(principal.getName(), dto);
        return ResponseEntity.ok().build();
    }

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

    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/{id}/request-block")
    public ResponseEntity<Void> requestBlock(Principal principal, @PathVariable UUID id) {
        cardService.userRequestBlock(principal.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable UUID id) {
        cardService.adminBlock(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        cardService.adminActivate(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cardService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }

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
}
