package za.co.cbank.securefilestatementdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.cbank.securefilestatementdelivery.entity.Transaction;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
	List<Transaction> findByCustomerAccount_AccountId(Integer customerAccountAccountId);

	List<Transaction> findByCustomerAccount_AccountId_OrderByPostDate(Integer accountId);
}
