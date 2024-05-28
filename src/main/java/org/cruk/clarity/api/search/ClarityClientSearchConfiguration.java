/*
 * CRUK-CI Clarity REST API Java Client.
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

package org.cruk.clarity.api.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cruk.clarity.api.ClarityAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import jakarta.xml.bind.Marshaller;

/**
 * Spring configuration for the Clarity Client with searches.
 */
@Configuration
public class ClarityClientSearchConfiguration
{
    /**
     * Constructor.
     */
    public ClarityClientSearchConfiguration()
    {
    }

    /**
     * Provide another Jaxb2Marshaller bean that's very similar to the main
     * "clarityJaxbMarshaller" but with the search package included.
     *
     * @return The Jaxb2Marshaller with searching.
     */
    @Bean
    public Jaxb2Marshaller claritySearchMarshaller()
    {
        Module module = ClarityAPI.class.getModule();
        List<String> packages = module.getPackages().stream()
                    .filter(p -> p.startsWith("com.genologics.ri"))
                    .collect(Collectors.toList());
        packages.add(Search.class.getPackage().getName());

        String[] packageArray = packages.toArray(new String[packages.size()]);

        Map<String, Object> marshallerProps = new HashMap<>();
        marshallerProps.put(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshallerProps.put(Marshaller.JAXB_ENCODING, "UTF-8");

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(packageArray);
        marshaller.setMarshallerProperties(marshallerProps);
        return marshaller;
    }
}
