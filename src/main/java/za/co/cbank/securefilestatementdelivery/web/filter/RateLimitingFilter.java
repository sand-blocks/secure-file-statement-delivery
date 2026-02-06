package za.co.cbank.securefilestatementdelivery.web.filter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class RateLimitingFilter implements Filter {

	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	private Bucket createNewBucket() {
		Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
		return Bucket4j.builder().addLimit(limit).build();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String ip = getClientIP(httpRequest);

		Bucket bucket = buckets.computeIfAbsent(ip, k -> createNewBucket());

		if (bucket.tryConsume(1)) {
			chain.doFilter(request, response);
		} else {
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			httpResponse.setStatus(429);
			httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "60"); // Optional hint
			httpResponse.getWriter().write("Too many requests from IP: " + ip);
		}
	}

	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null || xfHeader.isEmpty()) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0]; // Use the first IP in the list
	}
}