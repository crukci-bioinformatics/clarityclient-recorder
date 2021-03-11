package org.cruk.genologics.api.xstream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cruk.genologics.api.search.Search;
import org.cruk.genologics.api.search.SearchTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(XStreamFactory.class);

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
        final String packagesList = "/com/genologics/ri/packagelist.txt";

        List<String> packages = new ArrayList<String>();
        try (InputStream in = XStreamFactory.class.getResourceAsStream(packagesList))
        {
            if (in == null)
            {
                logger.error("There is no packages list on the classpath ({}).", packagesList);
            }
            else
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, "US-ASCII"));
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (isNotBlank(line))
                    {
                        packages.add(line.trim() + ".*");
                    }
                }
            }
        }
        catch (IOException e)
        {
            logger.error("Could not load API package names from packages list.");
        }

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
        xstream.allowTypes(new Class[] { String.class, URI.class, URL.class } );
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
