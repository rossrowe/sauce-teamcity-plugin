package com.saucelabs.teamcity.results;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.saucerest.SauceREST;
import com.saucelabs.teamcity.Constants;
import jetbrains.buildServer.serverSide.BuildsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.web.openapi.BuildTab;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Ross Rowe
 */
public class SauceBuildResultsTab extends BuildTab {

    private static final Logger logger = Logger.getLogger(SauceBuildResultsTab.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd-HH";

    public static final String JOB_DETAILS_URL = "http://saucelabs.com/rest/v1/%1$s/build/%2$s/jobs?full=true";

    private static final String HMAC_KEY = "HMACMD5";

    private PluginDescriptor myPluginDescriptor;

    protected SauceBuildResultsTab(WebControllerManager manager, BuildsManager buildManager, PluginDescriptor myPluginDescriptor) {
        super("sauceBuildResults", "Sauce Labs Results", manager, buildManager, myPluginDescriptor.getPluginResourcesPath("sauceBuildResults.jsp"));
        this.myPluginDescriptor = myPluginDescriptor;
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model, @NotNull SBuild build) {
        //invoke Sauce REST API to retrieve job ids for TC build
        try {
            List<JobInformation> jobs = retrieveJobIdsFromSauce(build);
            model.put("jobs", jobs);
        } catch (IOException e) {
            logger.error("Error retrieving job information", e);
        } catch (JSONException e) {
            logger.error("Error retrieving job information", e);
        } catch (InvalidKeyException e) {
            logger.error("Error retrieving job information", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error retrieving job information", e);
        }
    }

    public List<JobInformation> retrieveJobIdsFromSauce(SBuild build) throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException {
        //invoke Sauce Rest API to find plan results with those values
        List<JobInformation> jobInformation = new ArrayList<JobInformation>();

        SBuildFeatureDescriptor sauceBuildFeature = getSauceBuildFeature(build);
        if (sauceBuildFeature == null) {
            return null;
        }
        String username = sauceBuildFeature.getParameters().get(Constants.SAUCE_USER_ID_KEY);
        String accessKey = sauceBuildFeature.getParameters().get(Constants.SAUCE_PLUGIN_ACCESS_KEY);
        String buildNumber = build.getBuildTypeExternalId() + build.getBuildNumber();
        SauceREST sauceREST = new SauceREST(username, accessKey);
        String jsonResponse = sauceREST.retrieveResults(new URL(String.format(JOB_DETAILS_URL, username, buildNumber)));
        JSONObject job = new JSONObject(jsonResponse);
        JSONArray jobResults = job.getJSONArray("jobs");

        for (int i = 0; i < jobResults.length(); i++) {
            //check custom data to find job that was for build
            JSONObject jobData = jobResults.getJSONObject(i);
            String jobId = jobData.getString("id");
            JobInformation information = new JobInformation(jobId, calcHMAC(username, accessKey, jobId));
            String status = jobData.getString("passed");
            if (status.equals("null")) {
                status = "not set";
            }
            information.setStatus(status);
            information.setName(jobData.getString("name"));
            jobInformation.add(information);
        }
        //the list of results retrieved from the Sauce REST API is last-first, so reverse the list
        Collections.reverse(jobInformation);
        return jobInformation;
    }

    private SBuildFeatureDescriptor getSauceBuildFeature(SBuild build) {
        Collection<SBuildFeatureDescriptor> features = build.getBuildType().getBuildFeatures();
        if (features.isEmpty()) return null;
        for (SBuildFeatureDescriptor feature : features) {
            if (feature.getType().equals("sauce")) {
                return feature;
            }

        }
        return null;
    }

    @Override
    protected boolean isAvailableFor(@NotNull SBuild build) {
        //return true if sauce is configured
        return getSauceBuildFeature(build) != null && super.isAvailableFor(build); //should return true
    }

    public String calcHMAC(String username, String accessKey, String jobId) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String key = username + ":" + accessKey + ":" + format.format(calendar.getTime());
        byte[] keyBytes = key.getBytes();
        SecretKeySpec sks = new SecretKeySpec(keyBytes, HMAC_KEY);
        Mac mac = Mac.getInstance(sks.getAlgorithm());
        mac.init(sks);
        byte[] hmacBytes = mac.doFinal(jobId.getBytes());
        byte[] hexBytes = new Hex().encode(hmacBytes);
        return new String(hexBytes, "ISO-8859-1");
    }
}
