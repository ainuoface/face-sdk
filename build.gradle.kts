plugins {
    `maven-publish`
}

val sdkVersion = "v0.1.9"
val publicationVersion = System.getenv("VERSION") ?: sdkVersion
val sdkBinaryVersion = "0.1.9"
group = "com.github.ainuoface"
version = publicationVersion

val sdkArtifact = layout.projectDirectory.file(
    "maven/com/ainuo/face-sdk/$sdkBinaryVersion/face-sdk-$sdkBinaryVersion.aar"
)

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.ainuoface"
            artifactId = "face-sdk"
            version = publicationVersion

            artifact(sdkArtifact) {
                extension = "aar"
            }

            pom {
                name.set("Ainuo Face Android SDK")
                description.set("Ainuo Face SDK Android binary distribution.")
                packaging = "aar"
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    fun dependency(groupId: String, artifactId: String, version: String) {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", groupId)
                        dependencyNode.appendNode("artifactId", artifactId)
                        dependencyNode.appendNode("version", version)
                        dependencyNode.appendNode("scope", "runtime")
                    }
                    dependency("androidx.camera", "camera-core", "1.4.2")
                    dependency("androidx.camera", "camera-camera2", "1.4.2")
                    dependency("androidx.camera", "camera-lifecycle", "1.4.2")
                    dependency("androidx.camera", "camera-view", "1.4.2")
                    dependency("androidx.activity", "activity", "1.9.3")
                    dependency("com.google.mlkit", "face-detection", "16.1.7")
                    dependency("com.microsoft.onnxruntime", "onnxruntime-android", "1.20.0")
                    dependency("androidx.room", "room-runtime", "2.5.2")
                    dependency("androidx.core", "core-ktx", "1.13.1")
                    dependency("com.google.android.gms", "play-services-tasks", "18.2.0")
                }
            }
        }
    }
}

tasks.register("verifySdkArtifact") {
    inputs.file(sdkArtifact)
    doLast {
        check(sdkArtifact.asFile.isFile) {
            "Missing SDK binary: ${sdkArtifact.asFile.absolutePath}"
        }
    }
}

tasks.register<Copy>("copyReleaseArtifactForJitPack") {
    dependsOn("verifySdkArtifact")
    inputs.property("publicationVersion", publicationVersion)
    from(sdkArtifact)
    into(layout.buildDirectory.dir("libs"))
    rename { "face-sdk-$publicationVersion.aar" }
}

tasks.named("publishReleasePublicationToMavenLocal") {
    dependsOn("verifySdkArtifact", "copyReleaseArtifactForJitPack")
}
