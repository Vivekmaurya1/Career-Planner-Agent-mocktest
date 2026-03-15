package com.example.demo.Config;

import com.example.demo.dtos.UserInfoResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Component
public class AuthForwardFilter extends GenericFilter {

    @Value("${auth.service.url:http://localhost:8080}")
    private String authServiceUrl;

    // Reuse a single RestTemplate — thread-safe
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("[AuthFilter] Authorization header found, validating token...");
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", authHeader);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<UserInfoResponse> resp = restTemplate.exchange(
                        authServiceUrl + "/api/auth/me",
                        HttpMethod.GET,
                        entity,
                        UserInfoResponse.class
                );

                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    UserInfoResponse userInfo = resp.getBody();
                    log.info("[AuthFilter] ✓ Token valid | user='{}' email='{}'",
                            userInfo.getName(), userInfo.getEmail());

                    // Store both email and name as request attributes
                    request.setAttribute("authenticatedUser",      userInfo.getEmail());
                    request.setAttribute("authenticatedUserName",  userInfo.getName());
                    request.setAttribute("authenticatedUserId",    userInfo.getId());
                } else {
                    log.warn("[AuthFilter] Auth service returned non-2xx: {}", resp.getStatusCode());
                }

            } catch (HttpClientErrorException.Unauthorized e) {
                log.warn("[AuthFilter] Token is invalid or expired — proceeding as anonymous");
            } catch (HttpClientErrorException.Forbidden e) {
                log.warn("[AuthFilter] Token forbidden — proceeding as anonymous");
            } catch (Exception e) {
                // Don't block the request — auth is optional for mocktest endpoints
                log.warn("[AuthFilter] Could not validate token (auth service unavailable?): {}", e.getMessage());
            }
        } else {
            log.debug("[AuthFilter] No Authorization header — proceeding as anonymous");
        }

        chain.doFilter(request, response);
    }
}