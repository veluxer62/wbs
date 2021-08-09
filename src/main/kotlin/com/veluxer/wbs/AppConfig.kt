package com.veluxer.wbs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.server.DefaultServerRedirectStrategy
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler
import org.springframework.web.client.RestTemplate
import java.net.URI


@Configuration
@EnableConfigurationProperties(JiraProperties::class, AppProperties::class)
class AppConfig {

    @Bean
    fun jiraTemplate(jiraProperties: JiraProperties): RestTemplate {
        return RestTemplateBuilder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .basicAuthentication(jiraProperties.username, jiraProperties.password)
            .rootUri(jiraProperties.host)
            .build()
    }

}

@EnableWebFluxSecurity
class SecurityConfig(private val appProperties: AppProperties) {

    @Bean
    fun configure(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange()
            .pathMatchers("/", "/oauth2/**", "/login/**").permitAll()
            .anyExchange().authenticated()
            .and()
            .logout()
            .logoutUrl("/logout")
            .logoutSuccessHandler(logoutSuccessHandler())
            .and()
            .oauth2Login()
            .authenticationSuccessHandler(authenticationSuccessHandler())
            .authenticationFailureHandler(RedirectServerAuthenticationFailureHandler("/"))
            .and().csrf().disable()
            .build()
    }

    private fun authenticationSuccessHandler() =
        { webFilterExchange: WebFilterExchange, authentication: Authentication ->
            val exchange = webFilterExchange.exchange

            val token = authentication as OAuth2AuthenticationToken
            val email = token.principal.attributes["email"].toString()

            if (!email.endsWith("@${appProperties.allowDomain}")) {
                throw OAuth2AuthenticationException("403")
            }

            DefaultServerRedirectStrategy().sendRedirect(exchange, URI.create("/boards"))
        }

    fun logoutSuccessHandler(): ServerLogoutSuccessHandler {
        val handler = RedirectServerLogoutSuccessHandler()
        handler.setLogoutSuccessUrl(URI.create("/"))
        return handler
    }

}


@ConstructorBinding
@ConfigurationProperties("jira")
data class JiraProperties(
    val host: String,
    val username: String,
    val password: String,
)

@ConstructorBinding
@ConfigurationProperties("app")
data class AppProperties(
    val allowDomain: String,
)