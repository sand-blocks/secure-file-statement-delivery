package za.co.cbank.securefilestatementdelivery.BlackBoxTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatementGenerationBlackBoxTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser(username = "admin", roles = {"USER", "ADMIN"})
	void generateStatement_ShouldReturnRetrievalLink() throws Exception {
		String requestJson = "{\"accountId\":1000000003}";

		mockMvc.perform(post("/api/v1/statements/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.retrievalLink").exists())
				.andExpect(jsonPath("$.retrievalLink").value(containsString("/api/v1/public")));
	}
}
