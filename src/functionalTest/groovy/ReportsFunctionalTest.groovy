import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ReportsFunctionalTest extends Specification {
    @TempDir
    File testProjectDir
    File settingsFile
    File buildFile
    File specFile

    def setup() {
        settingsFile = new File(testProjectDir, 'settings.gradle')
        buildFile = new File(testProjectDir, 'build.gradle')
        specFile = new File(testProjectDir, 'src/test/groovy/MySpec.groovy')
        specFile.parentFile.mkdirs()
    }

    def "generate reports in all formats"() {
        given:
        settingsFile << "rootProject.name = 'hello-world'"
        buildFile << """
            plugins {
                id 'groovy'
                id 'com.github.platan.tests-execution-chart'
            }
            createTestsExecutionReport {
                formats {
                    html { enabled = true }
                    json { enabled = true }
                    mermaid { enabled = true }
                }
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                testImplementation 'org.codehaus.groovy:groovy-all:3.0.10'
                testImplementation platform('org.spockframework:spock-bom:2.1-groovy-3.0')
                testImplementation 'org.spockframework:spock-core'
            }
            test {
                useJUnitPlatform()
            }
        """
        specFile.text = new File('src/functionalTest/resources/MySpec.groovy').text

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('test', 'createTestsExecutionReport')
                .withPluginClasspath()
                .build()

        then:
        result.task(":createTestsExecutionReport").outcome == SUCCESS
        result.output.contains("Tests execution schedule report for task 'test'")
        result.output.contains("Tests execution schedule report saved to $projectDirRealPath/build/reports/tests-execution/mermaid/test.txt file.")
        result.output.contains("Tests execution schedule report saved to $projectDirRealPath/build/reports/tests-execution/json/test.json file.")
        result.output.contains("Tests execution schedule report saved to $projectDirRealPath/build/reports/tests-execution/html/test.html file.")
        result.output.contains('BUILD SUCCESSFUL')

        and:
        new File("$projectDirRealPath/build/reports/tests-execution/mermaid/test.txt").exists()
        new File("$projectDirRealPath/build/reports/tests-execution/json/test.json").exists()
        new File("$projectDirRealPath/build/reports/tests-execution/html/test.html").exists()
    }

    def "should replace special characters in mermaid graph test names"() {
        given:
        settingsFile << "rootProject.name = 'hello-world'"
        buildFile << """
            plugins {
                id 'groovy'
                id 'com.github.platan.tests-execution-chart'
            }
            createTestsExecutionReport {
                formats {
                    html { enabled = true }
                    json { enabled = true }
                    mermaid { enabled = true }
                }
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                testImplementation 'org.codehaus.groovy:groovy-all:3.0.10'
                testImplementation platform('org.spockframework:spock-bom:2.1-groovy-3.0')
                testImplementation 'org.spockframework:spock-core'
            }
            test {
                useJUnitPlatform()
            }
        """

        specFile << """
            import spock.lang.Specification

            class Test1Spec extends Specification {
            
                def "test with # character"() {
                    expect:
                    true
                }
                def "test with : character"() {
                    expect:
                    true
                }

            }        
        """

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments('test', 'createTestsExecutionReport')
            .withPluginClasspath()
            .build()

        then:
        result.task(":createTestsExecutionReport").outcome == SUCCESS

        and:
        def mermaidGraph = new File("$projectDirRealPath/build/reports/tests-execution/mermaid/test.txt").text
        !mermaidGraph.contains("test with # character")
        !mermaidGraph.contains("test with : character")
        mermaidGraph.contains("test with  character")
        mermaidGraph.contains("test with #colon; character")
    }

    def "should escape special characters in html table test names"() {
        given:
        settingsFile << "rootProject.name = 'hello-world'"
        buildFile << """
            plugins {
                id 'groovy'
                id 'com.github.platan.tests-execution-chart'
            }
            createTestsExecutionReport {
                formats {
                    html { enabled = true }
                    json { enabled = false }
                    mermaid { enabled = false }
                }
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                testImplementation 'org.codehaus.groovy:groovy-all:3.0.10'
                testImplementation platform('org.spockframework:spock-bom:2.1-groovy-3.0')
                testImplementation 'org.spockframework:spock-core'
            }
            test {
                useJUnitPlatform()
            }
        """

        specFile << """
            import spock.lang.Specification

            class Test1Spec extends Specification {
                def "test with <tag/> character"() {
                    expect:
                    true
                }
            }        
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('test', 'createTestsExecutionReport')
                .withPluginClasspath()
                .build()

        then:
        result.task(":createTestsExecutionReport").outcome == SUCCESS

        and:
        def html = new File("$projectDirRealPath/build/reports/tests-execution/html/test.html").text
        html.contains("&lt;tag/&gt;")
        !html.contains("<tag/>>")
    }

    def "display info about the lack of reports"() {
        given:
        settingsFile << "rootProject.name = 'hello-world'"
        buildFile.text = new File('src/functionalTest/resources/build.gradle').text

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('test', 'createTestsExecutionReport')
                .withPluginClasspath()
                .build()

        then:
        result.task(":createTestsExecutionReport").outcome == SUCCESS
        result.output.contains("Task was run but it hasn't created any reports. Possible reasons:")
        result.output.contains('BUILD SUCCESSFUL')

        and:
        !new File("$projectDirRealPath/build/reports/tests-execution/").exists()
    }

    // @TempDir creates directories in /var, but on macOS /var is a link to /private/var
    // so we have to use toRealPath to get a real path which is logged
    private Path getProjectDirRealPath() {
        testProjectDir.toPath().toRealPath()
    }
}
