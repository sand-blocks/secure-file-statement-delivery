package za.co.cbank.securefilestatementdelivery.controller.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.cbank.securefilestatementdelivery.dto.CustomerAccountDTO;
import za.co.cbank.securefilestatementdelivery.service.CustomerAccountService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/customer-accounts")
@RequiredArgsConstructor
public class CustomerAccountController {

	private final CustomerAccountService customerAccountService;

	@PostMapping(path = "/create",consumes = "application/json")
	public ResponseEntity<CustomerAccountDTO> createCustomerAccount(@RequestBody CustomerAccountDTO customerAccountDTO){
		CustomerAccountDTO createdAccount = customerAccountService.createCustomerAccount(customerAccountDTO);

		return  ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
	}

	@GetMapping("/{accountId}")
	public ResponseEntity<CustomerAccountDTO> getCustomerAccount(@PathVariable Integer accountId) {
		log.info("Fetching customer account: {}", accountId);

		return ResponseEntity.ok(customerAccountService.getCustomerAccountById(accountId));
	}
}
