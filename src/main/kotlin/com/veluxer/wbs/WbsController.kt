package com.veluxer.wbs

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.time.LocalDate

@Controller
class WbsController(private val jiraClient: JiraClient, private val jiraProperties: JiraProperties) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("jiraHost", jiraProperties.host)
        return "index"
    }

    @GetMapping("/issues/{boardId}")
    @ResponseBody
    fun getIssues(@PathVariable("boardId") boardId: Int): List<IssueDto> {
        val sprints = jiraClient.getActiveSprint(boardId)
        val issues = sprints.flatMap { jiraClient.getIssuesForSprint(it.id) }
        val epics = issues.filter { it.fields.epic != null }.map { it.fields.epic!! }.distinct()

        val tasks = issues.filter { it.fields.issuetype.name != "Sub-task" }.map { IssueDto.from(it) }
        val epicIssue = epics.map { IssueDto.from(it) }
        val unknownEpicIssue = IssueDto.createUnknownEpicIssue()
        val bugEpicIssue = IssueDto.createBugEpicIssue()

        return tasks + epicIssue + unknownEpicIssue + bugEpicIssue
    }
}

data class IssueDto(
    val id: Int,
    val key: String,
    val type: String,
    val title: String,
    val assignee: String,
    val status: String,
    val estimate: String,
    @JsonProperty("start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate?,
    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate?,
    val duration: Int?,
    val parent: Int,
    val open: Boolean = true
) {
    companion object {
        fun from(issue: Issue) = IssueDto(
            id = issue.id.toInt(),
            key = issue.key,
            type = issue.fields.issuetype.name,
            title = issue.fields.summary,
            assignee = issue.fields.assignee?.displayName.orEmpty(),
            status = issue.fields.status.name,
            estimate = issue.fields.timetracking.originalEstimate.orEmpty(),
            startDate = issue.fields.startAt,
            endDate = issue.fields.endAt,
            duration = if (issue.fields.endAt != null && issue.fields.startAt != null) {
                issue.fields.endAt.compareTo(issue.fields.startAt).plus(1)
            } else null,
            parent = if (issue.fields.epic != null) {
                issue.fields.epic.id
            } else if (issue.fields.issuetype.name == "Bug") {
                Int.MAX_VALUE - 1
            } else {
                Int.MAX_VALUE
            }
        )

        fun from(epic: Epic) = IssueDto(
            id = epic.id,
            key = epic.key,
            type = "epic",
            title = epic.name,
            assignee = "",
            status = "",
            estimate = "",
            startDate = null,
            endDate = null,
            duration = 0,
            parent = 0
        )

        fun createUnknownEpicIssue() = IssueDto(
            id = Int.MAX_VALUE,
            key = "NO-EPIC",
            type = "epic",
            title = "백로그 없는 이슈",
            assignee = "",
            status = "",
            estimate = "",
            startDate = null,
            endDate = null,
            duration = 0,
            parent = 0
        )

        fun createBugEpicIssue() = IssueDto(
            id = Int.MAX_VALUE - 1,
            key = "BUG-EPIC",
            type = "epic",
            title = "QA 이슈",
            assignee = "",
            status = "",
            estimate = "",
            startDate = null,
            endDate = null,
            duration = 0,
            parent = 0
        )
    }
}