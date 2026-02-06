package za.co.cbank.securefilestatementdelivery.Audit.aspect.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import za.co.cbank.securefilestatementdelivery.Audit.annotation.Auditable;
import za.co.cbank.securefilestatementdelivery.Audit.service.AuditLogService;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
	private final AuditLogService auditLogService;
	private final ExpressionParser parser = new SpelExpressionParser();
	private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

	@AfterReturning(value = "@annotation(auditable)", returning = "result")
	public void auditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
		String detail = auditable.detail();

		if (!detail.isEmpty()) {
			StandardEvaluationContext context = new StandardEvaluationContext();
			Object[] args = joinPoint.getArgs();
			String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

			for (int i = 0; i < args.length; i++) {
				context.setVariable(paramNames[i], args[i]);
			}

			detail = parser.parseExpression(detail).getValue(context, String.class);
		}
		auditLogService.logAction(
				MDC.get("traceId"),
				MDC.get("currentUser"),
				MDC.get("ipAddress"),
				auditable.action() + ": " + detail
		);
	}
}
