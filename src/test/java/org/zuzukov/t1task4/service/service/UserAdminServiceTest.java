package org.zuzukov.t1task4.service.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.exception.NotFoundException;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.UserAdminService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class UserAdminServiceTest {
    @Test
    void list_ok() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        UserAdminService service = new UserAdminService(repo);
        Page<User> page = new PageImpl<>(List.of(new User()));
        when(repo.findAll(PageRequest.of(0, 10))).thenReturn(page);
        Page<User> out = service.list(PageRequest.of(0, 10));
        assertEquals(1, out.getTotalElements());
    }

    @Test
    void setRole_userNotFound() {
        UserRepository repo = Mockito.mock(UserRepository.class);
        UserAdminService service = new UserAdminService(repo);
        UUID id = UUID.randomUUID();
        when(repo.findByUserId(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.setRole(id, Role.ROLE_USER, true));
    }
}


