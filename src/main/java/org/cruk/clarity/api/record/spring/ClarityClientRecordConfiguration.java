package org.cruk.clarity.api.record.spring;

import org.cruk.clarity.api.record.ClarityAPIRecordingAspect;
import org.cruk.clarity.api.search.ClarityClientSearchConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackageClasses = ClarityAPIRecordingAspect.class)
public class ClarityClientRecordConfiguration extends ClarityClientSearchConfiguration
{
    public ClarityClientRecordConfiguration()
    {
    }
}
