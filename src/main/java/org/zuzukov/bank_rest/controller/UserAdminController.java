package org.zuzukov.bank_rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.service.UserAdminService;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(
            summary = "Список всех пользователей (только ADMIN)",
            description = "Позволяет админу просматривать всех зарегистрированных пользователей с пагинацией.",
            parameters = {
                    @Parameter(name = "page", description = "Номер страницы (по умолчанию 0)", example = "0"),
                    @Parameter(name = "size", description = "Размер страницы (по умолчанию 10)", example = "10")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список пользователей получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = User.class),
                                    examples = @ExampleObject(value = """
                                        {
                                          "content": [
                                            {
                                              "userId": "1f7f3e4c-bca0-4e5c-9f9a-2b17cfa1c82b",
                                              "firstName": "Alice",
                                              "lastName": "Johnson",
                                              "email": "alice@example.com",
                                              "roles": ["ROLE_USER"]
                                            },
                                            {
                                              "userId": "ff2e8e61-3a2c-41a8-bb0a-3a33cd29e0df",
                                              "firstName": "Bob",
                                              "lastName": "Smith",
                                              "email": "bob@example.com",
                                              "roles": ["ROLE_ADMIN"]
                                            }
                                          ],
                                          "pageable": { "pageNumber": 0, "pageSize": 10 },
                                          "totalElements": 2,
                                          "totalPages": 1
                                        }
                                        """)
                            ))
            }
    )
    @GetMapping
    public ResponseEntity<Page<User>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var pageRes = userAdminService.list(pageable);
        log.debug("ADMIN list users: page={}, size={}, returned={}", page, size, pageRes.getNumberOfElements());
        return ResponseEntity.ok(pageRes);
    }

    @Operation(
            summary = "Назначить или снять роль пользователю (ADMIN)",
            description = """
                    Позволяет администратору включить или выключить конкретную роль пользователю.
                    Например, можно выдать `ROLE_ADMIN` или снять `ROLE_USER`.
                    """,
            parameters = {
                    @Parameter(name = "userId", description = "UUID пользователя", example = "d3f9b660-b8a1-4cf1-bf8a-6d2cf343d2d0"),
                    @Parameter(name = "role", description = "Роль (ROLE_USER, ROLE_ADMIN, ROLE_PREMIUM_USER, ROLE_GUEST)", example = "ROLE_ADMIN"),
                    @Parameter(name = "enabled", description = "true = добавить роль, false = убрать", example = "true")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Роль успешно изменена"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @PostMapping("/{userId}/roles/{role}")
    public ResponseEntity<Void> setRole(
            @PathVariable UUID userId,
            @PathVariable Role role,
            @RequestParam boolean enabled) {
        userAdminService.setRole(userId, role, enabled);
        log.info("ADMIN set role: userId={}, role={}, enabled={}", userId, role, enabled);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Удалить пользователя (ADMIN)",
            description = "Позволяет админу полностью удалить пользователя из системы по его UUID.",
            parameters = @Parameter(name = "userId", description = "UUID пользователя", example = "1f7f3e4c-bca0-4e5c-9f9a-2b17cfa1c82b"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Пользователь успешно удалён"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable UUID userId) {
        userAdminService.delete(userId);
        log.info("ADMIN delete user: userId={}", userId);
        return ResponseEntity.noContent().build();
    }
}
