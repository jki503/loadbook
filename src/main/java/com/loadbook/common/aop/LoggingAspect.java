package com.loadbook.common.aop;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(2)
@Component
@RequiredArgsConstructor
public class LoggingAspect {

	private static final String LOG_FORMAT = "Request : {}";
	private static final String ERROR_LOG_FORMAT = "API 로그를 생성하는 과정에서 문제가 생겼습니다.";
	private final ObjectMapper objectMapper;

	@Around("execution(* com.loadbook.domain..controller.*Controller.*(..))")
	public Object printLog(ProceedingJoinPoint joinPoint) throws Throwable {
		HttpServletRequest request = ((ServletRequestAttributes)Objects.requireNonNull(
			RequestContextHolder.getRequestAttributes())).getRequest();
		log.info(LOG_FORMAT, getMetaData(request));
		return joinPoint.proceed();
	}

	private String getMetaData(HttpServletRequest request) throws JsonProcessingException {
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			map.put("ip", getClientIp(request));
			map.put("method", request.getMethod());
			map.put("uri", request.getRequestURI());
			map.put("params", getParams(request));
			map.put("body", getBody(request));
			map.put("time", new Date());
		} catch (Exception e) {
			log.error(ERROR_LOG_FORMAT, e);
			return null;
		}

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");

		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		return ip;
	}

	private String getParams(HttpServletRequest request) throws JsonProcessingException {
		return objectMapper.writeValueAsString(request.getParameterMap());
	}

	private JsonNode getBody(HttpServletRequest request) throws IOException {
		ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper)request;
		return objectMapper.readTree(wrapper.getContentAsByteArray());
	}
}
