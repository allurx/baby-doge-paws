package red.zyc.babydogepaws.config;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author allurx
 */
@Configuration
public class SpringDocConfig {

    public static final String PARAMETER_COMPONENT_USER_PHONE_NUMBER = "#/components/parameters/userPhoneNumber";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()

                // 接口文档基本信息
                .info(new Info()
                        .title("BabyDogePaws API")
                        .version("v1.0.0")
                        .description("故事你真的在听吗"))

                // 添加可选的服务器列表
                .servers(List.of(
                        new Server().url("http://localhost").description("dev"),
                        new Server().url("http://119.28.74.167").description("prod")))

                // 添加通用（能够在其它地方复用）的构件，同样的请求或者响应可以复用一个component，避免重复代码
                .components(new Components()

                        // 添加通用的请求参数
                        .addParameters("userPhoneNumber", new Parameter()
                                .in(ParameterIn.QUERY.name())
                                // 定义请求参数的名称
                                .name("phoneNumber")
                                .schema(new StringSchema())
                                .required(true)
                                .description("用户手机号码")
                                .example("19962006575"))

                        // 添加基于http request header key的认证全方案
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)

                                // 客户端请求时必须带上这个请求头名称
                                .name(WebSecurityConfig.HeaderCheckFilter.REQUIRED_HEADER_NAME))
                )

                // 将上面名称为"RequestHeaderAuth"的SecurityScheme应用到OpenAPI文档中的所有操作中去，
                // 也可以为单个操作应用这个SecurityScheme，看实际需求
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))

                ;

    }

    @Autowired
    public void swaggerUiConfigProperties(SwaggerUiConfigProperties swaggerUiConfigProperties) {
        swaggerUiConfigProperties.setDocExpansion("NONE");
    }

}
