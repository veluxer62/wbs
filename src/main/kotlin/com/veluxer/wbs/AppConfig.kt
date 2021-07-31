package com.veluxer.wbs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(JiraProperties::class)
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

@ConstructorBinding
@ConfigurationProperties("jira")
data class JiraProperties(
    val host: String,
    val username: String,
    val password: String,
)