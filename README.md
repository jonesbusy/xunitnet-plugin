# xUnit .NET Plugin

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
