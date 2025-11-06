package com.nhahang.restaurant.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.nhahang.restaurant.model.entity.Role;
import com.nhahang.restaurant.model.entity.User;
import com.nhahang.restaurant.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenString = header.substring(7); 

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(tokenString);
            String email = decodedToken.getEmail();
            
            if (email != null) {
                
                User user = userRepository.findByEmail(email).orElse(null);

                // 2. KIỂM TRA USER VÀ ROLE
                if (user != null && user.getRole() != null) { 
                    Role userRole = user.getRole(); //

                    // 3. LẤY TẤT CẢ PERMISSIONS TỪ ROLE ĐÓ
                    // (Hibernate tự tải 'permissions' vì chúng ta đặt FetchType.EAGER)
                    Collection<? extends GrantedAuthority> authorities = userRole.getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                        .collect(Collectors.toList());
                    
                    // 4. TẠO AUTHENTICATION VÀ ĐẶT VÀO SECURITYCONTEXT
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user, null, authorities); 
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // Token không hợp lệ hoặc hết hạn
            logger.error("Firebase token verification failed", e);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}