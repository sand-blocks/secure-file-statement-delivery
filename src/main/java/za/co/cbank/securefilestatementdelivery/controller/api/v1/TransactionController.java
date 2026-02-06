package za.co.cbank.securefilestatementdelivery.controller.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.cbank.securefilestatementdelivery.dto.TransactionDTO;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;
import za.co.cbank.securefilestatementdelivery.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionService transactionService;

	@GetMapping("/account/{accountId}")
	public ResponseEntity<List<TransactionDTO>> getTransactionsByAccountId(@PathVariable Integer accountId) {
		return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
	}

	@GetMapping("/{transactionId}")
	public ResponseEntity<TransactionDTO> getTransactionsByTransactionId(@PathVariable Integer transactionId) {
		return ResponseEntity.ok(transactionService.getTransactionByTransactionId(transactionId));
	}

	@PostMapping(path = "/create",consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<TransactionDTO> createTransaction(@RequestBody TransactionDTO transactionDTO){
		log.info("Creating new transaction for account: {}", transactionDTO.getAccountId());
		TransactionDTO createdTransaction = transactionService.createTransaction(transactionDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
	}
}
