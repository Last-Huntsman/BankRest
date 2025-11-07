package org.zuzukov.bank_rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.exception.NotFoundException;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.service.UserAdminService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAdminServiceTest {

	private UserRepository userRepository;
	private UserAdminService service;

	@BeforeEach
	void setup() {
		userRepository = Mockito.mock(UserRepository.class);
		service = new UserAdminService(userRepository);
	}

	@Test
	void list_success() {
		when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new User())));
		Page<User> p = service.list(PageRequest.of(0, 10));
		assertEquals(1, p.getTotalElements());
	}

	@Test
	void setRole_addsRole() {
		UUID id = UUID.randomUUID();
		User u = new User();
		u.setUserId(id);
		u.setRoles(Set.of());
		when(userRepository.findByUserId(id)).thenReturn(Optional.of(u));
		service.setRole(id, org.zuzukov.bank_rest.entity.Role.ROLE_ADMIN, true);
		assertTrue(u.getRoles().contains(org.zuzukov.bank_rest.entity.Role.ROLE_ADMIN));
	}

	@Test
	void setRole_userNotFound() {
		when(userRepository.findByUserId(any())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> service.setRole(UUID.randomUUID(), org.zuzukov.bank_rest.entity.Role.ROLE_ADMIN, true));
	}

	@Test
	void delete_success() {
		UUID id = UUID.randomUUID();
		when(userRepository.existsById(id)).thenReturn(true);
		service.delete(id);
		verify(userRepository).deleteById(id);
	}

	@Test
	void delete_notFound() {
		when(userRepository.existsById(any())).thenReturn(false);
		assertThrows(NotFoundException.class, () -> service.delete(UUID.randomUUID()));
	}
}
