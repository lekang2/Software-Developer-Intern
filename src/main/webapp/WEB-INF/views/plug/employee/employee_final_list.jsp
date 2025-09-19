<!-- jsp file create by runxin, on 07/15/ -->
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fnx" uri="http://java.sun.com/jsp/jstl/functionsx" %>
<%@ taglib prefix="s" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="f" uri="http://www.jspxcms.com/tags/form" %>
<%@ taglib prefix="shiro" uri="http://shiro.apache.org/tags" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/views/head.jsp"/>
    <!-- updated 07/10/2024 by runxin -->
    <!-- Added Bootstrap CSS for better styling -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script type="text/javascript">
        $(function () {
            $("#sortHead").headSort();
        });

        function confirmDelete() {
            return confirm("<s:message code='confirmDelete'/>");
        }

        function optSingle(opt) {
            if (Cms.checkeds("ids") == 0) {
                alert("<s:message code='pleaseSelectRecord'/>");
                return false;
            }
            if (Cms.checkeds("ids") > 1) {
                alert("<s:message code='pleaseSelectOne'/>");
                return false;
            }
            var id = $("input[name='ids']:checkbox:checked").val();
            location.href = $(opt + id).attr("href");
        }

        function optDelete(form) {
            if (Cms.checkeds("ids") == 0) {
                alert("<s:message code='pleaseSelectRecord'/>");
                return false;
            }
            if (!confirmDelete()) {
                return false;
            }
            form.action = 'delete_final.do';
            form.submit();
            return true;
        }
     // updated 05/07/2024 by runxin
     function optSingleFinal(opt) {
        if (Cms.checkeds("ids") == 0) {
            alert("<s:message code='pleaseSelectRecord'/>");
            return false;
        }
        if (Cms.checkeds("ids") > 1) {
            alert("<s:message code='pleaseSelectOne'/>");
            return false;
        }
        var id = $("input[name='ids']:checkbox:checked").val();
        location.href = 'edit_final.do?id=' + id + '&' + $(opt + id).attr("href");
     }
     function exportFinalToExcel() {
         location.href = 'export_excel.do';
     }
     function exportFinalToPDF() {
         location.href = 'export_pdf.do';
     }
     
    </script>
</head>
<body class="skin-blue content-body">
<jsp:include page="/WEB-INF/views/commons/show_message.jsp"/>
<div class="content-header">
    <h1><s:message code="employee.management"/> - <s:message code="employee.final"/>
        <small>(<s:message code="totalElements" arguments="${pagedList.totalElements}"/>)</small>
    </h1>
</div>
<div class="content">
    <div class="box box-primary">
        <div class="box-body table-responsive">  
            <!-- updated 07/29/2024 by runxin -->
            <!-- searching box -->
            <form class="form-inline ls-search mb-3" action="final_list.do" method="get">
                <div class="form-group mr-2">
                    <label for="searchLastName" class="sr-only"><s:message code="employee.last-name"/></label>
                    <input class="form-control input-sm" type="text" id="searchLastName" name="search_CONTAIN_lastName" placeholder="Last Name" value="${search_CONTAIN_lastName[0]}"/>
                </div>
                <div class="form-group mr-2">
                    <label for="searchPosition" class="sr-only"><s:message code="employee.position"/></label>
                    <input class="form-control input-sm" type="text" id="searchPosition" name="search_CONTAIN_position" placeholder="Position" value="${search_CONTAIN_position[0]}"/>
                </div>
                <button class="btn btn-default btn-sm" type="submit"><s:message code="search"/></button>
            </form>   
            <!-- next form -->
            <form method="post">
                <tags:search_params/>
                <div class="btn-group">
                        <shiro:hasPermission name="plug:employee:edit">
                            <button class="btn btn-default" type="button" onclick="location.href='pending_list.do?${searchstring}';"><s:message code="employee.pending"/></button>
                        </shiro:hasPermission>
                </div>
                <%-- export excel file--%>
                <div class="btn-group">
                        <shiro:hasPermission name="plug:employee:edit">
                            <button class="btn btn-primary" type="button" onclick='exportFinalToExcel()'>Export Excel</button>
                        </shiro:hasPermission>
                </div>
                <%-- export pdf file--%>
                <div class="btn-group">
                        <shiro:hasPermission name="plug:employee:edit">
                            <button class="btn btn-primary" type="button" onclick='exportFinalToPDF()'>Export PDF</button>
                        </shiro:hasPermission>
                </div>
                <table id="pagedTable" class="table table-condensed table-bordered table-hover ls-tb">
                    <thead id="sortHead" pagesort="<c:out value='${page_sort[0]}' />" pagedir="${page_sort_dir[0]}"
                           pageurl="final_list.do?page_sort={0}&page_sort_dir={1}&${searchstringnosort}">
                    <tr class="ls_table_th">
                        <th width="25"><input type="checkbox" onclick="Cms.check('ids',this.checked);"/></th>
                        <th width="130"><s:message code="operate"/></th>
                        <th width="30" class="ls-th-sort"><span class="ls-sort" pagesort="id">ID</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="employeeID">Employee ID</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="firstName">First Name</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="lastName">Last Name</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="comment">Comment</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="approvedBy">Approved By</span></th> 
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="approvedDate">Approved Date</span></th>             
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="position">Job Position</th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="department">Department</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="manager">Manager</span></th>
                        <th class="ls-th-sort"><span class="ls-sort" pagesort="stateLocation">State</span></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="finalBean" varStatus="status" items="${pagedList.content}">
                        <tr>
                        <td align="center"><input type="checkbox" name="ids" value="${finalBean.id}"/></td>
                        <td align="center">
                            
                                <a id="edit_opt_${finalBean.id}" href="view_final.do?id=${finalBean.id}&position=${pagedList.number*pagedList.size+status.index}&${searchstring}"
                                   class="ls-opt">View</a>

                        </td>
                        <td align="center"><c:out value="${finalBean.id}"/></td>
                        <td align="center"><c:out value="${finalBean.employeeID}"/></td>
                        <td align="center"><c:out value="${finalBean.firstName}"/></td>
                        <td align="center"><c:out value="${finalBean.lastName}"/></td>
                        <td align="center"><c:out value="${finalBean.comment}"/></td>
                        <td align="center"><c:out value="${finalBean.approvedBy}"/></td>
                        <td align="center"><c:out value="${finalBean.approvedDate}"/></td>
                        <td align="center"><c:out value="${finalBean.jobPosition}"/></td>
                        <td align="center"><c:out value="${finalBean.department}"/></td>
                        <td align="center"><c:out value="${finalBean.manager}"/></td>
                        <td align="center"><c:out value="${finalBean.stateLocation}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <c:if test="${fn:length(pagedList.content) le 0}">
                    <div class="ls-norecord"><s:message code="recordNotFound"/></div>
                </c:if>
            </form>
            <form action="final_list.do" method="get" class="ls-page">
                <tags:search_params excludePage="true"/>
                <tags:pagination pagedList="${pagedList}"/>
            </form>
        </div>
    </div>
</div>
</body>
</html>
