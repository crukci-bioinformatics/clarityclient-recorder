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

import org.cruk.clarity.api.spring.ClarityClientConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Clarity Client with searches.
 */
@Configuration
public class ClarityClientSearchConfiguration extends ClarityClientConfiguration
{
    /**
     * Constructor. Provide the additional package containing the search classes
     * to the superclass and thus Jaxb context.
     */
    public ClarityClientSearchConfiguration()
    {
        super(Search.class);
    }
}
