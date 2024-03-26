## :link: Ligoj API plugin [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.ligoj.api/root/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.ligoj.api/root)
API framework for Ligoj plugins

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.ligoj.api%3Aroot&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=org.ligoj.api%3Aroot)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?metric=alert_status&project=org.ligoj.api%3Aroot)](https://sonarcloud.io/dashboard/index/org.ligoj.api:root)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abf810c094e44c0691f71174c707d6ed)](https://www.codacy.com/gh/ligoj/ligoj-api?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ligoj/ligoj-api&amp;utm_campaign=Badge_Grade)
[![CodeFactor](https://www.codefactor.io/repository/github/ligoj/ligoj-api/badge)](https://www.codefactor.io/repository/github/ligoj/ligoj-api)
[![Maintainability](https://api.codeclimate.com/v1/badges/df4f5f5fc210a3e77b1e/maintainability)](https://codeclimate.com/github/ligoj/ligoj-api/maintainability)
[![License](http://img.shields.io/:license-mit-blue.svg)](http://fabdouglas.mit-license.org/)

# Extension points

## Plugin definition extension points: 
- [org.ligoj.app.api.ServicePlugin](plugin-api/src/main/java/org/ligoj/app/api/ServicePlugin.java)
- [org.ligoj.app.api.ToolPlugin](plugin-api/src/main/java/org/ligoj/app/api/ToolPlugin.java)

## IAM extension points
- [org.ligoj.app.iam.IamProvider](plugin-api/src/main/java/org/ligoj/app/iam/IamProvider.java): Identity and Access Management (IAM) provider of the application.
- [org.ligoj.app.iam.IamConfigurationProvider](plugin-api/src/main/java/org/ligoj/app/iam/IamConfigurationProvider.java)
- [org.ligoj.app.iam.IAuthenticationContributor](plugin-api/src/main/java/org/ligoj/app/iam/IAuthenticationContributor.java)
- [org.ligoj.app.iam.ICompanyRepository](plugin-api/src/main/java/org/ligoj/app/iam/ICompanyRepository.java)
- [org.ligoj.app.iam.IContainerRepository](plugin-api/src/main/java/org/ligoj/app/iam/IContainerRepository.java)
- [org.ligoj.app.iam.IGroupRepository](plugin-api/src/main/java/org/ligoj/app/iam/IGroupRepository.java)
- [org.ligoj.app.iam.IUserRepository](plugin-api/src/main/java/org/ligoj/app/iam/IUserRepository.java)

# Maven structure

Minimal Maven structure for a plugin:
- Version, following the [semver](https://semver.org/) convention
- Plugin artifact id
  - Must start with the parent service plugin artifact id. For sample, plugin `plugin-id-ldap` is a tool plugin for the service `plugin-id`.
  - Otherwise, Must start with `plugin-`, without additional hyphen.
- Parent service plugin artifact as `provided` dependency.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.ligoj.api</groupId>
		<artifactId>plugin-parent</artifactId>
        <version>4.1.7</version> <!-- Version of plugin API -->
		<relativePath />
	</parent>

	<groupId>org.ligoj.plugin</groupId>
	<artifactId>plugin-id-ldap</artifactId> <!-- Tool plugin artifact-id, must start with "plugin-" -->
	<version>1.1.3-SNAPSHOT</version>       <!-- Tool plugin version -->
	<packaging>jar</packaging>

    <!-- Feature dependency -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.ligoj.plugin</groupId>
                <artifactId>plugin-id</artifactId>        <!-- Service plugin artifact-id -->
                <version>[2.2.0-SNAPSHOT,2.3.0)</version> <!-- Service plugin version range -->
                <scope>provided</scope>                   <!-- Always provided -->
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

# Build a plugin

Produced artifacts for a plugin named `plugin-id-ldap` are:
- Main jar file: `plugin-id-ldap-1.0.0.jar`
- Javadoc jar file: `plugin-id-ldap-1.0.0-javadoc.jar`. Optional, but when deployed, contributes to generated OpenAPI JSON file.
- Sources jar file: `plugin-id-ldap-1.0.0-sources.jar`. Optional.
- Test sources jar file: `plugin-id-ldap-1.0.0-test-sources.jar`. Optional.
- Jacoco coverage result

The following command generate all artifacts and run UTs and Its
```bash
mvn package -Pjavadoc,jacoco,sources
```

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
