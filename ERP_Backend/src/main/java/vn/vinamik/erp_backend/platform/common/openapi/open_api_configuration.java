package vn.vinamik.erp_backend.platform.common.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ERP Vinamik API",
                version = "v1",
                description = "API dung chung cho he thong ERP Vinamik."
        ),
        security = @SecurityRequirement(name = "erp_session")
)
@SecurityScheme(
        name = "erp_session",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "erp_session",
        description = "Cookie phien dang nhap ERP. Dang nhap truoc khi dung Try it out."
)
public class open_api_configuration {
}
