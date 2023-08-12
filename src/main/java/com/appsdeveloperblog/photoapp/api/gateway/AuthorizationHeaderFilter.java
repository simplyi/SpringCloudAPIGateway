package com.appsdeveloperblog.photoapp.api.gateway;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.google.common.net.HttpHeaders;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.Arrays;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
	
	@Autowired
	Environment env;
	
	public AuthorizationHeaderFilter() {
		super(Config.class);
	}
	
	public static class Config {
		private List<String> authorities;
//		private String role;
//		private String authority;

		public List<String> getAuthorities() {
			return authorities;
		}

		public void setAuthorities(String authorities) {
			this.authorities = Arrays.asList(authorities.split(" "));
		}

//		private String getRole() {
//			return role;
//		}
//
//		public void setRole(String role) {
//			this.role = role;
//		}
//
//		private String getAuthority() {
//			return authority;
//		}
//
//		public void setAuthority(String authority) {
//			this.authority = authority;
//		}
		
	}
	
	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("authorities");
	}
	
//	@Override
//	public List<String> shortcutFieldOrder() {
//		return Arrays.asList("role","authority");
//	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			
			ServerHttpRequest request = exchange.getRequest();
			
			if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
				return onError(exchange, "No authorization header",HttpStatus.UNAUTHORIZED);
			}
			
			String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
			String jwt = authorizationHeader.replace("Bearer", "");
			
			List<String> authorities = getAuthorities(jwt);
			
	        boolean hasRequiredAuthority = authorities.stream()
	        		.anyMatch(authority->config.getAuthorities().contains(authority));
	        
	        if(!hasRequiredAuthority) 
	        	return onError(exchange,"User is not authorized to perform this operation", HttpStatus.FORBIDDEN);
	        
//			if(!isJwtValid(jwt)) {
//				return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
//			}
			
			return chain.filter(exchange);
		};
	}
	
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        return response.setComplete();
    }
    
	private List<String> getAuthorities(String jwt) {
		List<String> returnValue = new ArrayList<>();

		String tokenSecret = env.getProperty("token.secret");
		byte[] secretKeyBytes = Base64.getEncoder().encode(tokenSecret.getBytes());
		SecretKey signingKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());

		JwtParser jwtParser = Jwts.parserBuilder()
				.setSigningKey(signingKey)
				.build();

		try {

			Jwt<Header, Claims> parsedToken = jwtParser.parse(jwt);
			List<Map<String, String>> scopes = ((Claims)parsedToken.getBody()).get("scope", List.class);
			scopes.stream().map(scopeMap -> returnValue.add(scopeMap.get("authority"))).collect(Collectors.toList());
			

		} catch (Exception ex) {
			return returnValue;
		}


		return returnValue;
	}
	
//	  
//		private boolean isJwtValid(String jwt) {
//			boolean returnValue = true;
//
//			String subject = null;
//			String tokenSecret = env.getProperty("token.secret");
//			byte[] secretKeyBytes = Base64.getEncoder().encode(tokenSecret.getBytes());
//			SecretKey signingKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());
//
//			JwtParser jwtParser = Jwts.parserBuilder()
//					.setSigningKey(signingKey)
//					.build();
//
//			try {
//
//				Jwt<Header, Claims> parsedToken = jwtParser.parse(jwt);
//				subject = parsedToken.getBody().getSubject();
//
//			} catch (Exception ex) {
//				returnValue = false;
//			}
//
//			if (subject == null || subject.isEmpty()) {
//				returnValue = false;
//			}
//
//			return returnValue;
//		}

}
