package com.denden.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Email 模板載入工具
 */
@Component
@Slf4j
public class EmailTemplateLoader {
    
    private static final String TEMPLATE_BASE_PATH = "templates/email/";
    
    public String loadTemplate(String templateName, Map<String, String> variables) {
        try {
            String templatePath = TEMPLATE_BASE_PATH + templateName;
            ClassPathResource resource = new ClassPathResource(templatePath);
            
            if (!resource.exists()) {
                log.error("Email 模板檔案不存在: {}", templatePath);
                throw new IllegalArgumentException("Email 模板檔案不存在: " + templateName);
            }
            
            String template = resource.getContentAsString(StandardCharsets.UTF_8);
            log.debug("成功載入 Email 模板: {}", templateName);
            
            return replaceVariables(template, variables);
            
        } catch (IOException e) {
            log.error("讀取 Email 模板失敗: {}, 錯誤: {}", templateName, e.getMessage(), e);
            throw new IllegalArgumentException("讀取 Email 模板失敗: " + templateName, e);
        }
    }
    
    private String replaceVariables(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        
        log.debug("模板變數替換完成，替換了 {} 個變數", variables.size());
        return result;
    }
    
    public String loadTemplate(String templateName) {
        return loadTemplate(templateName, Map.of());
    }
}
