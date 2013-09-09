<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<jsp:useBean id="build" scope="request" type="jetbrains.buildServer.serverSide.SBuild"/>
<jsp:useBean id="jobs" scope="request" type="java.util.ArrayList"/>

<script type="text/javascript">

    SL = (function () {
        return {
            displayResults: function (jobId, auth) {
                jQuery("#sauce-job-results").empty();
                var html = "<script type=\"text/javascript\" src=\"https://saucelabs.com/job-embed/";
                html += jobId;
                html += ".js?auth=";
                html += auth + "\"><";
                html += "/script>";
                jQuery("#sauce-job-results").html(html);
            }
        }
    })();
</script>
<div id="sauce-job-list" class="groupBox">
    <table>
        <tr>
            <th>Job Id</th>
            <th>Name</th>
            <th>Status</th>
        </tr>
        <c:forEach var="jobInfo" items="${jobs}">
            <tr>
                <td>
                    <a href="#" onclick="SL.displayResults('${jobInfo.jobId}', '${jobInfo.hmac}');">${jobInfo.jobId}</a>
                </td>
                <td>
                        ${jobInfo.name}
                </td>
                <td>
                        ${jobInfo.status}
                </td>
            </tr>
        </c:forEach>
    </table>
</div>
<div id="sauce-job-results" class="groupBox"></div>