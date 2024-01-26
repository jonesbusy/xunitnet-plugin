# xUnit .NET Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/xunitnet-plugin/job/main/badge/icon)](https://ci.jenkins.io/job/Plugins/job/xunitnet-plugin/job/main/)
[![Coverage](https://ci.jenkins.io/job/Plugins/job/xunitnet-plugin/job/main/badge/icon?status=${instructionCoverage}&subject=coverage&color=${colorInstructionCoverage})](https://ci.jenkins.io/job/Plugins/job/xunitnet-plugin/job/main)
[![LOC](https://ci.jenkins.io/job/Plugins/job/xunitnet-plugin/job/main/badge/icon?job=test&status=${lineOfCode}&subject=line%20of%20code&color=blue)](https://ci.jenkins.io/job/Plugins/job/xunitnet-plugin/job/main)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/xunitnet-plugin.svg)](https://github.com/jenkinsci/xunitnet-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/xunitnet.svg)](https://plugins.jenkins.io/xunitnet)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/xunitnet.svg?color=blue)](https://plugins.jenkins.io/xunitnet)

![](docs/images/xunit.png)

## Introduction

This plugin allows you to publish [XUnit .NET](https://xunit.net/) test results.

## Pipeline example

For more information refer to [XUnit Pipeline Steps](https://www.jenkins.io/doc/pipeline/steps/xunitnet/)

### For Scripted pipeline

```groovy
node {
    ...
    stage("Publish XUnit .NET Test Report") {
        xunitnet testResultsPattern: 'xunit-results.xml'
    }
    ...
}
```

### For Declarative pipeline

```groovy
pipeline {
    agent any
    ...
    stages {
        ...
        stage("Publish XUnit .NET Test Report") {
            steps {
                xunitnet testResultsPattern: 'xunit-results.xml'
            }
        }
        ...
    }
}
```

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
