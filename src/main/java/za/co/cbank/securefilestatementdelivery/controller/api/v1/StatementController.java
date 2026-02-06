package za.co.cbank.securefilestatementdelivery.controller.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.cbank.securefilestatementdelivery.dto.StatementDTO;
import za.co.cbank.securefilestatementdelivery.service.StatementService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
public class StatementController {
	private final StatementService statementService;

	@PostMapping(path = "/create",consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String, Object>> generateStatement(@RequestBody StatementDTO statementDTO){
		log.info("Request to generate statement for accountId: {}", statementDTO.getAccountId());
		Map<String, Object> statementResult = statementService.generateStatement(
				statementDTO.getAccountId());
		return  ResponseEntity.status(HttpStatus.CREATED).body(statementResult);
	}
}
