package com.yongsik.immigrationops.template.domain;

import com.yongsik.immigrationops.document.domain.DocumentType;
import java.util.List;

public record DocumentTemplate(
        DocumentType documentType,
        List<TemplateFieldDefinition> fields
) {
}

