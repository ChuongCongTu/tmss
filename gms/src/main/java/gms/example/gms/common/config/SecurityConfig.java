package gms.example.gms.common.config;

import gms.example.gms.common.security.JwtAuthenticationFilter;
import gms.example.gms.staff.enums.StaffRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Public: login / register ---
                        .requestMatchers("/api/auth/**").permitAll()

                        // --- Đọc (GET): mọi role đã đăng nhập đều xem được dữ liệu vận hành ---
                        // (customer, vehicle, part/tồn kho, repair order, invoice)
                        .requestMatchers(HttpMethod.GET,
                                "/api/customers/**",
                                "/api/parts/**",
                                "/api/repair-orders/**",
                                "/api/invoices/**",
                                "/api/*/repair-orders")   // GET /api/{vehicleId}/repair-orders
                        .hasAnyRole(
                                StaffRole.RECEPTIONIST.name(),
                                StaffRole.TECHNICIAN.name(),
                                StaffRole.MANAGER.name())

                        // --- Kho & phụ tùng (ghi): chỉ MANAGER ---
                        // Tạo/sửa part, điều chỉnh tồn kho
                        .requestMatchers(HttpMethod.POST, "/api/parts/**").hasRole(StaffRole.MANAGER.name())

                        // --- Tiếp khách (ghi): RECEPTIONIST hoặc MANAGER ---
                        // Tạo customer, thêm xe, tạo phiếu sửa, xuất hóa đơn, thanh toán
                        .requestMatchers(HttpMethod.POST,
                                "/api/customers/**",
                                "/api/repair-orders/**",
                                "/api/invoices/**")
                        .hasAnyRole(StaffRole.RECEPTIONIST.name(), StaffRole.MANAGER.name())

                        // --- Còn lại (gồm quản nhân viên sau này): mặc định cần đăng nhập ---
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
