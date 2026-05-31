package com.jullyscraft.template;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailTemplateBuilder {

    private final TemplateEngine templateEngine;

    public String build(String templateName, Map<String, Object> variables) {
        Context ctx = new Context();
        if (variables != null) {
            variables.forEach(ctx::setVariable);
        }
        return templateEngine.process(templateName, ctx);
    }
}