allprojects {
    group = "com.ubertob.kondor"
    version = "2.3.2"
}

tasks.wrapper {
    gradleVersion = libs.versions.gradlew.get()
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
