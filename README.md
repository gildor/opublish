# Opublish - Opionated publish plugin for Gradle

This plugin created to significantly simplify Java and Android (not ready yet!)
library publishing. To achieve that plugin provides reasonable defaults for Maven Publishing plugin and Bintray plugin.

This plugin doesn't provide flexibility that you can achieve configuring publishing manually,
but tries to be good enough for most of standard use cases.

Before start to use it please check section [Known config limitations](Known config limitations)

## Default behaviour

- Publish sources (can be disabled)
- Publish javadoc (can be disabled)

## How to configure
Use top-level project properties to configure group, version and description:
```
group = "my.library"
version = "1.2.3"
description = "My Library"
```

Publish credentials:
sonatype.user
sonatype.password

bintray.user
bintray.key
bintray.repo //default is maven

//TODO: Artifactory config 

## Known config limitations

- You cannot use custom javadoc task
- You cannot use custom sources task