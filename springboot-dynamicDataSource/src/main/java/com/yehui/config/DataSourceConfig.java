package com.yehui.config;

import com.yehui.sys.DataSourceKey;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yehui
 * 数据源配置类
 */
@Configuration
public class DataSourceConfig {


    /**
     * 主数据
     *
     * @return data source
     */
    @Bean("master")
    @Primary
    @ConfigurationProperties(prefix = "spring.druid.datasource.master")
    public DataSource master() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 从数据库
     *
     * @return data source
     */
    @Bean("slave")
    @ConfigurationProperties(prefix ="spring.druid.datasource.slave")
    public DataSource slave() {
        return DataSourceBuilder.create().build();
    }
    /**
     * 从数据库
     *
     * @return data source
     */
    @Bean("migration")
    @ConfigurationProperties(prefix ="spring.druid.datasource.migration")
    public DataSource migration() {
        return DataSourceBuilder.create().build();
    }


    /**
     * 配置动态数据源
     *
     * @return
     */
    @Bean("dynamicDataSource")
    public DataSource dynamicDataSource() {
        DynamicDataSource dynamicRoutingDataSource = new DynamicDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>(4);
        dataSourceMap.put(DataSourceKey.master.name(), master());
        dataSourceMap.put(DataSourceKey.salve.name(), slave());
        dataSourceMap.put(DataSourceKey.master.name(), slave());

        //设置默认的数据源
        dynamicRoutingDataSource.setDefaultTargetDataSource(master());
        // 多个slave数据源在此添加，自定义key，用于轮询
        dataSourceMap.put(DataSourceKey.salve.name() + "1", slave());
        //设置目标数据源
        dynamicRoutingDataSource.setTargetDataSources(dataSourceMap);
        //将数据源的key放在集合中判断是否正常
        DynamicDataSourceContextHolder.slaveDataSourceKeys.addAll(dataSourceMap.keySet());

        //实现负载均衡算法   将 Slave 数据源的 key 放在集合中，用于轮循
        DynamicDataSourceContextHolder.slaveDataSourceKeys.addAll(dataSourceMap.keySet());
        DynamicDataSourceContextHolder.slaveDataSourceKeys.remove(DataSourceKey.migration.name());
        return dynamicRoutingDataSource;
    }

    /**
     * 设置工厂类
     */
    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean() {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dynamicDataSource());

        //此处设置为了解决找不到mapper文件的问题
        try {
            sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*Mapper.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sqlSessionFactoryBean;
    }

    /**
     * 事物管理器
     */
    @Bean("transactionManager")
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dynamicDataSource());
    }
}
