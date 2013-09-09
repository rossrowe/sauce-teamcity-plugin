package com.saucelabs.teamcity.settings;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ross Rowe
 */
public class SauceSystemSettings extends BuildFeature {

    private final PluginDescriptor myPluginDescriptor;

    public SauceSystemSettings(PluginDescriptor myPluginDescriptor) {
        this.myPluginDescriptor = myPluginDescriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return "sauce";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Sauce Labs Build Feature";
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return myPluginDescriptor.getPluginResourcesPath("sauceSettings.jsp");
    }
}
