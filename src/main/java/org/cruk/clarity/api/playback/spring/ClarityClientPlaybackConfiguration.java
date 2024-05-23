package org.cruk.clarity.api.playback.spring;

import org.cruk.clarity.api.playback.ClarityAPIPlaybackAspect;
import org.cruk.clarity.api.search.ClarityClientSearchConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackageClasses = ClarityAPIPlaybackAspect.class)
public class ClarityClientPlaybackConfiguration extends ClarityClientSearchConfiguration
{
    public ClarityClientPlaybackConfiguration()
    {
    }
}
