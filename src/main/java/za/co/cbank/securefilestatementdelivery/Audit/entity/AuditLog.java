package za.co.cbank.securefilestatementdelivery.Audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "audit_id")
	private Integer auditId;
	@Column(name = "timestamp", nullable = false)
	@CreationTimestamp
	private LocalDateTime timestamp;
	@Column(name = "trace_id")
	private String traceId;
	@Column(name = "ip_address")
	private String ipAddress;
	@Column(name = "system_username")
	private String systemUsername;
	@Column(name = "metadata")
	private String metadata;

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getSystemUsername() {
		return systemUsername;
	}

	public void setSystemUsername(String systemUsername) {
		this.systemUsername = systemUsername;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}