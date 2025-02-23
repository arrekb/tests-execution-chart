package com.github.platan.tests_execution_chart.reporters.mermaid

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.Test
import com.github.platan.tests_execution_chart.TestExecutionScheduleReport
import com.github.platan.tests_execution_chart.config.Mermaid
import com.github.platan.tests_execution_chart.reporters.GanttDiagramReporter

internal class MermaidTestsReporter(private val config: Mermaid, private val logger: Logger) : GanttDiagramReporter() {

    override fun report(report: TestExecutionScheduleReport, task: Test) {
        val mermaidReport = TestExecutionMermaidDiagramFormatter().format(report)
        val reportFile = save(task, mermaidReport, config.outputLocation.get(), "txt")
        logger.lifecycle("Tests execution schedule report saved to ${reportFile.absolutePath} file.")
    }

}