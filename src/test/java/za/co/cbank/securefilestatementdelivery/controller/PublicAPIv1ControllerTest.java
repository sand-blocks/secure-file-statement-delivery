package za.co.cbank.securefilestatementdelivery.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;
import za.co.cbank.securefilestatementdelivery.controller.api.v1.PublicController;
import za.co.cbank.securefilestatementdelivery.service.StatementService;
import za.co.cbank.securefilestatementdelivery.web.filter.AuditContextFilter;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PublicController.class)
@AutoConfigureMockMvc
@WithMockUser
class PublicAPIv1ControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StatementService statementService;

	@Test
	void shouldDownloadPdfSuccessfully() throws Exception {
		UUID token = UUID.randomUUID();
		byte[] mockPdfContent = "fake pdf content".getBytes();
		when(statementService.downloadStatementUsingToken(token.toString()))
				.thenReturn(Mono.just(mockPdfContent));

		MvcResult result = mockMvc.perform(get("/api/v1/public/" + token))
				.andExpect(request().asyncStarted())
				.andReturn();

		mockMvc.perform(asyncDispatch(result))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
				.andExpect(content().bytes(mockPdfContent));
	}

	@Test
	void shouldReturnBadRequestOnInvalidUUID() throws Exception {
		UUID token = UUID.randomUUID();
		when(statementService.downloadStatementUsingToken(token.toString()))
				.thenThrow(new IllegalArgumentException("Invalid UUID"));

		mockMvc.perform(get("/api/v1/public/444" + token))
				.andExpect(status().isBadRequest());
	}
}