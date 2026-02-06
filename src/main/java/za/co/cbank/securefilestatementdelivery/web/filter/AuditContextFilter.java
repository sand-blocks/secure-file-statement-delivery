package za.co.cbank.securefilestatementdelivery.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuditContextFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null) ip = request.getRemoteAddr();


		String user = (SecurityContextHolder.getContext().getAuthentication() != null)
				? SecurityContextHolder.getContext().getAuthentication().getName()
				: "anonymous";
		try {
			MDC.put("ipAddress", ip);
			MDC.put("currentUser", user);

			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
