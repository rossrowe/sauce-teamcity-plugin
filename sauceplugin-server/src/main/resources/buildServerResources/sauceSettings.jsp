<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<c:url var="actionUrl" value="/sauceSettings.html"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="sauceBrowsersBean" scope="request" class="com.saucelabs.ci.BrowserFactory"/>

<c:set var="selectedBrowsers"
       value="${propertiesBean.properties['saucePlugin.webDriverBrowsers']}"/>

<c:set var="sauceEnabled"
       value="${propertiesBean.properties['saucePlugin.sauceEnabled']}"/>

<c:set var="displaySauceSettings"
       value="${not empty sauceEnabled ? true : false}"/>

<tr id="saucePlugin.sauceConnect.container">
    <th><label for="saucePlugin.sauceConnect">Start Sauce Connect:</label></th>
    <td>
        <props:checkboxProperty name="saucePlugin.sauceConnect" treatFalseValuesCorrectly="${true}" uncheckedValue="false"/>
    </td>
</tr>

<tr id="saucePlugin.userId.container">
    <th><label for="saucePlugin.userId">Sauce User test:</label></th>
    <td>
        <props:textProperty name="saucePlugin.userId"/>
    </td>
</tr>

<tr id="saucePlugin.accessKey.container">
    <th><label for="saucePlugin.accessKey">Sauce Access Key:</label></th>
    <td>
        <props:textProperty name="saucePlugin.accessKey"/>
    </td>
</tr>


<tr id="saucePlugin.webDriverBrowsers.container">
    <th><label for="saucePlugin.webDriverBrowsers">Browsers:</label></th>
    <td>
        <props:selectProperty name="saucePlugin.webDriverBrowsers" multiple="true">
            <%--<c:if test="${empty reportType}">--%>
                <%--<c:set var="selected" value="true"/>--%>
            <%--</c:if>--%>
            <%--<props:option value="" selected="${selected}">&lt;Do not process&gt;</props:option>--%>
            <c:forEach var="browser" items="${sauceBrowsersBean.webDriverBrowsers}">
                <c:set var="selected" value="false"/>
                <c:if test="${selectedBrowsers == browser.key}">
                    <c:set var="selected" value="true"/>
                </c:if>
                <props:option value="${browser.key}"
                              selected="${selected}"><c:out value="${browser.name}"/></props:option>
            </c:forEach>
        </props:selectProperty>
    </td>
</tr>