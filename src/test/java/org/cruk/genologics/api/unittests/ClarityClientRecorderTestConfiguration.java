/*
 * CRUK-CI Genologics REST API Java Client.
 * Copyright (C) 2013 Cancer Research UK Cambridge Institute.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cruk.genologics.api.unittests;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.cruk.genologics.api.GenologicsAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({
    "classpath:/org/cruk/genologics/api/genologics-client-context.xml",
    "classpath:/unittest-context.xml"
})
public abstract class ClarityClientRecorderTestConfiguration
{
    @Autowired
    protected GenologicsAPI api;

    public ClarityClientRecorderTestConfiguration()
    {
    }

    @PostConstruct
    public void setCredentialsOnApi()
    {
        InputStream propsIn = getClass().getResourceAsStream("/testcredentials.properties");

        if (propsIn != null)
        {
            try
            {
                Properties credentials = new Properties();
                credentials.load(propsIn);
                api.setConfiguration(credentials);
            }
            catch (IOException e)
            {
                Logger logger = LoggerFactory.getLogger(getClass());
                logger.error("Could not read from credentials file: ", e);
            }
            finally
            {
                IOUtils.closeQuietly(propsIn);
            }
        }
    }
}
