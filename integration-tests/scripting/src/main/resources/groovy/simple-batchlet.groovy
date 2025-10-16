package groovy

def stop() {
    println('In stop function');
}

//access built-in variables: jobContext, stepContext and batchProperties,
//set job exit status to the value of testName property, and
//return the value of testName property as step exit status,
//
def process() {
    println('jobName: ' + jobContext.getJobName());
    println('stepName: ' + stepContext.getStepName());
    testName = batchProperties.get('testName');
    jobContext.setExitStatus(testName);
    return testName;
}
