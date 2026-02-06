package za.co.cbank.securefilestatementdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.cbank.securefilestatementdelivery.entity.CustomerAccount;

@Repository
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Integer> {
	CustomerAccount findByAccountId(Integer accountId);
}
