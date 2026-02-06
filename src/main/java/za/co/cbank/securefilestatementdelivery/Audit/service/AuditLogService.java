package za.co.cbank.securefilestatementdelivery.Audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.cbank.securefilestatementdelivery.Audit.entity.AuditLog;
import za.co.cbank.securefilestatementdelivery.Audit.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

//	@Async
//	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logAction(String traceId, String username, String ipAddress, String metadata) {
		AuditLog log = new AuditLog();
		log.setTraceId(traceId);
		log.setSystemUsername(username);
		log.setIpAddress(ipAddress);
		log.setMetadata(metadata);

		auditLogRepository.save(log);
	}
}
