package com.swancms.plug.web.back;

import static com.swancms.core.constant.Constants.CREATE;
import static com.swancms.core.constant.Constants.DELETE_SUCCESS;
import static com.swancms.core.constant.Constants.EDIT;
import static com.swancms.core.constant.Constants.MESSAGE;
import static com.swancms.core.constant.Constants.OPRT;
import static com.swancms.core.constant.Constants.SAVE_SUCCESS;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.swancms.aws.s3.service.AWSS3Service;
import com.swancms.common.orm.RowSide;
import com.swancms.common.web.Servlets;
import com.swancms.core.constant.Constants;
import com.swancms.core.domain.Site;
import com.swancms.core.service.OperationLogService;
import com.swancms.core.support.Backends;
import com.swancms.core.support.Context;
import com.swancms.plug.domain.EmployeeInfo;
import com.swancms.plug.service.EmployeeInfoService;
//updated 05/07/2024 by runxin
import com.swancms.plug.domain.EmployeeInfoPending;
import com.swancms.plug.service.EmployeeInfoPendingService;
import com.swancms.plug.domain.EmployeeInfoFinal;
import com.swancms.plug.service.EmployeeInfoFinalService;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Cell;

import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.TimeZone;
import java.time.ZonedDateTime;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.kernel.geom.PageSize;
import java.math.BigDecimal;
import java.util.Arrays;

@Controller
@RequestMapping("/plug/employee")
public class EmployeeInfoController {
	private static final Logger logger = LoggerFactory
            .getLogger(EmployeeInfoController.class);

    @Autowired
    private OperationLogService logService;
    @Autowired
    private EmployeeInfoService service;
    // updated 07/13/2024 by runxin
    @Autowired
    private EmployeeInfoPendingService service_pending;
    @Autowired
    private EmployeeInfoFinalService service_final;
    @Autowired
    private AWSS3Service s3Service;

	
    @RequiresPermissions("plug:employee:list")
    @GetMapping("list.do")
    public String list(@PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                       HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        Page<EmployeeInfo> pagedList = service.findAll(siteId, params, pageable);
        modelMap.addAttribute("pagedList", pagedList);
        return "plug/employee/employee_list";
    }
    // updated 07/15 by runxin 
    @GetMapping("pending_list.do")
    public String pendingList(@PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                              HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        Page<EmployeeInfoPending> pagedList = service_pending.findAll(siteId, params, pageable);
        modelMap.addAttribute("pagedList", pagedList);
        modelMap.addAttribute("search_CONTAIN_lastName", params.get("search_CONTAIN_lastName"));
        modelMap.addAttribute("search_CONTAIN_position", params.get("search_CONTAIN_position"));
        return "plug/employee/employee_pending_list";
    }
    
    //updated 07/24 by runxin
    @GetMapping("final_list.do")
    public String finalList(@PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                              HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        Page<EmployeeInfoFinal> pagedList = service_final.findAll(siteId, params, pageable);
        modelMap.addAttribute("pagedList", pagedList);
        modelMap.addAttribute("search_CONTAIN_lastName", params.get("search_CONTAIN_lastName"));
        modelMap.addAttribute("search_CONTAIN_position", params.get("search_CONTAIN_position"));
        return "plug/employee/employee_final_list";
    }
    
    

    @RequiresPermissions("plug:employee:create")
    @GetMapping("create.do")
    public String create(Integer id, org.springframework.ui.Model modelMap) {
        Site site = Context.getCurrentSite();
        if (id != null) {
        	EmployeeInfo bean = service.get(id);
            Backends.validateDataInSite(bean, site.getId());
            modelMap.addAttribute("bean", bean);
        }
        modelMap.addAttribute(OPRT, CREATE);
        return "plug/employee/employee_form_create";
    }
    
 //   @RequiresPermissions("plug:employee:view")
    @GetMapping("view.do")
    public String view(Integer id, Integer position,
                       @PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                       HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        EmployeeInfo bean = service.get(id);
        Backends.validateDataInSite(bean, siteId);
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        RowSide<EmployeeInfo> side = service.findSide(siteId, params, bean, position, pageable.getSort());
        modelMap.addAttribute("bean", bean);
        modelMap.addAttribute("side", side);
        modelMap.addAttribute("position", position);
        modelMap.addAttribute(OPRT, "view");
        return "plug/employee/employee_form_view";
    }

    
    // updated 07/16 by runxin
    //@RequiresPermissions("plug:employee:view")
    @GetMapping("view_pending.do")
    public String view_pending(Integer id, Integer position,
                       @PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                       HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        EmployeeInfoPending pendingBean = service_pending.get(id);
        Backends.validateDataInSite(pendingBean, siteId);
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        RowSide<EmployeeInfoPending> side = service_pending.findSide(siteId, params, pendingBean, position, pageable.getSort());
        modelMap.addAttribute("pendingBean", pendingBean);
        modelMap.addAttribute("side", side);
        modelMap.addAttribute("position", position);
        modelMap.addAttribute(OPRT, "view");
        return "plug/employee/employee_form_view_pending";
    }
    
    @GetMapping("view_final.do")
    public String view_final(Integer id, Integer position,
                       @PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                       HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        EmployeeInfoFinal finalBean = service_final.get(id);
        Backends.validateDataInSite(finalBean, siteId);
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        RowSide<EmployeeInfoFinal> side = service_final.findSide(siteId, params, finalBean, position, pageable.getSort());
        modelMap.addAttribute("finalBean", finalBean);
        modelMap.addAttribute("side", side);
        modelMap.addAttribute("position", position);
        modelMap.addAttribute(OPRT, "view");
        return "plug/employee/employee_form_view_final";
    }

    
    @RequiresPermissions("plug:employee:edit")
    @GetMapping("edit.do")
    public String edit(Integer id, Integer position,
    		@PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                       HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        EmployeeInfo bean = service.get(id);
        Backends.validateDataInSite(bean, siteId);
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        RowSide<EmployeeInfo> side = service.findSide(siteId, params, bean, position, pageable.getSort());
        modelMap.addAttribute("bean", bean);
        modelMap.addAttribute("side", side);
        //modelMap.addAttribute("position", position);
        modelMap.addAttribute(OPRT, EDIT);
        return "plug/employee/employee_form_edit";
    }
    
    //updated by runxin 07/16/2024
    @RequiresPermissions("plug:employee:edit")
    @GetMapping("edit_pending.do")
    public String edit_pending(Integer id, Integer position,
    		@PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable,
                       HttpServletRequest request, org.springframework.ui.Model modelMap) {
        Integer siteId = Context.getCurrentSiteId();
        EmployeeInfoPending pendingBean = service_pending.get(id);
        Backends.validateDataInSite(pendingBean, siteId);
        Map<String, String[]> params = Servlets.getParamValuesMap(request, Constants.SEARCH_PREFIX);
        RowSide<EmployeeInfoPending> side = service_pending.findSide(siteId, params, pendingBean, position, pageable.getSort());
        modelMap.addAttribute("pendingBean", pendingBean);
        modelMap.addAttribute("side", side);
        //modelMap.addAttribute("position", position);
        modelMap.addAttribute(OPRT, EDIT);
        return "plug/employee/employee_form_edit_pending";
    }
    

    @RequiresPermissions("plug:employee:save")
    @PostMapping("save.do")
    public String save(@Valid EmployeeInfo bean, String redirect, HttpServletRequest request, RedirectAttributes ra) {
        Integer siteId = Context.getCurrentSiteId();
        service.save(bean, siteId);
        logService.operation("opr.employee.add", bean.getFirstName()+" "+bean.getLastName(), null, bean.getId(), request);
        logger.info("save EmployeeInfo, name={}.", bean.getFirstName()+" "+bean.getLastName());
        ra.addFlashAttribute(MESSAGE, SAVE_SUCCESS);
        if (Constants.REDIRECT_LIST.equals(redirect)) {
            return "redirect:list.do";
        } else if (Constants.REDIRECT_CREATE.equals(redirect)) {
            return "redirect:create.do";
        } else {
            ra.addAttribute("id", bean.getId());
            return "redirect:edit.do";
        }
    }
    
    //updated by runxin 07/16/2024
    @RequiresPermissions("plug:employee:save")
    @PostMapping("save_pending.do")
    public String save_pending(@Valid EmployeeInfoPending pendingBean, String redirect, HttpServletRequest request, RedirectAttributes ra) {
        Integer siteId = Context.getCurrentSiteId();
        // Set approvedDate to current timestamp     
        service_pending.save(pendingBean, siteId);
        logService.operation("opr.employee.add", pendingBean.getFirstName()+" "+pendingBean.getLastName(), null, pendingBean.getId(), request);
        logger.info("save EmployeeInfoPending, name={}.", pendingBean.getFirstName()+" "+pendingBean.getLastName());
        ra.addFlashAttribute(MESSAGE, SAVE_SUCCESS);
        if (Constants.REDIRECT_LIST.equals(redirect)) {
            return "redirect:pending_list.do";
        } else if (Constants.REDIRECT_CREATE.equals(redirect)) {// runxin, check later
            return "redirect:create.do";
        } else {
            ra.addAttribute("id", pendingBean.getId());
            return "redirect:edit_pending.do";
        }
    }

    @RequiresPermissions("plug:employee:update")
    @PostMapping("update.do")
    public String update(@ModelAttribute("bean") EmployeeInfo bean, Integer position,
                         String redirect, HttpServletRequest request, RedirectAttributes ra) {
        Site site = Context.getCurrentSite();
        Backends.validateDataInSite(bean, site.getId());
        service.update(bean);
        logService.operation("opr.employee.edit", bean.getFirstName()+" "+bean.getLastName(), null,
                bean.getId(), request);
        logger.info("update EmployeeInfo, name={}.", bean.getFirstName()+" "+bean.getLastName());
        ra.addFlashAttribute(MESSAGE, SAVE_SUCCESS);
        if (Constants.REDIRECT_LIST.equals(redirect)) {
            return "redirect:list.do";
        } else {
            ra.addAttribute("id", bean.getId());
            ra.addAttribute("position", position);
            return "redirect:edit.do";
        }
    }

    
    //updated by runxin 07/16/2024
    @RequiresPermissions("plug:employee:update")
    @PostMapping("update_pending.do")
    public String update_pending(@ModelAttribute("pendingBean") EmployeeInfoPending pendingBean, Integer position,
                         String redirect, HttpServletRequest request, RedirectAttributes ra) {
        Site site = Context.getCurrentSite();
        Backends.validateDataInSite(pendingBean, site.getId());
        if (pendingBean != null) {
        	pendingBean.applyDefaultValue();
            
            service_pending.update(pendingBean);
            logService.operation("opr.employee.edit", pendingBean.getFirstName() + " " + pendingBean.getLastName(), null,
            		pendingBean.getId(), request);
            logger.info("update EmployeeInfoPending, name={}.", pendingBean.getFirstName() + " " + pendingBean.getLastName());
            ra.addFlashAttribute(MESSAGE, SAVE_SUCCESS);
            if (Constants.REDIRECT_LIST.equals(redirect)) {
                return "redirect:pending_list.do";
            } else {
                ra.addAttribute("id", pendingBean.getId());
                ra.addAttribute("position", position);
                return "redirect:edit_pending.do";
            }
        } else {
            logger.error("Bean is null in update_pending method, runxin check");
            ra.addFlashAttribute(MESSAGE, "Bean is null. Update failed.");
            return "redirect:pending_list.do";
        }    
    }
    
    
    @RequiresPermissions("plug:employee:delete")
    @RequestMapping("delete.do")
    public String delete(Integer[] ids, HttpServletRequest request,
                         RedirectAttributes ra) {
        Site site = Context.getCurrentSite();
        validateIds(ids, site.getId());
        EmployeeInfo[] beans = service.delete(ids);
        for (EmployeeInfo bean : beans) {
            logService.operation("opr.employee.delete", bean.getFirstName()+" "+bean.getLastName(), null, bean.getId(), request);
            logger.info("delete EmployeeInfo, name={}.", bean.getFirstName()+" "+bean.getLastName());
        }
        ra.addFlashAttribute(MESSAGE, DELETE_SUCCESS);
        return "redirect:list.do";
    }
    
    
    //updated by runxin 07/16/2024
    @RequiresPermissions("plug:employee:delete")
    @RequestMapping("delete_pending.do")
    public String delete_pending(Integer[] ids, HttpServletRequest request,
                         RedirectAttributes ra) {
        Site site = Context.getCurrentSite();
        validatePendingIds(ids, site.getId());
        EmployeeInfoPending[] beans = service_pending.delete(ids);
        for (EmployeeInfoPending bean : beans) {
            logService.operation("opr.employee.delete", bean.getFirstName()+" "+bean.getLastName(), null, bean.getId(), request);
            logger.info("delete EmployeeInfoPending, name={}.", bean.getFirstName()+" "+bean.getLastName());
        }
        ra.addFlashAttribute(MESSAGE, DELETE_SUCCESS);
        return "redirect:pending_list.do";
    }

    @ModelAttribute("bean")
    public EmployeeInfo preloadBean(@RequestParam(required = false) Integer oid) {
        return oid != null ? service.get(oid) : null;
    }

    private void validateIds(Integer[] ids, Integer siteId) {
        for (Integer id : ids) {
            Backends.validateDataInSite(service.get(id), siteId);
        }
    }
    
    //updated by runxin 07/18/2024
    @ModelAttribute("pendingBean")
    public EmployeeInfoPending preloadPendingBean(@RequestParam(required = false) Integer oid) {
        return oid != null ? service_pending.get(oid) : null;
    }
    private void validatePendingIds(Integer[] ids, Integer siteId) {
        for (Integer id : ids) {
            Backends.validateDataInSite(service_pending.get(id), siteId);
        }
    }
    
    @ModelAttribute("finalBean")
    public EmployeeInfoFinal preloadFinalBean(@RequestParam(required = false) Integer oid) {
        return oid != null ? service_final.get(oid) : null;
    }
    private void validateFinalIds(Integer[] ids, Integer siteId) {
        for (Integer id : ids) {
            Backends.validateDataInSite(service_final.get(id), siteId);
        }
    }
    
/* Involves downloading resumes, uses getResumePath from the Resume.java domain class which is not required within EmployeeInfo.java
   // @RequiresPermissions("plug:resume:download")
    @RequestMapping("download.do")
    public void download(Integer id, HttpServletResponse response) throws JAXBException, IOException {
    	EmployeeInfo bean = service.get(id);
    	String uniqueFileName = bean.getResumePath();
        String ext = FilenameUtils.getExtension(uniqueFileName).toLowerCase();
        String dest_filename = "employee_info_" + bean.getId() + "_"+ bean.getName() + "."+ext;

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=utf-8");
        response.setContentType("application/x-download;charset=UTF-8");
        response.addHeader("Content-disposition", "filename=" + dest_filename);  
        try {
        	byte[]  downloadedFile = s3Service.downloadFile(uniqueFileName);
            response.getOutputStream().write(downloadedFile);
            
        } catch (IOException ex) {
        	logger.info("Resume download failed.");
        	logger.error("Error= {} while downloading resume.", ex.getMessage());
        }
    }
*/


	//updated 05/07/2024 by runxin
    @RequiresPermissions("plug:employee:edit")
    @PostMapping("upload_employee_list.do")
    @ResponseBody
    public Map<String, Object> uploadEmployeeList(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        logger.info("Starting file upload process");
        if (file.isEmpty()) {
            response.put("status", 400);
            response.put("message", "File is empty");
            logger.error("Uploaded file is empty");
            return response;
        }
        
        List<EmployeeInfoPending> employeeInfos = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
           //int numberOfRows = sheet.getPhysicalNumberOfRows();
           //System.out.println("Number of rows in the Excel file: " + numberOfRows);
            for (Row row : sheet) {
                if (row.getRowNum() == 0 ) continue; // Skip header row
                logger.info("Processing row " + row.getRowNum());

                EmployeeInfoPending employeeInfo = new EmployeeInfoPending();
                
                // Validate start date and birth date
                
                Date startDate = DateUtil.getJavaDate(row.getCell(5).getNumericCellValue()); // Use DateUtil for date
                Date birthDate = DateUtil.getJavaDate(row.getCell(7).getNumericCellValue()); // Use DateUtil for date
                
                String employee_id=getCellValue(row.getCell(3));
                String position=getCellValue(row.getCell(4));
                String gender=getCellValue(row.getCell(6));
                //ensure the format of mobile is an integer
                String mobile = new BigDecimal(getCellValue(row.getCell(8))).toPlainString();
                String workEmail=getCellValue(row.getCell(9));
                String personalEmail=getCellValue(row.getCell(10));
                String department=getCellValue(row.getCell(11));
                String manager=getCellValue(row.getCell(12));
                String country=getCellValue(row.getCell(13));
                String stateLocation=getCellValue(row.getCell(14));
                String city=getCellValue(row.getCell(15));
                String address=getCellValue(row.getCell(16));
                String postalCode = new BigDecimal(getCellValue(row.getCell(17))).toPlainString();
                postalCode=postalCode.replaceAll("\\.0$", "");
                String county=getCellValue(row.getCell(18));
                String remark=getCellValue(row.getCell(19));
                String comment=getCellValue(row.getCell(20));
                String importedBy=getCellValue(row.getCell(21));
                // operate based on validation function return message
                Map<String, Object> validationResult = validation(startDate, birthDate, gender,position, mobile, workEmail, personalEmail, department, manager, country, stateLocation, city, address, postalCode, county, remark);

                if (!(Boolean) validationResult.get("status")) {
                    errorMessages.add("Row " + (row.getRowNum() + 1) + ":\n " + validationResult.get("message"));
                    continue;
                }
                // Set Eastern Time Zone
                ZonedDateTime easternTime = ZonedDateTime.now(ZoneId.of("America/New_York")); 
                employeeInfo.setImportedDate(Date.from(easternTime.toInstant())); 
                employeeInfo.setFirstName(row.getCell(0).getStringCellValue().toString());  
                employeeInfo.setLastName(getCellValue(row.getCell(1)));
                employeeInfo.setMiddleName(getCellValue(row.getCell(2)));
                employeeInfo.setEmployeeID(employee_id);
                employeeInfo.setJobPosition(position);
                employeeInfo.setStartDate(startDate);
                employeeInfo.setGender(gender);
                employeeInfo.setBirthDate(birthDate);
                employeeInfo.setMobile(mobile);
                employeeInfo.setWorkEmail(workEmail);
                employeeInfo.setPersonalEmail(personalEmail);
                employeeInfo.setDepartment(department);
                employeeInfo.setManager(manager);
                employeeInfo.setCountry(country);
                employeeInfo.setStateLocation(stateLocation);
                employeeInfo.setCity(city);
                employeeInfo.setAddress(address);
                employeeInfo.setPostalCode(postalCode);
                employeeInfo.setCounty(county);
                employeeInfo.setRemark(remark); 
                employeeInfo.setComment(comment);
                employeeInfo.setImportedBy(importedBy);
                employeeInfos.add(employeeInfo); 
                
                
            }
            
            if (!errorMessages.isEmpty()) {
                response.put("status", 400);
                response.put("message", "Validation errors:\n " + String.join(";\n ", errorMessages));
                logger.error("Validation errors: " + String.join("; ", errorMessages));
                return response;
            }
            // Save all valid employee info to the database
            for (EmployeeInfoPending employeeInfo : employeeInfos) {
                service_pending.save(employeeInfo, Context.getCurrentSiteId());
            }          
            response.put("status", 200);
            response.put("message", "File uploaded successfully");
            logger.info("File uploaded successfully");
        } catch (IOException e) {
            response.put("status", 500);
            response.put("message", "Failed to process file: " + e.getMessage());
            logger.error("Failed to process file: " + e.getMessage(), e);
        }
        return response;
    }    
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case 1:
                return cell.getStringCellValue();
            default:
                return cell.toString();
        }
    }  
    // this is the validation function 
    private Map<String, Object> validation(Date startDate, Date birthDate, String gender, String position,String mobile, String workEmail, String personalEmail, String department, String manager, String country, String stateLocation, String city, String address, String postalCode, String county, String remark) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", true);
        StringBuilder errorMessage = new StringBuilder();

        // Validate age
        if (!isValidDate(startDate, birthDate)) {
            result.put("status", false);
            errorMessage.append("Invalid date. Employee must be at least 18 years old.\n");
        }
        
        // Validate gender
        if (gender == null || (!gender.equalsIgnoreCase("M") && !gender.equalsIgnoreCase("F"))) {
            result.put("status", false);
            errorMessage.append("Invalid gender. Must be 'M' or 'F'.\n ");
        }


        // Validate position
        if (position == null || (!position.equalsIgnoreCase("Intern") && !position.equalsIgnoreCase("Developer"))) {
            result.put("status", false);
            errorMessage.append("Invalid position. Must be 'Intern' or 'Developer'.\n ");
        }
        if (errorMessage.length() > 0) {
            result.put("message", errorMessage.toString().trim());
        }

        return result;
    }

    
    // Method to check if the employee is over 18 years old
    private boolean isValidDate(Date startDate, Date birthDate) {
        LocalDate birthLocalDate = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(birthLocalDate, startLocalDate).getYears() >= 18;
    }
    
    
    //updated by runxin 07/18/2024
    @PostMapping("final_upload.do")
    @ResponseBody
    public Map<String, Object> finalUpload(
        @RequestParam("id") Integer id,
        @RequestParam Map<String, String> allRequestParams) {
        Map<String, Object> response = new HashMap<>();
        try {
        	EmployeeInfoPending pendingBean = service_pending.get(id);
            
            // Print the whole data in the console
        	logger.info("PendingBean Details: " + pendingBean.toString());
            // Set the approvedBy and approvedDate
            pendingBean.setApprovedBy(allRequestParams.get("approvedBy"));
            
            ZonedDateTime easternTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
            pendingBean.setApprovedDate(Date.from(easternTime.toInstant())); 
            logger.info("PendingBean Details: " + pendingBean.toString());
            // Create EmployeeInfoFinal object and copy data
            EmployeeInfoFinal finalBean = new EmployeeInfoFinal();
            //save the data from the pendingBean to the finalBean
            finalBean.setId(pendingBean.getId());
            finalBean.setSite(pendingBean.getSite());
            finalBean.setEmployeeID(pendingBean.getEmployeeID());
            finalBean.setFirstName(pendingBean.getFirstName());
            finalBean.setLastName(pendingBean.getLastName());
            finalBean.setMiddleName(pendingBean.getMiddleName());
            finalBean.setJobPosition(pendingBean.getJobPosition());
            finalBean.setStartDate(pendingBean.getStartDate());
            finalBean.setGender(pendingBean.getGender());
            finalBean.setBirthDate(pendingBean.getBirthDate());
            finalBean.setMobile(pendingBean.getMobile());
            finalBean.setWorkEmail(pendingBean.getWorkEmail());
            finalBean.setPersonalEmail(pendingBean.getPersonalEmail());
            finalBean.setDepartment(pendingBean.getDepartment());
            finalBean.setManager(pendingBean.getManager());
            finalBean.setCountry(pendingBean.getCountry());
            finalBean.setStateLocation(pendingBean.getStateLocation());
            finalBean.setCity(pendingBean.getCity());
            finalBean.setAddress(pendingBean.getAddress());
            finalBean.setPostalCode(pendingBean.getPostalCode());
            finalBean.setCounty(pendingBean.getCounty());
            finalBean.setRemark(pendingBean.getRemark());
            finalBean.setComment(pendingBean.getComment());
            finalBean.setImportedBy(pendingBean.getImportedBy());
            finalBean.setImportedDate(pendingBean.getImportedDate());
            finalBean.setApprovedDate(pendingBean.getApprovedDate());
            finalBean.setApprovedBy(pendingBean.getApprovedBy());
            
            logger.info("finalBean Details: " + finalBean.toString());
            // Save to final table
            Integer siteId = Context.getCurrentSiteId();
            service_final.save(finalBean, siteId);
         // Delete from pending table
            service_pending.delete(id);

            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error during final upload: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
    
    //updated by runxin 07/25/2024
    //export serivce:excel file
    @GetMapping("export_excel.do")
    public void exportFinal(HttpServletResponse response, @PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable, HttpServletRequest request) {
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=employees_info_final.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "First Name", "Last Name", "Job Position", "Start Date", "Gender", "Birth Date", "Mobile", "Work Email", "Personal Email", "Department", "Manager", "Country", "State Location", "City", "County", "Address", "Postal Code", "Remark", "Comment", "Imported By", "Approved By", "Approved Date", "Imported Date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Fetch data from the database
            Integer siteId = Context.getCurrentSiteId();
            Map<String, String[]> params = new HashMap<>();
            List<EmployeeInfoFinal> employees = service_final.findAll(siteId, params, pageable).getContent();

            // Populate data rows
            //do not save middle name,site id and id data to the excel file
            int rowNum = 1;
            for (EmployeeInfoFinal employee : employees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(employee.getEmployeeID());
                row.createCell(1).setCellValue(employee.getFirstName());
                row.createCell(2).setCellValue(employee.getLastName());
                row.createCell(3).setCellValue(employee.getJobPosition());
                row.createCell(4).setCellValue(employee.getStartDate().toString());
                row.createCell(5).setCellValue(employee.getGender());
                row.createCell(6).setCellValue(employee.getBirthDate().toString());
                row.createCell(7).setCellValue(employee.getMobile());
                row.createCell(8).setCellValue(employee.getWorkEmail());
                row.createCell(9).setCellValue(employee.getPersonalEmail());
                row.createCell(10).setCellValue(employee.getDepartment());
                row.createCell(11).setCellValue(employee.getManager());
                row.createCell(12).setCellValue(employee.getCountry());
                row.createCell(13).setCellValue(employee.getStateLocation());
                row.createCell(14).setCellValue(employee.getCity());
                row.createCell(15).setCellValue(employee.getCounty());
                row.createCell(16).setCellValue(employee.getAddress());
                row.createCell(17).setCellValue(employee.getPostalCode());
                row.createCell(18).setCellValue(employee.getRemark());
                row.createCell(19).setCellValue(employee.getComment());
                row.createCell(20).setCellValue(employee.getImportedBy());
                row.createCell(21).setCellValue(employee.getApprovedBy());
                row.createCell(22).setCellValue(employee.getApprovedDate().toString());
                row.createCell(23).setCellValue(employee.getImportedDate().toString());
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //updated by runxin 07/25/2024
    //export serivce:pdf file
    
    
    @GetMapping("export_pdf.do")
    public void exportPdf(HttpServletResponse response, @PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable, HttpServletRequest request) {
    	response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=employees_info_final.pdf");

        try {
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdf);
            document.setMargins(20, 20, 20, 20);

            // Adjust column widths
            float[] columnWidths = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);

            // Add header row with styling
            String[] headers = {"Employee ID", "First Name", "Last Name", "Job Position", 
                                "Start Date", "Gender", "Birth Date", "Mobile",
                                "Department", "Manager", "Country", "State Location", "City", 
                                "Postal Code", "County"};
            for (String header : headers) {
                com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(new Paragraph(header));
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cell.setFontColor(ColorConstants.BLACK);
                cell.setBold();
                cell.setTextAlignment(TextAlignment.CENTER);
                cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
                table.addHeaderCell(cell);
            }
            // Fetch data
            Integer siteId = Context.getCurrentSiteId();
            Map<String, String[]> params = new HashMap<>();
            List<EmployeeInfoFinal> employees = service_final.findAll(siteId, params, pageable).getContent();

            // Add data rows
            for (EmployeeInfoFinal employee : employees) {
            	table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getEmployeeID())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getFirstName())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getLastName())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getJobPosition())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getStartDate().toString())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getGender())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getBirthDate().toString())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getMobile())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getDepartment())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getManager())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getCountry())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getStateLocation())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getCity())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getPostalCode())).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(employee.getCounty())).setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //updated 07/28/2024 by runxin
    //template employee information upload
    @GetMapping("import_excel.do")
    public void importFinal(HttpServletResponse response, @PageableDefault(sort = "id", direction = Direction.ASC) Pageable pageable, HttpServletRequest request) {
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=employees_info_template.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"firstName","lastName","middleName","employeeID","jobPosition","startDate","gender","birthDate","mobile","workEmail","personalEmail","department","manager","country","stateLocation","city","address","postalCode","county","remark","comment","imported by"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            workbook.write(response.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
