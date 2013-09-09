package com.saucelabs.teamcity.listener;

import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.teamcity.Constants;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.buildLog.LogMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Ross Rowe
 */
public class SauceServerAdapter extends BuildServerAdapter {

    private static final String SAUCE_ON_DEMAND_SESSION_ID = "SauceOnDemandSessionID";
    private final SBuildServer myBuildServer;

    private static final Logger logger = Logger.getLogger(SauceServerAdapter.class);

    public SauceServerAdapter(SBuildServer sBuildServer) {
        myBuildServer = sBuildServer;
    }

    public void register() {
        myBuildServer.addListener(this);
    }

    @Override
    public void buildFinished(SRunningBuild build) {
        super.buildFinished(build);

        Iterator<LogMessage> iterator = build.getBuildLog().getMessagesIterator();
        while (iterator.hasNext()) {
            LogMessage logMessage = iterator.next();
            String line = logMessage.getText();
            if (StringUtils.containsIgnoreCase(line, SAUCE_ON_DEMAND_SESSION_ID)) {
                //extract session id
                String sessionId = StringUtils.substringBetween(line, SAUCE_ON_DEMAND_SESSION_ID + "=", " ");
                if (sessionId == null) {
                    //we might not have a space separating the session id and job-name, so retrieve the text up to the end of the string
                    sessionId = StringUtils.substringAfter(line, SAUCE_ON_DEMAND_SESSION_ID + "=");
                }
                if (sessionId != null && !sessionId.equalsIgnoreCase("null")) {
                    storeBuildNumberInSauce(build, sessionId);
                    build.getTags().add(sessionId);
                }
            }
        }
    }

    private void storeBuildNumberInSauce(SRunningBuild build, String sessionId) {
        Collection<SBuildFeatureDescriptor> features = build.getBuildType().getBuildFeatures();
        if (features.isEmpty()) return;
        for (SBuildFeatureDescriptor feature : features) {
            if (feature.getType().equals("sauce")) {
                SauceREST sauceREST = new SauceREST(getUsername(feature), getAccessKey(feature));
                Map<String, Object> updates = new HashMap<String, Object>();
                try {
                    String json = sauceREST.getJobInfo(sessionId);
                    JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
                    updates.put("build", build.getBuildTypeExternalId() + build.getBuildNumber());
                    if (jsonObject.get("passed") == null || jsonObject.get("passed").equals("")) {
                        if (build.getStatusDescriptor().getStatus().isSuccessful()) {
                            updates.put("passed", Boolean.TRUE.toString());
                        } else if (build.getStatusDescriptor().getStatus().isFailed()) {
                            updates.put("passed", Boolean.FALSE.toString());
                        }
                    }

                    sauceREST.updateJobInfo(sessionId, updates);
                } catch (org.json.simple.parser.ParseException e) {
                    logger.error("Failed to parse JSON", e);
                }
            }
        }
    }

    private String getAccessKey(SBuildFeatureDescriptor feature) {
        return feature.getParameters().get(Constants.SAUCE_PLUGIN_ACCESS_KEY);
    }

    private String getUsername(SBuildFeatureDescriptor feature) {
        return feature.getParameters().get(Constants.SAUCE_USER_ID_KEY);
    }

    @Override
    public void buildStarted(SRunningBuild build) {
        super.buildStarted(build);

    }
}
