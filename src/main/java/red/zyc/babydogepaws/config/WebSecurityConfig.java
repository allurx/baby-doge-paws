package red.zyc.babydogepaws.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import red.zyc.babydogepaws.common.util.WebUtil;
import red.zyc.babydogepaws.model.response.base.Response;
import red.zyc.babydogepaws.model.response.base.ResponseMessage;

import java.io.IOException;
import java.util.List;

/**
 * @author allurx
 */
@Configuration
public class WebSecurityConfig {

    @Bean
    public FilterRegistrationBean<HeaderCheckFilter> headerCheckFilterRegistration() {
        FilterRegistrationBean<HeaderCheckFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HeaderCheckFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    public static class HeaderCheckFilter implements Filter {

        private static final Logger LOGGER = LoggerFactory.getLogger(HeaderCheckFilter.class);
        private static final String REQUIRED_HEADER_VALUE = "41123b6ff55a5922fe76f1a76ca2b3f27dffd63f9242066521c1696cd6a22d26";
        private static final List<String> SWAGGER_UI_REQUEST_PREFIXES = List.of("/swagger-ui", "/v3/api-docs");
        static final String REQUIRED_HEADER_NAME = "api-key";


        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            var httpRequest = (HttpServletRequest) request;
            var requestURI = httpRequest.getRequestURI();

            // Check if the request path is for Swagger UI
            if (SWAGGER_UI_REQUEST_PREFIXES.stream().anyMatch(requestURI::startsWith)) {
                // Skip filter for Swagger UI requests
                chain.doFilter(request, response);
                return;
            }

            // Check if the request header contains the specific parameter
            String headerValue = httpRequest.getHeader(REQUIRED_HEADER_NAME);
            if (!REQUIRED_HEADER_VALUE.equals(headerValue)) {

                // Handle missing header
                WebUtil.response(response, Response.ok(ResponseMessage.UN_AUTHORIZED));
                return;
            }

            // Continue with the next filter or the request handling
            try {
                chain.doFilter(request, response);
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                WebUtil.response(response, Response.ok(ResponseMessage.INTERNAL_SERVER_ERROR));
            }
        }

    }
}
