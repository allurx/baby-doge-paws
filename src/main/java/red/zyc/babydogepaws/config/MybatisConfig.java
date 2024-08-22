package red.zyc.babydogepaws.config;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

import java.util.Properties;

/**
 * @author allurx
 */
public class MybatisConfig {

    static class TT implements Interceptor {

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            return null;
        }

        @Override
        public Object plugin(Object target) {
            return Interceptor.super.plugin(target);
        }

        @Override
        public void setProperties(Properties properties) {
            Interceptor.super.setProperties(properties);
        }
    }
}
