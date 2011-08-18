import sbt._
import Keys._

object ProjectBuild extends Build
{
   val jetty7version = "7.4.2.v20110526"

   lazy val root = Project("testserver", file(".")) settings(
      name := "Jetty Test Server With Echo",
      libraryDependencies ++= Seq(
         "org.eclipse.jetty" % "jetty-webapp" % jetty7version,
         "org.eclipse.jetty" % "jetty-plus" % jetty7version,
         "javax.servlet" % "servlet-api" % "2.5",
         "junit" % "junit" % "4.8" % "test"
      )
   )
}
