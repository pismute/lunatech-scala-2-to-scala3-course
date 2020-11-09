

Global / onChangedBuildSource := ReloadOnSourceChanges


Global / onLoad := {
  (Global / onLoad).value andThen (state => "project exercises" :: state)
}

lazy val `scala-2-to-scala-3` = (project in file("."))
  .aggregate(
    common,
    `exercises`
  )
  .settings(ThisBuild / scalaVersion := Version.scalaVersion)
  .settings(CommonSettings.commonSettings: _*)

lazy val common = project
  .settings(CommonSettings.commonSettings: _*)

lazy val `exercises` = project
  .configure(CommonSettings.configure)
  .dependsOn(common % "test->test;compile->compile")
           