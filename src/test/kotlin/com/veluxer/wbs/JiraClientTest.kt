package com.veluxer.wbs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class JiraClientTest {
    @Autowired
    lateinit var jiraClient: JiraClient

    @Test
    fun test_get_active_sprint() {
        assertDoesNotThrow {
            jiraClient.getActiveSprint(288)
        }
    }

    @Test
    fun test_get_issues_for_sprint() {
        assertDoesNotThrow {
            jiraClient.getIssuesForSprint(816)
        }
    }
}