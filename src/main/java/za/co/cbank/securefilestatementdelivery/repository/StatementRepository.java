package za.co.cbank.securefilestatementdelivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.cbank.securefilestatementdelivery.entity.Statement;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Integer> {
	Statement findByRetrievalToken(String retrievalToken);
}
