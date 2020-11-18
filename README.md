# Quarkus JBeret Extension
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![Build](https://github.com/quarkiverse/quarkiverse-jberet/workflows/Build/badge.svg?branch=master)](https://github.com/quarkiverse/quarkiverse-jberet/actions?query=workflow%3ABuild)
[![License](https://img.shields.io/github/license/quarkiverse/quarkiverse-jberet.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Central](https://img.shields.io/maven-central/v/io.quarkiverse.jberet/quarkus-jberet-parent?color=green)](https://search.maven.org/search?q=a:quarkus-jberet-parent)
[![All Contributors](https://img.shields.io/badge/all_contributors-1-green.svg)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

The Quarkus JBeret Extension adds support for 
[JSR-352 Batch Applications for the Java Platform](https://jcp.org/en/jsr/detail?id=352). 
[JBeret](https://github.com/jberet) is an implementation of the JSR-352.

## Usage

To use the extension, add the dependency to the target project:

```xml
<dependency>
  <groupId>io.quarkiverse.jberet</groupId>
  <artifactId>quarkus-jberet</artifactId>
  <version>0.0.2</version>
</dependency>
```

The Batch API and Runtime will be available out of the box. Please refer to the 
[Batch documentation](https://jcp.org/en/jsr/detail?id=352), or the 
[JBeret documentation](https://jberet.gitbooks.io/jberet-user-guide/content/) to learn about Batch Applications.  

## Configuration

The JBeret Quarkus extension supports the following configuration:

 | Name  | Type  | Default  |
 |---|---|---|
 | `quarkus.jberet.repository`<br>The repository type to store JBeret and Job data. A `jdbc` type requires a JDBC datasource. | `in-memory`, `jdbc`  | `in-memory` |
 | `quarkus.jberet.jobs.includes`<br>A list of patterns to match batch files to include.  | list of string  | |
 | `quarkus.jberet.jobs.excludes`<br>A list of patterns to match batch files to exclude. | list of string  | |
 | `quarkus.jberet.job."job-name".cron`<br>A cron style expression in Quartz format to schedule the job. | string  | |
 | `quarkus.jberet.job."job-name".params."param-key"`<br>A parameter to start a scheduled job. | string  | |
 
## Non-standard Features

### Simplified Configuration

The Batch API requires the `@BatchProperty` annotation to inject the specific configuration from the batch definition 
file. Instead, you can use the `@ConfigProperty` annotation, which is used to inject configuration properties in 
Quarkus. 

Although, there is a slight limitation: since Quarkus validates configuration values at startup time, the 
Batch Job configuration may not be available yet, so injection points pointing to Batch properties need to set a default 
value or use an `Optional`.

### CDI Beans

The Batch APIs `JobOperator` and `JobRepository` are available as CDI beans, so they can be injected directly into any 
code:

```java
@Inject
JobOperator jobOperator;
@Inject
JobRepository jobRepository;

void start() {
    long executionId = jobOperator.start("batchlet", new Properties());
    JobExecution jobExecution = jobRepository.getJobExecution(executionId);
}
```

### Scheduler

The [JBeret Scheduler](https://github.com/jberet/jberet-schedule) is integrated out of the box in this extension. 

To schedule a Job execution, please refer to the `quarkus.jberet.job."job-name".cron` and  
`quarkus.jberet.job."job-name".params."param-key"` configurations.

A Job can also be scheduled programmatically, using the `JobScheduler` API and the Quarkus startup event:

```java
@ApplicationScoped
public class Scheduler {
    @Inject
    JobScheduler jobScheduler;

    void onStart(@Observes StartupEvent startupEvent) {
        final JobScheduleConfig scheduleConfig = JobScheduleConfigBuilder.newInstance()
                .jobName("scheduler")
                .initialDelay(0)
                .build();

        jobScheduler.schedule(scheduleConfig);
    }
}
```

The `JobScheduler` does not support persistent schedules. 

### REST API

The [JBeret REST](https://github.com/jberet/jberet-rest) is integrated as separate extension that can be easily added 
to the target project with the following dependency:

```xml
<dependency>
  <groupId>io.quarkiverse.jberet</groupId>
  <artifactId>quarkus-jberet</artifactId>
  <version>0.0.2</version>
</dependency>
```

The [JBeret REST](https://github.com/jberet/jberet-rest) API, provides REST resources to several operations around the 
Batch API: starting and stopping jobs, querying the status of a job, schedule a job, and many more. The extension 
includes a REST client to simplify the REST API calls:

```java
@Inject
BatchClient batchClient;

void start() throws Exception {
    JobExecutionEntity jobExecutionEntity = batchClient.startJob("batchlet", new Properties());
}
```
 
## Example Applications

Example applications can be found inside the integration-test folder:

* `chunk` - A simple Job that reads, processes, and stores data from a file.
* `jdbc-repository` - A Job that uses a `jdbc` datasource to store JBeret and Job metadata.
* `scheduler` - Schedule a Job to run every 10 seconds 

Or take a look into the [World of Warcraft Auctions - Batch Application](https://github.com/radcortez/wow-auctions). It 
downloads the World of Warcraft Auction House data and provides statistics about items prices.

## Native Image Limitations

The Quakus JBeret Extension fully supports the Graal Native Image. Except for:

* A combination of JVM based runners and Native executables when using the `jdbc` Repository type connected to the same 
datasource. This will corrupt the Batch and Job metadata. This is because the Graal Native Image does not support Java 
Serialization (used by JBeret to store metadata). The native image substitutes the Java Serializer with a JSON 
Serializer. Work is underway to provide JBeret with a way to select the serialization mechanism and remove this limitation.  

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="http://www.radcortez.com"><img src="https://avatars1.githubusercontent.com/u/5796305?v=4" width="100px;" alt=""/><br /><sub><b>Roberto Cortez</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkiverse-jberet/commits?author=radcortez" title="Code">💻</a> <a href="#maintenance-radcortez" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-enable -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
