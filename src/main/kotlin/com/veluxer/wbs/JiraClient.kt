package com.veluxer.wbs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.ZonedDateTime

@Component
class JiraClient(private val jiraTemplate: RestTemplate) {

    fun getActiveSprint(boardId: Int): List<Sprint> {
        val response = jiraTemplate.getForEntity(
            "/rest/agile/1.0/board/$boardId/sprint?state=active",
            AllSprintsResponseBody::class.java
        )
        return response.body?.sprints.orEmpty()
    }

    fun getIssuesForSprint(sprintId: Int): List<Issue> {
        val response = jiraTemplate.getForEntity(
            "/rest/agile/1.0/sprint/$sprintId/issue?maxResults=10000",
            IssuesForSprintResponseBody::class.java
        )
        return response.body?.issues.orEmpty()
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssuesForSprintResponseBody(
    val issues: List<Issue>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Issue(
    val id: String,
    val self: String,
    val key: String,
    val fields: IssueFields,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssueFields(
    val summary: String,
    val issuetype: IssueType,
    val epic: Epic? = null,
    val assignee: Assignee? = null,
    val status: Status,
    val timetracking: Timetracking,
    @JsonProperty("customfield_11811")
    val startAt: LocalDate? = null,
    @JsonProperty("customfield_11300")
    val endAt: LocalDate? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Timetracking(
    val originalEstimate: String? = null,
    val remainingEstimate: String? = null,
    val originalEstimateSeconds: Long? = null,
    val remainingEstimateSeconds: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Status(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Assignee(
    val displayName: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Epic(
    val id: Int,
    val key: String,
    val self: String,
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssueType(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AllSprintsResponseBody(
    @JsonProperty("values")
    val sprints: List<Sprint>
)

data class Sprint(
    val id: Int,
    val self: String,
    val state: SprintState,
    val name: String,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val originBoardId: Int,
)

@Suppress("EnumEntryName")
enum class SprintState {
    future,
    active,
    closed
}