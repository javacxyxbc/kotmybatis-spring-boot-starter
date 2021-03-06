package kot.bootstarter.kotmybatis.config;


import kot.bootstarter.kotmybatis.common.CT;
import kot.bootstarter.kotmybatis.common.id.IdGenerator;
import kot.bootstarter.kotmybatis.common.id.IdGeneratorBySnowflakeImpl;
import kot.bootstarter.kotmybatis.common.id.IdGeneratorByUUIDImpl;
import kot.bootstarter.kotmybatis.common.id.IdGeneratorFactory;
import kot.bootstarter.kotmybatis.plugin.KeyPropertiesPlugin;
import kot.bootstarter.kotmybatis.plugin.MapResultToCamelPlugin;
import kot.bootstarter.kotmybatis.properties.KotMybatisProperties;
import kot.bootstarter.kotmybatis.utils.SpringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;


/**
 * @author YangYu
 */
@Configuration
@ConditionalOnBean(SqlSessionFactory.class)
@EnableConfigurationProperties(KotMybatisProperties.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class KotMybatisAutoConfiguration implements ApplicationContextAware {

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @Autowired
    private KotMybatisProperties kotMybatisProperties;

    @Bean
    @ConditionalOnMissingBean(IdGeneratorFactory.class)
    public IdGeneratorFactory idGeneratorFactory() {
        return new IdGeneratorFactory();
    }

    @Bean("idGeneratorBySnowflake")
    @ConditionalOnMissingBean(IdGeneratorBySnowflakeImpl.class)
    public IdGenerator idGeneratorBySnowflake() {
        return new IdGeneratorBySnowflakeImpl(kotMybatisProperties.getWorkId(), kotMybatisProperties.getDataCenterId());
    }

    @Bean("idGeneratorByUUID")
    @ConditionalOnMissingBean(IdGeneratorByUUIDImpl.class)
    public IdGenerator idGeneratorByUUIDImpl() {
        return new IdGeneratorByUUIDImpl();
    }

    @PostConstruct
    public void kotInterceptor() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            //插件拦截链采用了责任链模式，执行顺序和加入连接链的顺序有关
            Properties mrtcpProperties = new Properties();
            mrtcpProperties.setProperty(CT.UNDER_SORE_TO_CAMEL, String.valueOf(kotMybatisProperties.isMapResultUnderSoreToCamel()));
            sqlSessionFactory.getConfiguration().addInterceptor(new MapResultToCamelPlugin(mrtcpProperties));
            sqlSessionFactory.getConfiguration().addInterceptor(new KeyPropertiesPlugin());
            configurationBuilder(sqlSessionFactory);
        }
    }

    /**
     * 构建 Configuration
     */
    private void configurationBuilder(SqlSessionFactory sqlSessionFactory) {
        sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(kotMybatisProperties.isEntityResultUnderSoreToCamel());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.set(applicationContext);
    }
}
