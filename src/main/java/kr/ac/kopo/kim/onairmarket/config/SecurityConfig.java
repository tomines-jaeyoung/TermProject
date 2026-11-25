package kr.ac.kopo.kim.onairmarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로그인: members 테이블(MemberService.loadUserByUsername)로 아이디·비밀번호를 확인한다.
 * 목록·상세·영상 보기는 누구나, 판매 등록·입찰·관심 저장·내 정보는 로그인 후에만 가능하다.
 * 관리자 화면(/admin/**)은 role 이 ADMIN 인 회원만, 이용 정지된 회원은 로그인할 수 없다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 로그인이 필요한 주소만 지정하고 나머지(목록·상세·없는 주소의 404 화면 등)는 누구나 볼 수 있다
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/sell", "/sell/**", "/my", "/auctions/*/bid", "/auctions/*/bookmark",
                                "/products/*/buy").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", false)
                        .failureHandler((request, response, ex) -> response.sendRedirect(request.getContextPath()
                                + (ex instanceof DisabledException ? "/login?suspended" : "/login?error")))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout"));
        return http.build();
    }
}
