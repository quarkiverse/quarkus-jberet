# Quarkus JBeret Extension
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![Build](https://github.com/quarkiverse/quarkiverse-jberet/workflows/Build/badge.svg?branch=main)](https://github.com/quarkiverse/quarkiverse-jberet/actions?query=workflow%3ABuild)
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
  <version>1.1.0</version>
</dependency>
```

:information_source: **Recommended Quarkus version: `2.6.2.Final` or higher**

The Batch API and Runtime will be available out of the box. Please refer to the 
[Batch documentation](https://jcp.org/en/jsr/detail?id=352), or the 
[JBeret documentation](https://jberet.gitbooks.io/jberet-user-guide/content/) to learn about Batch Applications.  

## Configuration

The JBeret Quarkus extension supports the following configuration:

 | Name  | Type  | Default  |
 |---|---|---|
 | `quarkus.jberet.repository`<br>The repository type to store JBeret and Job data. A `jdbc` type requires a JDBC datasource. | `in-memory`, `jdbc`  | `in-memory` |
 | `quarkus.jberet.repository.jdbc.datasource`<br>The datasource name. | string  | `<default>` |
 | `quarkus.jberet.jobs.includes`<br>A list of patterns to match batch files to include.  | list of string  | |
 | `quarkus.jberet.jobs.excludes`<br>A list of patterns to match batch files to exclude. | list of string  | |
 | `quarkus.jberet.job."job-name".cron`<br>A cron style expression in Quartz format to schedule the job. | string  | |
 | `quarkus.jberet.job."job-name".params."param-key"`<br>A parameter to start a job. | string  | |
 
## Non-standard Features

### Simplified Configuration

The Batch API requires the `@BatchProperty` annotation to inject the specific configuration from the batch definition 
file. Instead, you can use the `@ConfigProperty` annotation, which is used to inject configuration properties in 
Quarkus using the MicroProfile Config API and keep consistency:

```java
@Inject
@BatchProperty(name = "job.config.name")
String batchConfig;

// These is equivalent to @BatchProperty injection
@ConfigProperty(name = "job.config.name")
Optional<String> mpConfig;
```

Although, there is a slight limitation: since job configuration is mostly dynamic and only injected on job execution, 
Quarkus may fail to start due to invalid configuration (can't find the Job configuration values). In this case, 
configuration injection points with the `@ConfigProperty` annotation need to set a default value or use an `Optional`.     

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

#### Additional Beans

Specific Quarkus implementation is available in `QuarkusJobOperator`, which can be also injected directly:

```java
@Inject
QuarkusJobOperator jobOperator;

void start() {
    Job job = new JobBuilder("programmatic")
            .step(new StepBuilder("programmaticStep")
                    .batchlet("programmaticBatchlet")
                    .build())
            .build();
    
    long executionId = jobOperator.start(job, new Properties());
    JobExecution jobExecution = jobOperator.getJobExecution(executionId);
}
```

With `QuarkusJobOperator` it is possible to define and start programmatic Jobs, with the 
[JBeret Programmatic Job Definition](https://jberet.gitbooks.io/jberet-user-guide/content/programmatic_job_definition_with_java/).

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
  <artifactId>quarkus-jberet-rest</artifactId>
  <version>0.0.4</version>
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

Example applications can be found inside the `integration-tests` folder:

* `chunk` - A simple Job that reads, processes, and stores data from a file.
* `jdbc-repository` - A Job that uses a `jdbc` datasource to store JBeret and Job metadata.
* `scheduler` - Schedule a Job to run every 10 seconds 

Or take a look into the [World of Warcraft Auctions - Batch Application](https://github.com/radcortez/wow-auctions). It 
downloads the World of Warcraft Auction House data and provides statistics about items prices. 

## Native Image Limitations

The Quakus JBeret Extension fully supports the Graal VM Native Image with the following exceptions:

* [Scripting Languages](https://jberet.gitbooks.io/jberet-user-guide/content/develop_batch_artifacts_in_script_languages/). 
While `Javascript` should work, it is unlikely that other scripting languages will be supported in 
[Graal](https://github.com/oracle/graaljs/blob/master/docs/user/ScriptEngine.md) via JSR-223. 

## Contributors ✨____

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
