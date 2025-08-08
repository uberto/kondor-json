allprojects {
    group = "com.ubertob.kondor"
    version = "4.0.0-beta-1"
}

tasks.wrapper {
    gradleVersion = libs.versions.gradlew.get()
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
