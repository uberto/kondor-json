allprojects {
    group = "com.ubertob.kondor"
    version = "4.1.0"
}

tasks.wrapper {
    gradleVersion = libs.versions.gradlew.get()
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
