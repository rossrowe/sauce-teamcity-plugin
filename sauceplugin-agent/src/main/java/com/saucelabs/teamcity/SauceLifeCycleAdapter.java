package com.saucelabs.teamcity;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Handles populating the environment variables and starting and stopping Sauce Connect when a TeamCity build is started/stopped.
 *
 * @author Ross Rowe
 */
public class SauceLifeCycleAdapter extends AgentLifeCycleAdapter {

    /**
     * Singleton Sauce Connect v4 manager instance, populated by Spring.
     */
    private final SauceConnectFourManager sauceFourTunnelManager;
    /**
     * Singleton instance used to retrieve browser information supported by Sauce, populated by Spring.
     */
    private BrowserFactory sauceBrowserFactory;
    /**
     * Singleton Sauce Connect v3 manager instance, populated by Spring.
     */
    private SauceConnectTwoManager sauceTunnelManager;

    /**
     * @param agentDispatcher
     * @param sauceBrowserFactory    Singleton instance used to retrieve browser information supported by Sauce, populated by Spring.
     * @param sauceTunnelManager     Singleton Sauce Connect v3 manager instance, populated by Spring.
     * @param sauceFourTunnelManager Singleton Sauce Connect v4 manager instance, populated by Spring.
     */
    public SauceLifeCycleAdapter(
            @NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher,
            BrowserFactory sauceBrowserFactory,
            SauceConnectTwoManager sauceTunnelManager,
            SauceConnectFourManager sauceFourTunnelManager) {
        agentDispatcher.addListener(this);
        this.sauceBrowserFactory = sauceBrowserFactory;
        this.sauceFourTunnelManager = sauceFourTunnelManager;
        this.sauceTunnelManager = sauceTunnelManager;
    }

    /**
     * If Sauce Connect is enabled, then close the Sauce Connect process.
     *
     * @param build       the current build
     * @param buildStatus state of the build
     */
    @Override
    public void beforeBuildFinish(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
        super.beforeBuildFinish(build, buildStatus);

        Collection<AgentBuildFeature> features = build.getBuildFeaturesOfType("sauce");
        if (features.isEmpty()) return;
        for (AgentBuildFeature feature : features) {
            Loggers.AGENT.info("Closing Sauce Connect");
            if (shouldStartSauceConnect(feature)) {
                String options = getSauceConnectOptions(build, feature);
                if (shouldStartSauceConnectThree(feature)) {
                    sauceTunnelManager.closeTunnelsForPlan(getUsername(feature), options, null);
                } else {
                    sauceFourTunnelManager.closeTunnelsForPlan(getUsername(feature), options, null);
                }
            }
        }
    }

    /**
     * @param feature contains the Sauce information set by the user within the build configuration
     * @return boolean indicating whether Sauce Connect v3 should be started
     */
    private boolean shouldStartSauceConnectThree(AgentBuildFeature feature) {
        String useSauceConnect = feature.getParameters().get(Constants.USE_SAUCE_CONNECT_3);
        return useSauceConnect != null && useSauceConnect.equals("true");
    }

    /**
     * If the build has the Sauce build feature enabled, populates the environment variables and starts Sauce Connect.
     *
     * @param runningBuild the current running build
     */
    @Override
    public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
        super.buildStarted(runningBuild);
        Loggers.AGENT.info("Build Started, setting Sauce environment variables");
        Collection<AgentBuildFeature> features = runningBuild.getBuildFeaturesOfType("sauce");
        if (features.isEmpty()) return;
        for (AgentBuildFeature feature : features) {
            populateEnvironmentVariables(runningBuild, feature);
            if (shouldStartSauceConnect(feature)) {
                startSauceConnect(runningBuild, feature);
            }
        }
    }

    /**
     * Starts Sauce Connect.
     *
     * @param runningBuild
     * @param feature contains the Sauce information set by the user within the build configuration
     */
    private void startSauceConnect(AgentRunningBuild runningBuild, AgentBuildFeature feature) {
        try {
            Loggers.AGENT.info("Starting Sauce Connect");
            String options = getSauceConnectOptions(runningBuild, feature);
            if (shouldStartSauceConnectThree(feature)) {
                sauceTunnelManager.openConnection(
                        getUsername(feature),
                        getAccessKey(feature),
                        Integer.parseInt(getSeleniumPort(feature)),
                        null,
                        options,
                        feature.getParameters().get(Constants.SAUCE_HTTPS_PROTOCOL),
                        null,
                        Boolean.TRUE, null);
            } else {
                sauceFourTunnelManager.openConnection(
                        getUsername(feature),
                        getAccessKey(feature),
                        Integer.parseInt(getSeleniumPort(feature)),
                        null,
                        options,
                        feature.getParameters().get(Constants.SAUCE_HTTPS_PROTOCOL),
                        null,
                        Boolean.TRUE, null);
            }
        } catch (IOException e) {
            Loggers.AGENT.error("Error launching Sauce Connect", e);
            //TODO log error to build log
        }
    }

    private String getSauceConnectOptions(AgentRunningBuild runningBuild, AgentBuildFeature feature) {
        String options = feature.getParameters().get(Constants.SAUCE_CONNECT_OPTIONS);
        if (options == null || options.equals(""))
        {
            //default tunnel identifier to teamcity-%teamcity.agent.name%
            options = "-i teamcity-" + runningBuild.getSharedConfigParameters().get("teamcity.agent.name");
        }
        return options;
    }

    /**
     * @param feature contains the Sauce information set by the user within the build configuration
     * @return
     */
    private String getSeleniumHost(AgentBuildFeature feature) {
        String host = feature.getParameters().get(Constants.SELENIUM_HOST_KEY);
        if (host == null || host.equals("")) {
            if (shouldStartSauceConnect(feature)) {
                host = "localhost";
            } else {
                host = "ondemand.saucelabs.com";
            }
        }
        return host;
    }

    /**
     * @param feature contains the Sauce information set by the user within the build configuration
     * @return
     */
    private String getSeleniumPort(AgentBuildFeature feature) {
        String port = feature.getParameters().get(Constants.SELENIUM_PORT_KEY);
        if (port == null || port.equals("")) {
            if (shouldStartSauceConnect(feature)) {
                port = "4445";
            } else {
                port = "80";
            }

        }
        return port;

    }

    /**
     * @param feature contains the Sauce information set by the user within the build configuration
     * @return
     */
    private boolean shouldStartSauceConnect(AgentBuildFeature feature) {
        String useSauceConnect = feature.getParameters().get(Constants.SAUCE_CONNECT_KEY);
        return useSauceConnect != null && useSauceConnect.equals("true");
    }

    /**
     * @param runningBuild
     * @param feature      contains the Sauce information set by the user within the build configuration
     */
    private void populateEnvironmentVariables(AgentRunningBuild runningBuild, AgentBuildFeature feature) {

        if (getProxyHost(feature) != null) {
            setupProxy(feature);
            //recreate browser factory to use proxy settings
            sauceBrowserFactory = new BrowserFactory();
        }

        Loggers.AGENT.info("Populating environment variables");
        String userName = getUsername(feature);
        String apiKey = getAccessKey(feature);

        String[] selectedBrowsers = getSelectedBrowsers(feature);
        if (selectedBrowsers.length == 0) {
            Loggers.AGENT.error("No selected browsers found");
        } else {
            Loggers.AGENT.info("Selected browsers: " + Arrays.toString(selectedBrowsers));
            if (selectedBrowsers.length == 1) {
                Browser browser = sauceBrowserFactory.webDriverBrowserForKey(selectedBrowsers[0]);
                if (browser == null) {
                    Loggers.AGENT.info("No browser found for: " + selectedBrowsers[0]);
                    Loggers.AGENT.info("Browsers : " + sauceBrowserFactory);
                } else {
                    String sodDriverURI = getSodDriverUri(userName, apiKey, browser, feature);
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_BROWSER_ENV, browser.getBrowserName());
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_VERSION_ENV, browser.getVersion());
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_PLATFORM_ENV, browser.getOs());
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_DRIVER_ENV, sodDriverURI);
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_ORIENTATION, browser.getDeviceOrientation());
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_DEVICE, browser.getDevice());
                    addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_DEVICE_TYPE, browser.getDeviceType());
                }
            } else {
                JSONArray browsersJSON = new JSONArray();
                for (String browser : selectedBrowsers) {
                    Browser browserInstance = sauceBrowserFactory.webDriverBrowserForKey(browser);
                    if (browserInstance != null) {
                        browserAsJSON(userName, apiKey, browsersJSON, browserInstance);
                    }
                }
                addSharedEnvironmentVariable(runningBuild, Constants.SAUCE_BROWSERS_ENV, browsersJSON.toString());
            }
        }
        addSharedEnvironmentVariable(runningBuild, Constants.SAUCE_USER_NAME, userName);
        addSharedEnvironmentVariable(runningBuild, Constants.SAUCE_API_KEY, apiKey);
        //backwards compatibility with environment variables expected by Sausage
        addSharedEnvironmentVariable(runningBuild, Constants.SAUCE_USERNAME, userName);
        addSharedEnvironmentVariable(runningBuild, Constants.SAUCE_ACCESS_KEY, apiKey);

        addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_HOST_ENV, getSeleniumHost(feature));
        addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_PORT_ENV, getSeleniumPort(feature));
        addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_STARTING_URL_ENV, feature.getParameters().get(Constants.SELENIUM_STARTING_URL_KEY));
        addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_MAX_DURATION_ENV, feature.getParameters().get(Constants.SELENIUM_MAX_DURATION_KEY));
        addSharedEnvironmentVariable(runningBuild, Constants.SELENIUM_IDLE_TIMEOUT_ENV, feature.getParameters().get(Constants.SELENIUM_IDLE_TIMEOUT_KEY));

    }

    private void setupProxy(AgentBuildFeature feature) {
        String proxyHost = getProxyHost(feature);
        if (StringUtils.isNotBlank(proxyHost)) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("https.proxyHost", proxyHost);
        }
        String proxyPort = getProxyPort(feature);
        if (StringUtils.isNotBlank(proxyPort)) {
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("https.proxyPort", proxyPort);
        }
        String userName = getProxyUser(feature);
        String password = getProxyPassword(feature);
        if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
            System.setProperty("http.proxyUser", userName);
            System.setProperty("https.proxyUser", userName);
            System.setProperty("http.proxyPassword", password);
            System.setProperty("https.proxyPassword", password);
        }
    }

    private String getProxyHost(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.PROXY_HOST);
    }

    private String getProxyPort(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.PROXY_PORT);
    }

    private String getProxyUser(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.PROXY_USER);
    }

    private String getProxyPassword(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.PROXY_PASSWORD);
    }

    /**
     * @param userName
     * @param apiKey
     * @param browsersJSON
     * @param browserInstance
     */
    private void browserAsJSON(String userName, String apiKey, JSONArray browsersJSON, Browser browserInstance) {
        if (browserInstance == null) {
            return;
        }
        JSONObject config = new JSONObject();
        try {
            config.put("os", browserInstance.getOs());
            config.put("browser", browserInstance.getBrowserName());
            config.put("browser-version", browserInstance.getVersion());
            config.put("long-name", browserInstance.getLongName());
            config.put("long-version", browserInstance.getLongVersion());
            config.put("url", browserInstance.getUri(userName, apiKey));
            if (browserInstance.getDevice() != null) {
                config.put("device", browserInstance.getDevice());
            }
            if (browserInstance.getDeviceType() != null) {
                config.put("device-type", browserInstance.getDeviceType());
            }
            if (browserInstance.getDeviceOrientation() != null) {
                config.put("device-orientation", browserInstance.getDeviceOrientation());
            }

        } catch (JSONException e) {
            Loggers.AGENT.error("Unable to create JSON Object", e);
        }
        browsersJSON.put(config);
    }

    private void addSharedEnvironmentVariable(AgentRunningBuild runningBuild, String key, String value) {
        if (value != null) {
            Loggers.AGENT.info("Setting environment variable: " + key + " value: " + value);
            runningBuild.addSharedEnvironmentVariable(key, value);
        }
    }

    private String getAccessKey(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.SAUCE_PLUGIN_ACCESS_KEY);
    }

    private String getUsername(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.SAUCE_USER_ID_KEY);
    }


    /**
     * Generates a String that represents the Sauce OnDemand driver URL. This is used by the
     * <a href="http://selenium-client-factory.infradna.com/">selenium-client-factory</a> library to instantiate the Sauce-specific drivers.
     *
     * @param username
     * @param apiKey
     * @param feature  @return String representing the Sauce OnDemand driver URI
     */
    protected String getSodDriverUri(String username, String apiKey, Browser browser, AgentBuildFeature feature) {
        StringBuilder sb = new StringBuilder("sauce-ondemand:?username=");
        sb.append(username);
        sb.append("&access-key=").append(apiKey);
        if (browser != null) {
            sb.append("&os=").append(browser.getOs());
            sb.append("&browser=").append(browser.getBrowserName());
            sb.append("&browser-version=").append(browser.getVersion());
        }
        sb.append("&max-duration=").append(feature.getParameters().get(Constants.SELENIUM_MAX_DURATION_KEY));
        sb.append("&idle-timeout=").append(feature.getParameters().get(Constants.SELENIUM_IDLE_TIMEOUT_KEY));

        return sb.toString();
    }

    /**
     * @param feature contains the Sauce information set by the user within the build configuration
     * @return
     */
    private String[] getSelectedBrowsers(AgentBuildFeature feature) {
        Loggers.AGENT.info("Retrieving parameter: " + Constants.SELENIUM_SELECTED_BROWSER);
        String selectedBrowser = feature.getParameters().get(Constants.SELENIUM_SELECTED_BROWSER);
        Loggers.AGENT.info("Parameter value: " + selectedBrowser);
        if (selectedBrowser != null) {
            String[] selectedBrowsers = selectedBrowser.split(",");
            if (selectedBrowsers.length != 0) {
                return selectedBrowsers;
            }
        }
        Loggers.AGENT.info("Retrieving parameter: " + Constants.SELENIUM_WEB_DRIVER_BROWSERS);
        selectedBrowser = feature.getParameters().get(Constants.SELENIUM_WEB_DRIVER_BROWSERS);
        Loggers.AGENT.info("Parameter value: " + selectedBrowser);
        if (selectedBrowser != null) {
            return selectedBrowser.split(",");
        } else {
            return new String[]{};
        }
    }
}
