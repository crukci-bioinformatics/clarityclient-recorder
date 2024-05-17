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

package org.cruk.clarity.api.xstream;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cruk.clarity.api.ClarityAPI;
import org.cruk.clarity.api.search.Search;
import org.cruk.clarity.api.search.SearchTerms;
import org.springframework.beans.factory.FactoryBean;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;


/**
 * A factory bean for XStream instances. These instances are set up to allow
 * deserialisation of the API classes and some core Java classes only.
 *
 * @since 2.27.2
 */
public class XStreamFactory implements FactoryBean<XStream>
{
    /**
     * List of packages whose classes are allowed to be deserialised by XStream.
     * These are read from the "packagelist.txt" file on the class path.
     */
    private final List<String> packageWildcards;

    /**
     * Create the factory. Read the API packages from the package list file on the
     * class path.
     */
    public XStreamFactory()
    {
        Module apiMod = ClarityAPI.class.getModule();
        List<String> packages = apiMod.getPackages().stream()
                .filter(n -> n.startsWith("com.genologics.ri"))
                .map(n -> n + ".*")
                .collect(Collectors.toList());

        packages.add(Search.class.getPackage().getName() + ".*");

        packageWildcards = Collections.unmodifiableList(packages);
    }

    /**
     * Create an instance of XStream set up for deserialising search objects.
     *
     * @return An XStream object configured for reading and writing searches.
     */
    @Override
    public XStream getObject()
    {
        String[] wildcards = packageWildcards.toArray(new String[packageWildcards.size()]);

        XStream xstream = new XStream();
        xstream.processAnnotations(Search.class);
        xstream.processAnnotations(SearchTerms.class);

        // clear out existing permissions and set own ones
        xstream.addPermission(NoTypePermission.NONE);

        // allow some basics
        xstream.addPermission(NullPermission.NULL);
        xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xstream.allowTypes(new Class<?>[] { String.class, URI.class, URL.class } );
        xstream.allowTypeHierarchy(Collection.class);
        xstream.allowTypeHierarchy(Map.class);

        // Allow types from the API classes and the search package.
        xstream.allowTypesByWildcard(wildcards);

        return xstream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<XStream> getObjectType()
    {
        return XStream.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return false, always.
     */
    @Override
    public boolean isSingleton()
    {
        return false;
    }
}
