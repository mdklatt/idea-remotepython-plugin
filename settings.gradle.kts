sourceControl {
    // https://docs.gradle.org/current/javadoc/org/gradle/vcs/SourceControl.html
    gitRepository(uri("https://github.com/mdklatt/idea-common.git")) {
        producesModule("dev.mdklatt:idea-common")  // <groupId>:<artifactId>
    }
}
