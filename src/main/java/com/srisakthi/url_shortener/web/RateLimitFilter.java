package com.srisakthi.url_shortener.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.srisakthi.url_shortener.config.UrlShortenerProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Pattern REDIRECT_PATH = Pattern.compile("^/[a-z0-9]{7}$");

	private final UrlShortenerProperties properties;
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public RateLimitFilter(UrlShortenerProperties properties) {
		this.properties = properties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && path.startsWith(context)) {
			path = path.substring(context.length());
		}
		if (path.isEmpty()) {
			path = "/";
		}
		return path.startsWith("/actuator");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && path.startsWith(context)) {
			path = path.substring(context.length());
		}
		if (path.isEmpty()) {
			path = "/";
		}

		String method = request.getMethod();
		LimitKind kind = null;
		if ("POST".equalsIgnoreCase(method) && "/api/v1/urls".equals(path)) {
			kind = LimitKind.CREATE;
		}
		else if ("GET".equalsIgnoreCase(method) && REDIRECT_PATH.matcher(path).matches()) {
			kind = LimitKind.REDIRECT;
		}

		if (kind != null) {
			String ip = clientIp(request);
			Bucket bucket = resolveBucket(ip, kind);
			if (!bucket.tryConsume(1)) {
				response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				response.getWriter().write("rate limit exceeded");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private Bucket resolveBucket(String ip, LimitKind kind) {
		String key = ip + "|" + kind.name();
		return buckets.computeIfAbsent(key, k -> newBucket(kind));
	}

	private Bucket newBucket(LimitKind kind) {
		var rl = properties.getRateLimit();
		long capacity = kind == LimitKind.CREATE ? rl.getCreateCapacity() : rl.getRedirectCapacity();
		long refillMinutes = kind == LimitKind.CREATE ? rl.getCreateRefillMinutes() : rl.getRedirectRefillMinutes();
		Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofMinutes(refillMinutes)));
		return Bucket.builder().addLimit(limit).build();
	}

	private static String clientIp(HttpServletRequest request) {
		String xf = request.getHeader("X-Forwarded-For");
		if (xf != null && !xf.isBlank()) {
			return xf.split(",")[0].trim();
		}
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}

	private enum LimitKind {
		CREATE,
		REDIRECT
	}

}
