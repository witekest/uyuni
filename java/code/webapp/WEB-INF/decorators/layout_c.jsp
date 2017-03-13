<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"
%><%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"
%><%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"
%><%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator"
%><%@ taglib uri="http://www.opensymphony.com/sitemesh/page" prefix="page"
%><%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn"
%><%@ page contentType="text/html; charset=UTF-8"
%><!DOCTYPE HTML>
<html:html>
  <head>
    <jsp:include page="layout_head.jsp" />
    <decorator:head />
  </head>
  <body onload="<decorator:getProperty property="body.onload" />">
    <header class="navbar-pf">
      <jsp:include page="/WEB-INF/includes/header.jsp" />
    </header>
    <div class="spacewalk-main-column-layout">
      <aside id="spacewalk-aside" class="collapse">
        <div id="nav"></div>
        <jsp:include page="/WEB-INF/includes/leftnav.jsp" />
      </aside>
      <section id="spacewalk-content">
        <div id="breadcrumb"></div>
        <noscript>
            <div class="alert alert-danger">
                <bean:message key="common.jsp.noscript"/>
            </div>
        </noscript>
        <!-- Alerts and messages -->
        <logic:messagesPresent>
          <div class="alert alert-warning">
            <ul>
            <html:messages id="message">
              <li><c:out value="${message}"/></li>
            </html:messages>
            </ul>
          </div>
        </logic:messagesPresent>
        <html:messages id="message" message="true">
          <rhn:messages><c:out escapeXml="false" value="${message}" /></rhn:messages>
        </html:messages>
        <decorator:body />
      </section>
      <script src='/javascript/manager/menu.bundle.js'></script>
    </div>
    <footer>
      <jsp:include page="/WEB-INF/includes/footer.jsp" />
    </footer>
  </body>
</html:html>
