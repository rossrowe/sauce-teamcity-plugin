package com.saucelabs.teamcity;

/**
 * @author Ross Rowe
 */
public class Constants {

    public static final String SELENIUM_DRIVER_ENV = "SELENIUM_DRIVER";
    public static final String SELENIUM_HOST_ENV = "SELENIUM_HOST";
    public static final String SELENIUM_PORT_ENV = "SELENIUM_PORT";
    public static final String SELENIUM_BROWSER_ENV = "SELENIUM_BROWSER";
    public static final String SELENIUM_PLATFORM_ENV = "SELENIUM_PLATFORM";
    public static final String SELENIUM_VERSION_ENV = "SELENIUM_VERSION";
    public static final String SAUCE_ONDEMAND_HOST = "SAUCE_HOST";
    public static final String SELENIUM_STARTING_URL_ENV = "SELENIUM_STARTING_URL";
    public static final String SAUCE_CUSTOM_DATA_ENV = "SAUCE_BAMBOO_BUILDNUMBER";
    public static final String BAMBOO_BUILD_NUMBER_ENV = "BAMBOO_BUILDNUMBER";
    public static final String SAUCE_USER_NAME = "SAUCE_USER_NAME";
    public static final String SAUCE_API_KEY = "SAUCE_API_KEY";
    public static final String SAUCE_BROWSERS = "SAUCE_ONDEMAND_BROWSERS";
    public static final String SELENIUM_MAX_DURATION_ENV = "SELENIUM_MAX_DURATION";
    public static final String SELENIUM_IDLE_TIMEOUT_ENV = "SELENIUM_IDLE_TIMEOUT";
    public static final String SAUCE_USER_ID_KEY = "saucePlugin.userId";
    public static final String SAUCE_PLUGIN_ACCESS_KEY = "saucePlugin.accessKey";
    public static final String SELENIUM_HOST_KEY = "saucePlugin.seleniumHost";
    public static final String SELENIUM_PORT_KEY = "saucePlugin.seleniumPort";
    public static final String SELENIUM_STARTING_URL_KEY = "saucePlugin.startingURL";
    public static final String SELENIUM_MAX_DURATION_KEY = "saucePlugin.maxDuration";
    public static final String SELENIUM_IDLE_TIMEOUT_KEY = "saucePlugin.idleTimeout";
    public static final String SELENIUM_SELECTED_BROWSER = "saucePlugin.webDriverBrowsers";
    public static final String SAUCE_CONNECT_OPTIONS = "saucePlugin.sauceConnectOptions";
    public static final String SAUCE_HTTPS_PROTOCOL = "saucePlugin.httpsProtocol";
    public static final String SAUCE_CONNECT_KEY = "saucePlugin.sauceConnect";


    private Constants() {
    }
}
