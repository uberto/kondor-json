allprojects {
    group = "com.ubertob.kondor"
    version = "3.5.2"
}

tasks.wrapper {
    gradleVersion = libs.versions.gradlew.get()
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
