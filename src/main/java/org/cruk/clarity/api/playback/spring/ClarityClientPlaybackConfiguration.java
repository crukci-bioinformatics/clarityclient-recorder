package org.cruk.clarity.api.playback.spring;

import org.cruk.clarity.api.xstream.XStreamFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan("org.cruk.clarity.api.playback")
public class ClarityClientPlaybackConfiguration
{
    public ClarityClientPlaybackConfiguration()
    {
    }

    @Bean
    public XStreamFactory claritySearchXStream()
    {
        return new XStreamFactory();
    }
}
