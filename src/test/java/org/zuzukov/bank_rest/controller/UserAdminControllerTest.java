package org.zuzukov.bank_rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zuzukov.bank_rest.controller.UserAdminController;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.service.UserAdminService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserAdminControllerTest {

	private MockMvc mockMvc;
	private UserAdminService userAdminService;

	@BeforeEach
	void setup() {
		userAdminService = Mockito.mock(UserAdminService.class);
		UserAdminController controller = new UserAdminController(userAdminService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new org.zuzukov.bank_rest.exception.GlobalExceptionHandler())
				.build();
	}

	@Test
	void list_success() throws Exception {
		User user = new User();
		user.setUserId(UUID.randomUUID());
		user.setEmail("alice@example.com");

		Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
		when(userAdminService.list(any())).thenReturn(page);

		mockMvc.perform(get("/admin/users?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].email").value("alice@example.com"));
	}

	@Test
	void setRole_success() throws Exception {
		UUID userId = UUID.randomUUID();

		mockMvc.perform(post("/admin/users/" + userId + "/roles/ROLE_ADMIN?enabled=true"))
				.andExpect(status().isOk());

		verify(userAdminService).setRole(userId, Role.ROLE_ADMIN, true);
	}

	@Test
	void delete_success() throws Exception {
		UUID userId = UUID.randomUUID();

		mockMvc.perform(delete("/admin/users/" + userId))
				.andExpect(status().isNoContent());

		verify(userAdminService).delete(userId);
	}
}
