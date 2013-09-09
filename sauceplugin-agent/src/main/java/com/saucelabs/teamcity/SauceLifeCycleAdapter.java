package com.saucelabs.teamcity;

import com.saucelabs.ci.Browser;
import com.saucelabs.ci.BrowserFactory;
import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Ross Rowe
 */
public class SauceLifeCycleAdapter extends AgentLifeCycleAdapter {

    private final InspectionReporter myInspectionReporter;
    private final DuplicatesReporter myDuplicatesReporter;
    private AgentRunningBuild myBuild;
    private BrowserFactory sauceBrowserFactory;
    private SauceConnectTwoManager sauceTunnelManager;

    public SauceLifeCycleAdapter(
            @NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher,
            @NotNull jetbrains.buildServer.agent.inspections.InspectionReporter inspectionReporter,
            @NotNull DuplicatesReporter duplicatesReporter,
            BrowserFactory sauceBrowserFactory,
            SauceConnectTwoManager sauceTunnelManager) {
        agentDispatcher.addListener(this);
        this.myInspectionReporter = inspectionReporter;
        this.myDuplicatesReporter = duplicatesReporter;
        this.sauceBrowserFactory = sauceBrowserFactory;
        this.sauceTunnelManager = sauceTunnelManager;
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        super.beforeRunnerStart(runner);
    }

    @Override
    public void beforeBuildFinish(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
        super.beforeBuildFinish(build, buildStatus);
        Collection<AgentBuildFeature> features = getBuild().getBuildFeaturesOfType("sauce");
        if (features.isEmpty()) return;
        for (AgentBuildFeature feature : features) {
            sauceTunnelManager.closeTunnelsForPlan(getUsername(feature), null);
        }
        //invoke Sauce REST API to store build information
    }

    @Override
    public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
        super.buildStarted(runningBuild);
        this.myBuild = runningBuild;
        Collection<AgentBuildFeature> features = getBuild().getBuildFeaturesOfType("sauce");
        if (features.isEmpty()) return;
        for (AgentBuildFeature feature : features) {
            populateEnvironmentVariables(runningBuild, feature);
            if (shouldStartSauceConnect(feature)) {
                startSauceConnect(feature);
            }
        }
    }

    private void startSauceConnect(AgentBuildFeature feature) {
        try {
            sauceTunnelManager.openConnection(
                    getUsername(feature),
                    getAccessKey(feature),
                    getSauceConnectPort(feature),
                    null,
                    feature.getParameters().get(Constants.SAUCE_CONNECT_OPTIONS),
                    feature.getParameters().get(Constants.SAUCE_HTTPS_PROTOCOL),
                    null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getSauceConnectPort(AgentBuildFeature feature) {
        String port = feature.getParameters().get(Constants.SELENIUM_PORT_KEY);
        if (port == null) {
            port = "4445";
        }
        return Integer.parseInt(port);
    }

    private boolean shouldStartSauceConnect(AgentBuildFeature feature) {
        String useSauceConnect = feature.getParameters().get(Constants.SAUCE_CONNECT_KEY);
        return useSauceConnect != null && useSauceConnect.equals("true");
    }

    private void populateEnvironmentVariables(AgentRunningBuild runningBuild, AgentBuildFeature feature) {

        String userName = getUsername(feature);
        String apiKey = getAccessKey(feature);
        String sodDriverURI = getSodDriverUri(userName, apiKey, runningBuild, feature);
        addSharedEnvironmentVariable(runningBuild,Constants.SAUCE_USER_NAME, userName);
        addSharedEnvironmentVariable(runningBuild,Constants.SAUCE_API_KEY, apiKey);
        addSharedEnvironmentVariable(runningBuild,Constants.SELENIUM_DRIVER_ENV, sodDriverURI);
        addSharedEnvironmentVariable(runningBuild,Constants.SELENIUM_HOST_ENV, feature.getParameters().get(Constants.SELENIUM_HOST_KEY));
        addSharedEnvironmentVariable(runningBuild,Constants.SELENIUM_PORT_ENV, feature.getParameters().get(Constants.SELENIUM_PORT_KEY));
        addSharedEnvironmentVariable(runningBuild,Constants.SELENIUM_STARTING_URL_ENV, feature.getParameters().get(Constants.SELENIUM_STARTING_URL_KEY));
        addSharedEnvironmentVariable(runningBuild,Constants.SELENIUM_MAX_DURATION_ENV, feature.getParameters().get(Constants.SELENIUM_MAX_DURATION_KEY));
        addSharedEnvironmentVariable(runningBuild,Constants.SELENIUM_IDLE_TIMEOUT_ENV, feature.getParameters().get(Constants.SELENIUM_IDLE_TIMEOUT_KEY));

    }

    private void addSharedEnvironmentVariable(AgentRunningBuild runningBuild, String key, String value) {
        if (value != null) {
            runningBuild.addSharedEnvironmentVariable(key, value);
        }
    }

    private String getAccessKey(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.SAUCE_PLUGIN_ACCESS_KEY);
    }

    private String getUsername(AgentBuildFeature feature) {
        return feature.getParameters().get(Constants.SAUCE_USER_ID_KEY);
    }

    private AgentRunningBuild getBuild() {
        return myBuild;
    }

    @Override
    public void messageLogged(@NotNull BuildMessage1 buildMessage) {
        super.messageLogged(buildMessage);
    }

    @Override
    public void messageLogged(@NotNull AgentRunningBuild build, @NotNull BuildMessage1 buildMessage) {
        super.messageLogged(build, buildMessage);
    }

    /**
     * Generates a String that represents the Sauce OnDemand driver URL. This is used by the
     * <a href="http://selenium-client-factory.infradna.com/">selenium-client-factory</a> library to instantiate the Sauce-specific drivers.
     *
     * @param username
     * @param apiKey
     * @param runningBuild
     * @param feature      @return String representing the Sauce OnDemand driver URI
     */
    protected String getSodDriverUri(String username, String apiKey, AgentRunningBuild runningBuild, AgentBuildFeature feature) {
        StringBuilder sb = new StringBuilder("sauce-ondemand:?username=");
        sb.append(username);
        sb.append("&access-key=").append(apiKey);

        String[] selectedBrowsers = getSelectedBrowsers(feature);
        if (selectedBrowsers.length == 1) {
            Browser browser = sauceBrowserFactory.webDriverBrowserForKey(feature.getParameters().get(Constants.SELENIUM_SELECTED_BROWSER));
            if (browser != null) {
                sb.append("&os=").append(browser.getOs());
                sb.append("&browser=").append(browser.getBrowserName());
                sb.append("&browser-version=").append(browser.getVersion());
            }
        }

//        sb.append("&firefox-profile-url=").append(StringUtils.defaultString(feature.getFirefoxProfileUrl()));
        sb.append("&max-duration=").append(feature.getParameters().get(Constants.SELENIUM_MAX_DURATION_KEY));
        sb.append("&idle-timeout=").append(feature.getParameters().get(Constants.SELENIUM_IDLE_TIMEOUT_KEY));
//        sb.append("&user-extensions-url=").append(StringUtils.defaultString(feature.getUserExtensionsJson()));

        return sb.toString();
    }

    private String[] getSelectedBrowsers(AgentBuildFeature config) {
        return new String[0];  //To change body of created methods use File | Settings | File Templates.
    }
}
