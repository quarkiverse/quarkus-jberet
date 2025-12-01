# Quarkus JBeret Extension

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->

[![Build](https://github.com/quarkiverse/quarkus-jberet/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/quarkiverse/quarkus-jberet/actions/workflows/build.yml)
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
  <version>2.8.0</version>
</dependency>
```

:information_source: **Recommended Quarkus version: `3.28.1` or higher**

The Batch API and Runtime will be available out of the box. Please refer to the
[Batch documentation](https://jcp.org/en/jsr/detail?id=352), or the
[JBeret documentation](https://jberet.gitbooks.io/jberet-user-guide/content/) to learn about Batch Applications.

Also, the [Quarkus JBeret Extension](https://docs.quarkiverse.io/quarkus-jberet/dev/index.html) adds features on top 
of JBeret to improve the developer experience. Please check the extension 
[documentation](https://docs.quarkiverse.io/quarkus-jberet/dev/index.html). 

## Example Applications

Example applications can be found inside the `integration-tests` folder:

- `chunk` - A simple Job that reads, processes, and stores data from a file.
- `jdbc-repository` - A Job that uses a `jdbc` datasource to store JBeret and Job metadata.
- `scheduler` - Schedule a Job to run every 10 seconds

Or take a look into the [World of Warcraft Auctions - Batch Application](https://github.com/radcortez/wow-auctions). It
downloads the World of Warcraft Auction House data and provides statistics about items prices.

## Contributors ✨\_\_\_\_

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
