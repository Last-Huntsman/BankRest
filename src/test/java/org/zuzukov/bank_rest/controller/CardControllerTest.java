package org.zuzukov.bank_rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zuzukov.bank_rest.controller.CardController;
import org.zuzukov.bank_rest.dto.card.CardCreateDto;
import org.zuzukov.bank_rest.dto.card.CardDto;
import org.zuzukov.bank_rest.dto.card.TransferRequestDto;
import org.zuzukov.bank_rest.entity.CardStatus;
import org.zuzukov.bank_rest.service.CardService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CardControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private CardService cardService;

	@BeforeEach
	void setup() {
		cardService = Mockito.mock(CardService.class);
		CardController controller = new CardController(cardService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new org.zuzukov.bank_rest.exception.GlobalExceptionHandler())
				.build();
		objectMapper = new ObjectMapper();
	}

	@Test
	void create_success() throws Exception {
		CardCreateDto req = new CardCreateDto();
		req.setOwnerEmail("john.doe@example.com");
		req.setCardNumber("4111111111111111");
		req.setExpiryMonth(12);
		req.setExpiryYear(2030);
		req.setInitialBalance(new BigDecimal("1000.0"));

		CardDto resp = new CardDto();
		resp.setId(UUID.randomUUID());
		resp.setOwnerEmail("john.doe@example.com");

		when(cardService.adminCreate(any())).thenReturn(resp);

		mockMvc.perform(post("/cards")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ownerEmail").value("john.doe@example.com"));
	}

	@Test
	void transfer_success() throws Exception {
		TransferRequestDto dto = new TransferRequestDto();
		dto.setFromCardId(UUID.randomUUID());
		dto.setToCardId(UUID.randomUUID());
		dto.setAmount(new BigDecimal("250.50"));

		mockMvc.perform(post("/cards/transfer")
					.principal((Principal) () -> "user@example.com")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk());

		verify(cardService).transferBetweenOwn(eq("user@example.com"), any(TransferRequestDto.class));
	}

	@Test
	void listOwn_success() throws Exception {
		CardDto c = new CardDto();
		c.setId(UUID.randomUUID());
		c.setOwnerEmail("user@example.com");
		Page<CardDto> page = new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1);
		when(cardService.userListOwn(eq("user@example.com"), isNull(), any())).thenReturn(page);

		mockMvc.perform(get("/cards?page=0&size=10")
					.principal((Principal) () -> "user@example.com"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].ownerEmail").value("user@example.com"));
	}

	@Test
	void requestBlock_success() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(post("/cards/" + id + "/request-block")
					.principal((Principal) () -> "user@example.com"))
				.andExpect(status().isOk());

		verify(cardService).userRequestBlock("user@example.com", id);
	}

	@Test
	void block_success() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(post("/cards/" + id + "/block"))
				.andExpect(status().isOk());

		verify(cardService).adminBlock(id);
	}

	@Test
	void activate_success() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(post("/cards/" + id + "/activate"))
				.andExpect(status().isOk());

		verify(cardService).adminActivate(id);
	}

	@Test
	void delete_success() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/cards/" + id))
				.andExpect(status().isNoContent());

		verify(cardService).adminDelete(id);
	}

	@Test
	void adminList_success() throws Exception {
		CardDto c = new CardDto();
		c.setId(UUID.randomUUID());
		c.setOwnerEmail("admin-filter@example.com");
		Page<CardDto> page = new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1);
		when(cardService.adminSearch(eq("admin@example.com"), eq(CardStatus.ACTIVE), eq("1234"), any()))
				.thenReturn(page);

		mockMvc.perform(get("/cards/admin")
					.param("ownerEmail", "admin@example.com")
					.param("status", "ACTIVE")
					.param("last4", "1234")
					.param("page", "0")
					.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].ownerEmail").value("admin-filter@example.com"));
	}
}

