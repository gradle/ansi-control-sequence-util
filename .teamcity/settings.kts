import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.DslContext
import jetbrains.buildServer.configs.kotlin.v2019_2.RelativeId
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.toId
import jetbrains.buildServer.configs.kotlin.v2019_2.version

version = "2025.11"

project {
    buildType {
        name = "Publish ANSI control sequence util"
        id = RelativeId(name.toId())
        description = "Publish ANSI control sequence util to Maven Central staging repository"

        vcs {
            root(DslContext.settingsRoot)
            checkoutMode = CheckoutMode.ON_AGENT
            cleanCheckout = true
        }

        requirements {
            contains("teamcity.agent.jvm.os.name", "Linux")
        }

        steps {
            gradle {
                useGradleWrapper = true
                tasks = "clean publishMavenPublicationToSonatypeRepository"
                gradleParams = "-Prelease=true"
            }
        }
        params {
            param("env.JDK8", "%linux.java8.oracle.64bit%")
            param("env.JAVA_HOME", "%linux.java21.openjdk.64bit%")
            param("env.ORG_GRADLE_PROJECT_sonatypeUsername", "%mavenCentralStagingRepoUser%")
            password("env.ORG_GRADLE_PROJECT_sonatypePassword", "%mavenCentralStagingRepoPassword%")
            password("env.PGP_SIGNING_KEY", "%pgpSigningKey%")
            password("env.PGP_SIGNING_KEY_PASSPHRASE", "%pgpSigningPassphrase%")
        }
    }
}
