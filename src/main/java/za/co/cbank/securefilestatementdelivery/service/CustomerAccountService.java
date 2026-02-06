package za.co.cbank.securefilestatementdelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import za.co.cbank.securefilestatementdelivery.Audit.annotation.Auditable;
import za.co.cbank.securefilestatementdelivery.dto.CustomerAccountDTO;
import za.co.cbank.securefilestatementdelivery.exception.DatabaseEntityException;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;
import za.co.cbank.securefilestatementdelivery.mapper.CustomerAccountMapper;
import za.co.cbank.securefilestatementdelivery.repository.CustomerAccountRepository;
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAccountService {
	private final CustomerAccountRepository customerAccountRepository;
	private final CustomerAccountMapper customerAccountMapper;

  @Auditable(action = "Created Customer Account")
	public CustomerAccountDTO createCustomerAccount(CustomerAccountDTO customerAccountDTO) {
		try {
			CustomerAccount customerAccount = customerAccountMapper.toEntity(customerAccountDTO);
			CustomerAccount savedCustomerAccount = customerAccountRepository.save(customerAccount);
			log.info("Customer Account: created successfully.");

			return customerAccountMapper.toDto(savedCustomerAccount);
		} catch (DataIntegrityViolationException e) {
			throw new DatabaseEntityException("Database constraint violation: " + e.getMostSpecificCause().getMessage());
		} catch (Exception e) {
			log.error("Customer Account failed to create :", e);
			throw new RuntimeException("An unexpected error occurred.");
		}
	}

	public CustomerAccountDTO getCustomerAccountById(Integer accountId){
		return customerAccountRepository.findById(accountId)
				.map(customerAccountMapper::toDto)
				.orElseThrow( () -> new DatabaseEntityException("Customer account not found"));
	}

}
