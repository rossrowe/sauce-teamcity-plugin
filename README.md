Sauce Labs TeamCity Plugin
=====================

This plugin allows you to integrate Sauce Labs with TeamCity. Specifically, you can:

*    Specify the browsers versions and operating systems you want your tests to run against
*    Automate the setup and tear down of Sauce Connect, which enables you to run your Selenium tests against local websites using Sauce Labs
*    Integrate the Sauce results videos within the TeamCity build output


Installation
====

[Download](https://repository-saucelabs.forge.cloudbees.com/release/com/saucelabs/teamcity/sauceplugin/1.0/sauceplugin-1.0.zip) the plugin zip file and copy it into your ~/.BuildServer/plugins directory


Usage
===

The plugin provides a 'Sauce Labs Build Feature' which can be added a TeamCity build.

Once the build feature has been selected, enter your Sauce Labs username and access key, and specify whether you want Sauce Connect to be launched as part of your build.  You can also select the browsers you wish to be used by your tests.

In order to integrate the Sauce tests with the TeamCity build, you will need to include the following output as part of the running of each test:

    SauceOnDemandSessionID=SESSION_ID job-name=JOB_NAME

where SESSION_ID is the job session id, and job-name is the name of your job.