resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")

addSbtPlugin("org.playframework"   %  "sbt-plugin"            % "3.0.7")
addSbtPlugin("io.github.irundaia"  %  "sbt-sassify"           % "1.5.2")
addSbtPlugin("org.scalameta"       %  "sbt-scalafmt"          % "2.4.0")
addSbtPlugin("org.scoverage"       %  "sbt-scoverage"         % "2.3.1")
addSbtPlugin("uk.gov.hmrc"         %  "sbt-auto-build"        % "3.24.0")
addSbtPlugin("uk.gov.hmrc"         %  "sbt-distributables"    % "2.6.0")
addSbtPlugin("com.github.sbt"      %  "sbt-concat"            % "1.0.0")
addSbtPlugin("com.github.sbt"      %  "sbt-uglify"            % "3.0.0")
addSbtPlugin("com.github.sbt"      %  "sbt-digest"            % "2.0.0")
addSbtPlugin("org.wartremover"     %  "sbt-wartremover"       % "3.2.7")
addSbtPlugin("com.timushev.sbt"    %  "sbt-updates"           % "0.6.4")

addDependencyTreePlugin
