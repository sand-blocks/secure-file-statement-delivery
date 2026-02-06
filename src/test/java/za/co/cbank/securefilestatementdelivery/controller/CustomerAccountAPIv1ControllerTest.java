package za.co.cbank.securefilestatementdelivery.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.co.cbank.securefilestatementdelivery.dto.CustomerAccountDTO;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerAccountAPIv1ControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@WithMockUser
	void createCustomerAccount_ShouldReturnCreated() throws Exception {
		CustomerAccountDTO requestDto = CustomerAccountDTO.builder()
				.accountId(12345)
				.firstName("Sarah")
				.lastName("McTesty")
				.emailAddress("sarah.mctesty@cbank.co.za")
				.cellphoneNumber("0712345678")
				.idNumber("90010100000010")
				.build();

		mockMvc.perform(post("/api/v1/customer-accounts/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.firstName").value("Sarah"))
				.andExpect(jsonPath("$.emailAddress").value("sarah.mctesty@cbank.co.za"));
	}

	@Test
	void createAccount_WithoutAuth_ShouldReturn401() throws Exception {
		mockMvc.perform(post("/api/v1/customer-accounts/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}
}