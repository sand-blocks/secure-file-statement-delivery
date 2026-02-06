package za.co.cbank.securefilestatementdelivery.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import za.co.cbank.securefilestatementdelivery.service.StatementService;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/public")
public class PublicController {


	private final StatementService statementService;

	@GetMapping(path = "/{retrievalToken}")
	public Mono<ResponseEntity<byte[]>> getStatementByLink(@PathVariable UUID retrievalToken){
		try {
		Mono<byte[]> fileBody = statementService.downloadStatementUsingToken(retrievalToken.toString());
			return fileBody.map(bytes -> ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
					.body(bytes));
		}
		catch (Exception e){
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
		}

	}
}
