resolvers in ThisBuild += "lightbend-commercial-mvn" at
  "https://repo.lightbend.com/pass/ufew-6w02eOIlT5mB_0iWwUmKB1er_U-PTaAzzsAVUIk3w-_/commercial-releases"
resolvers in ThisBuild += Resolver.url("lightbend-commercial-ivy",
  url("https://repo.lightbend.com/pass/ufew-6w02eOIlT5mB_0iWwUmKB1er_U-PTaAzzsAVUIk3w-_/commercial-releases"))(Resolver.ivyStylePatterns)