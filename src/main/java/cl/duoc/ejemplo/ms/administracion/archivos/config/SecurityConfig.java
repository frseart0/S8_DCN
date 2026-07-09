package cl.duoc.ejemplo.ms.administracion.archivos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad con Microsoft Entra ID (semanas 6 y 8).
 *
 * Todos los endpoints exigen un JWT válido emitido por el tenant configurado
 * (spring.security.oauth2.resourceserver.jwt.issuer-uri). Los App Roles del
 * app registration llegan en el claim "roles" y se mapean a authorities con
 * prefijo ROLE_:
 *
 * - GUIA_LECTOR: solo puede usar el endpoint de descarga de guías.
 * - GUIA_OPERADOR: puede usar el resto de los endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/guias/*/descargar").hasRole("GUIA_LECTOR")
						.anyRequest().hasRole("GUIA_OPERADOR"))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	/**
	 * Convierte el claim "roles" del token de Entra ID en authorities de
	 * Spring Security con prefijo ROLE_.
	 */
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {

		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return converter;
	}
}
