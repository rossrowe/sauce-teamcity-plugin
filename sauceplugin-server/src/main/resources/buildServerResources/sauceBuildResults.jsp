<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>

<jsp:useBean id="build" scope="request" type="jetbrains.buildServer.serverSide.SBuild"/>
<jsp:useBean id="jobs" scope="request" type="java.util.ArrayList"/>
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
                    <a href="<%=request.getAttribute("javax.servlet.forward.request_uri")%>?<%=request.getAttribute("javax.servlet.forward.query_string")%>&jobId=${jobInfo.jobId}&hmac=${jobInfo.hmac}">${jobInfo.jobId}</a>
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

<c:choose>
    <c:when test="${param.jobId != null}">
        <div id="sauce-job" class="groupBox">
            <h2>Details for ${param.jobId}</h2>
            <script type="text/javascript" src="https://saucelabs.com/job-embed/${param.jobId}.js?auth=${param.hmac}"/>
        </div>
    </c:when>
    <c:otherwise>


    </c:otherwise>
</c:choose>
<div id="sauce-job-results" class="groupBox"></div>