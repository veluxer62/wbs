package com.veluxer.wbs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.CharacterEncodingFilter


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

@Configuration
@EnableWebSecurity
class SecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        val filter = CharacterEncodingFilter()
        http
            .authorizeRequests()
            .antMatchers("/", "/oauth2/**", "/login/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .defaultSuccessUrl("/login")
            .failureUrl("/")
            .and()
            .headers().frameOptions().disable()
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/"))
            .and()
            .formLogin()
            .successForwardUrl("/wbs")
            .and()
            .logout()
            .logoutUrl("/logout")
            .logoutSuccessUrl("/")
            .deleteCookies("JSESSIONID")
            .invalidateHttpSession(true)
            .and()
            .addFilterBefore(filter, CsrfFilter::class.java)
            .csrf().disable()
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