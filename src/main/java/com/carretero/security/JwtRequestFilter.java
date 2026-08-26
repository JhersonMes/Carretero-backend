package com.carretero.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService jwtUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String header = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (header != null && header.startsWith("Bearer ")) {
            final int TOKEN_INDEX = 7;
            jwtToken = header.substring(TOKEN_INDEX);

            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (Exception ex) {
                request.setAttribute("msg", ex.getMessage());
            }
        }

        if (username != null && jwtToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/")
                || path.equals("/index.html")
                || path.equals("/login")
                || path.equals("/register")
                || path.startsWith("/static")
                || path.startsWith("/assets")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".html")
                || path.endsWith(".json")
                || path.endsWith(".ico");
    }
}
