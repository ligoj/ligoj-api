## :link: Ligoj API plugin ![Maven Central](https://img.shields.io/maven-central/v/org.ligoj.api/root)
API framework for Ligoj plugins

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.ligoj.api%3Aroot&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=org.ligoj.api%3Aroot)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?metric=alert_status&project=org.ligoj.api%3Aroot)](https://sonarcloud.io/dashboard/index/org.ligoj.api:root)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abf810c094e44c0691f71174c707d6ed)](https://www.codacy.com/gh/ligoj/ligoj-api?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ligoj/ligoj-api&amp;utm_campaign=Badge_Grade)
[![CodeFactor](https://www.codefactor.io/repository/github/ligoj/ligoj-api/badge)](https://www.codefactor.io/repository/github/ligoj/ligoj-api)
[![License](https://img.shields.io/:license-mit-blue.svg)](LICENSE)

Requirements: Java 25, Maven 3.9.16+. Built on [Ligoj Bootstrap](https://github.com/ligoj/bootstrap) (`org.ligoj.bootstrap:bootstrap-business-parent`).

# Modules

- **parent**: parent POM of the modules of this repository, built on the bootstrap business parent.
- **plugin-api**: the extension points, following semver: plugin definitions and IAM interfaces.
- **plugin-core**: the shared model, repositories and REST resources (nodes, subscriptions, parameters, projects, delegations, tasks, events) and the base classes of the plugins.
- **plugin-api-test**: the test support of the plugins: Spring test context, CSV fixtures and base test classes.
- **plugin-iam-empty**: an empty, read-only IAM provider, for tests.
- **plugin-parent**: the parent POM of every plugin.

# Extension points

## Plugin definition extension points

- `org.ligoj.bootstrap.core.plugin.FeaturePlugin` (bootstrap): the base of all plugins: key (`feature:iam:empty`, `service:id:ldap`, ...), `install`, `update` and the entities to install from `csv/` files.
- [org.ligoj.app.api.ServicePlugin](plugin-api/src/main/java/org/ligoj/app/api/ServicePlugin.java): a service (first level node, such as `service:id`) or a tool (second level, such as `service:id:ldap`) handling subscriptions.
- [org.ligoj.app.api.ToolPlugin](plugin-api/src/main/java/org/ligoj/app/api/ToolPlugin.java): a tool with versions and status checks of its node instances and subscriptions.
- [org.ligoj.app.api.ConfigurablePlugin](plugin-api/src/main/java/org/ligoj/app/api/ConfigurablePlugin.java): a plugin exposing a configuration per subscription.
- [org.ligoj.app.resource.ActivitiesProvider](plugin-core/src/main/java/org/ligoj/app/resource/ActivitiesProvider.java): the activities of users on a subscription, such as their last activity on the tool.

The base classes plugins usually extend, in plugin-core:
- [AbstractServicePlugin](plugin-core/src/main/java/org/ligoj/app/resource/plugin/AbstractServicePlugin.java) and [AbstractToolPluginResource](plugin-core/src/main/java/org/ligoj/app/resource/plugin/AbstractToolPluginResource.java). The latter rejects the `CREATE` subscription mode by default.
- [AbstractConfiguredServicePlugin](plugin-core/src/main/java/org/ligoj/app/resource/plugin/AbstractConfiguredServicePlugin.java): configuration entities attached to a subscription. Use `findConfigured` to read them and `findConfiguredManaged` before any change: the latter also requires the subscriptions of the project to be managed by the user.
- [LongTaskRunnerNode](plugin-core/src/main/java/org/ligoj/app/resource/node/LongTaskRunnerNode.java) and [LongTaskRunnerSubscription](plugin-core/src/main/java/org/ligoj/app/resource/subscription/LongTaskRunnerSubscription.java): long running tasks, one at a time per node or subscription.
- [ServicePluginLocator](plugin-core/src/main/java/org/ligoj/app/resource/ServicePluginLocator.java): the plugin resource of a node.
- [LigojPluginsClassLoader](plugin-core/src/main/java/org/ligoj/app/resource/plugin/LigojPluginsClassLoader.java): `toPath(node)`/`toPath(subscription, ...)`, the file storage of a node or subscription.

## IAM extension points

- [org.ligoj.app.iam.IamProvider](plugin-api/src/main/java/org/ligoj/app/iam/IamProvider.java): Identity and Access Management (IAM) provider of the application.
- [org.ligoj.app.iam.IamConfigurationProvider](plugin-api/src/main/java/org/ligoj/app/iam/IamConfigurationProvider.java)
- [org.ligoj.app.iam.IAuthenticationContributor](plugin-api/src/main/java/org/ligoj/app/iam/IAuthenticationContributor.java)
- [org.ligoj.app.iam.ICompanyRepository](plugin-api/src/main/java/org/ligoj/app/iam/ICompanyRepository.java)
- [org.ligoj.app.iam.IContainerRepository](plugin-api/src/main/java/org/ligoj/app/iam/IContainerRepository.java)
- [org.ligoj.app.iam.IGroupRepository](plugin-api/src/main/java/org/ligoj/app/iam/IGroupRepository.java)
- [org.ligoj.app.iam.IUserRepository](plugin-api/src/main/java/org/ligoj/app/iam/IUserRepository.java)
- [org.ligoj.app.iam.IPasswordGenerator](plugin-api/src/main/java/org/ligoj/app/iam/IPasswordGenerator.java)

# Maven structure

Minimal Maven structure for a plugin:
- Version, following the [semver](https://semver.org/) convention
- Plugin artifact id, derived from the plugin key without its first part: the key `service:id:ldap` gives `plugin-id-ldap`, the key `feature:iam:empty` gives `plugin-iam-empty`
  - A tool plugin's artifact id starts with the artifact id of its service plugin. For sample, plugin `plugin-id-ldap` is a tool plugin for the service `plugin-id`.
  - Otherwise, it is `plugin-` followed by a single name, without additional hyphen.
- Parent service plugin artifact as `provided` dependency.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.ligoj.api</groupId>
		<artifactId>plugin-parent</artifactId>
		<version>5.0.3</version> <!-- Version of plugin API -->
		<relativePath />
	</parent>

	<groupId>org.ligoj.plugin</groupId>
	<artifactId>plugin-id-ldap</artifactId> <!-- Tool plugin artifact-id, must start with "plugin-" -->
	<version>1.1.3-SNAPSHOT</version>       <!-- Tool plugin version -->
	<packaging>jar</packaging>

	<!-- Feature dependency -->
	<dependencies>
		<dependency>
			<groupId>org.ligoj.plugin</groupId>
			<artifactId>plugin-id</artifactId>        <!-- Service plugin artifact-id -->
			<version>[2.2.0-SNAPSHOT,2.3.0)</version> <!-- Service plugin version range -->
			<scope>provided</scope>                   <!-- Always provided -->
		</dependency>
	</dependencies>
</project>
```

# Build a plugin

Produced artifacts for a plugin named `plugin-id-ldap` are:
- Main jar file: `plugin-id-ldap-1.0.0.jar`
- Javadoc jar file: `plugin-id-ldap-1.0.0-javadoc.jar`. Optional, but when deployed, contributes to generated OpenAPI JSON file.
- Sources jar file: `plugin-id-ldap-1.0.0-sources.jar`. Optional.
- Test sources jar file: `plugin-id-ldap-1.0.0-test-sources.jar`. Optional.
- Jacoco coverage result

The following command generates all artifacts and runs the unit and integration tests (integration tests are skipped without the `it` profile):
```bash
mvn verify -Pjavadoc,jacoco,sources,it
```

`plugin-parent` also provides these auto-activated profiles:
- `code-sign`: signs the plugin jar with `jarsigner`, when `~/.ligoj/code-signing.p12` exists. The keystore password comes from the `LIGOJ_SIGN_STOREPASS` environment variable or the `ligoj.sign.storepass` property; `-Djarsigner.skip=true` skips the signature. The signature is verified at startup by the application against the `ligoj.plugin.signature.truststore` truststore, see the comments of [plugin-parent/pom.xml](plugin-parent/pom.xml) to create the keystore and the truststore.
- `ui-build`: builds the Vue.js front-end of the plugin when `ui/package.json` exists, with a Node version downloaded under `target/` (`ui.node.version`); `-Dskip.ui.build=true` skips it.

# Build this repository

```bash
# Build and run the unit tests, install the modules locally
mvn clean install

# With the integration tests and the coverage
mvn clean verify -Pit,jacoco
```

Test failures do not fail the build (`testFailureIgnore` from the parent POMs): check the `Tests run: … Failures: … Errors: …` lines.

# Install a plugin

## From the UI

The common steps:
- Login to application
- Go to the `Administration` page
- Choose the `Plugin` section


### Install a local plugin

The specific steps:
- Click on `Install > Install from file`
- In the modal, fill the inputs accordingly to your plugin
- Upload it
- Restart the application

### Install a deployed Maven plugin

The specific steps:
- Click on `Install > Install from repository`
- In the modal, type the artifact name
- Choose one or many plugins
- Confirm
- Restart the application


## From the Ligoj CLI

The [Ligoj CLI](https://github.com/ligoj/cli) is an administration tool for all Ligoj API operations.

See all command options with `ligoj plugin`

### Install a local plugin

The command is:

```bash
ligoj plugin upload --id "plugin-id-ldap" --version "1.1.4" --from "/path/to/plugin-id-ldap-1.1.4.jar"
```

### Install a deployed Maven plugin

The command is:

```bash
ligoj plugin install --id "plugin-id-ldap" --version "1.1.4"  --repository "central" --javadoc
```
