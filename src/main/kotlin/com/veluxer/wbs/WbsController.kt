package com.veluxer.wbs

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.time.LocalDateTime
import java.time.ZoneId

@Controller
class WbsController(
    private val jiraClient: JiraClient,
    private val jiraProperties: JiraProperties,
) {
    @GetMapping
    suspend fun index() = "login"

    @GetMapping("/boards")
    suspend fun boards() = "boards"

    @GetMapping("/wbs/{boardId}")
    suspend fun wbs(@PathVariable("boardId") boardId: Int, model: Model): String {
        model.addAttribute("jiraHost", jiraProperties.host)
        model.addAttribute("boardId", boardId)
        return "wbs"
    }

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @GetMapping("/issues/{boardId}")
    @ResponseBody
    suspend fun getIssues(@PathVariable("boardId") boardId: Int): Flow<IssueDto> {
        val sprints = jiraClient.getActiveSprint(boardId)
        val issues = sprints.flatMapMerge {
            jiraClient.getIssuesForSprint(it.id)
        }

        val epics = issues.filter { it.fields.epic != null }.map { it.fields.epic!! }.distinctUntilChanged()
        val tasks = issues
            .filter { it.fields.issuetype.name != "Sub-task" && it.fields.issuetype.name != "Story" }
            .map { IssueDto.from(it) }
        val epicIssue = epics.map { IssueDto.from(it) }
        val unknownEpicIssue = IssueDto.createUnknownEpicIssue()
        val bugEpicIssue = IssueDto.createBugEpicIssue()

        return merge(tasks, epicIssue, flowOf(unknownEpicIssue, bugEpicIssue))
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    val startDate: LocalDateTime?,
    @JsonProperty("end_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    val endDate: LocalDateTime?,
    val duration: Int?,
    val parent: Int,
    val open: Boolean = true,
) {
    companion object {
        fun from(issue: Issue): IssueDto {
            val startDate = issue.fields.startAt?.atStartOfDay()
                ?: issue.fields.created.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDate().atStartOfDay()
            val endDate = issue.fields.endAt?.atTime(23, 59)
                ?: issue.fields.updated.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDate().atTime(23, 59)

            return IssueDto(
                id = issue.id.toInt(),
                key = issue.key,
                type = issue.fields.issuetype.name,
                title = issue.fields.summary,
                assignee = issue.fields.assignee?.displayName.orEmpty(),
                status = issue.fields.status.name,
                estimate = issue.fields.timetracking.originalEstimate.orEmpty(),
                startDate = startDate,
                endDate = endDate,
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
        }

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
            title = "에픽 없는 이슈",
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
