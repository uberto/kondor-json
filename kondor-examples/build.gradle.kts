import me.champeau.jmh.JmhParameters
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `common-kotlin`
    alias(libs.plugins.jmh)
}

configure<JmhParameters> {
    iterations = 10
    // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    benchmarkMode = listOf("thrpt", "avgt")
    // Batch size: number of benchmark method calls per operation. (some benchmark modes can ignore this setting)
    batchSize = 1
    fork = 2 // How many times to forks a single benchmark. Use 0 to disable forking altogether
//    profilers = listOf("async")
//    Unable to load async-profiler. Ensure asyncProfiler library is on LD_LIBRARY_PATH (Linux), DYLD_LIBRARY_PATH (Mac OS), or -Djava.library.path. Alternatively, point to explicit library location with -prof async:libPath=<path>.
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(project(":kondor-core"))
    implementation(libs.kotlin.jdk8)
    implementation(libs.http4k)
    implementation(libs.jackson)
    implementation(libs.striKt)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.jUnit.get())
            dependencies {
                implementation(project(":kondor-tools"))
            }
            targets {
                all {
                    testTask.configure {
                        maxHeapSize = "2g"
                        testLogging {
//                            showStandardStreams = true
                            events = setOf(
                                TestLogEvent.SKIPPED,
                                TestLogEvent.FAILED,
                                TestLogEvent.PASSED
                            )
                        }
                    }
                }
            }
        }
    }
}
