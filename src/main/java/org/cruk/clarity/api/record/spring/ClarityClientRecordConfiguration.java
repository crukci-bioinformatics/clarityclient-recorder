package org.cruk.clarity.api.record.spring;

import org.cruk.clarity.api.xstream.XStreamFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan("org.cruk.clarity.api.record")
public class ClarityClientRecordConfiguration
{
    public ClarityClientRecordConfiguration()
    {
    }

    @Bean
    public XStreamFactory claritySearchXStream()
    {
        return new XStreamFactory();
    }
}
