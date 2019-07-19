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

package org.cruk.genologics.api.record;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.cruk.genologics.api.GenologicsAPI;
import org.cruk.genologics.api.search.Search;
import org.cruk.genologics.api.search.SearchTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.genologics.ri.LimsEntity;
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.XStream;

/**
 * Aspect for recording server exchanges with a real Clarity server as XML files
 * to a directory on disk.
 */
@Aspect
public class GenologicsAPIRecordingAspect
{
    /**
     * ASCII character set.
     */
    private static final Charset ASCII = Charset.forName("US-ASCII");

    /**
     * End of line in byte form.
     */
    private static final String EOL = System.getProperty("line.separator", "\n");

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(GenologicsAPI.class);

    /**
     * The directory to write the messages to.
     */
    private File messageDirectory;

    /**
     * The directory to write searches to.
     */
    private File searchDirectory;

    /**
     * The JAXB marshaller used to directly marshal the API entities into XML files.
     */
    private Jaxb2Marshaller jaxbMarshaller;

    /**
     * XStream XML serialiser.
     */
    private XStream xstream;


    /**
     * Initialiser. Set up XStream.
     */
    {
        xstream = new XStream();
        xstream.processAnnotations(Search.class);
        xstream.processAnnotations(SearchTerms.class);
    }

    /**
     * Constructor.
     */
    public GenologicsAPIRecordingAspect()
    {
        this(new File("serverexchanges"));
    }

    /**
     * Constructor.
     */
    public GenologicsAPIRecordingAspect(File messageDirectory)
    {
        setMessageDirectory(messageDirectory);
    }

    /**
     * Get the directory the messages are being written to.
     *
     * @return The message directory.
     */
    public File getMessageDirectory()
    {
        return messageDirectory;
    }

    /**
     * Set the directory the messages are being written to.
     * Also sets the search directory if it is not already set.
     *
     * @param messageDirectory The message directory.
     */
    public void setMessageDirectory(File messageDirectory)
    {
        if (searchDirectory == null || searchDirectory.getParentFile().equals(this.messageDirectory))
        {
            setSearchDirectory(new File(messageDirectory, Search.DEFAULT_SEARCH_DIRECTORY));
        }
        this.messageDirectory = messageDirectory;
    }

    /**
     * Get the directory searches are being written to.
     *
     * @return The search directory.
     */
    public File getSearchDirectory()
    {
        return searchDirectory;
    }

    /**
     * Set the directory the messages are being written to.
     *
     * @param searchDirectory The search directory.
     */
    public void setSearchDirectory(File searchDirectory)
    {
        this.searchDirectory = searchDirectory;
    }

    /**
     * Inject the JAXB marshaller. This is required.
     *
     * @param jaxbMarshaller The marshaller.
     */
    @Required
    public void setJaxbMarshaller(Jaxb2Marshaller jaxbMarshaller)
    {
        this.jaxbMarshaller = jaxbMarshaller;
    }

    /**
     * Join point around the Clarity client's {@code load()} and {@code retrieve()} methods.
     * Simply marshalls the object that has come back from the Clarity server to a file
     * named with the required class's short name (no package) plus
     * either its LIMS id (if there is one) or the identifier given at the end of
     * the path of the URI.
     *
     * @param pjp The join point.
     * @return The entity returned from the server.
     *
     * @throws Throwable if there is anything fails.
     */
    public Object doLoad(ProceedingJoinPoint pjp) throws Throwable
    {
        Object thing = pjp.proceed();

        writeEntity(thing);

        return thing;
    }

    /**
     * Join point around the Clarity client's {@code loadAll()} method.
     * Marshals all the objects returned from the server to files on disk, as per
     * {@code doLoad()}.
     *
     * @param pjp The join point.
     * @return The entities returned from the server.
     *
     * @throws Throwable if there is anything fails.
     *
     * @see #doLoad(ProceedingJoinPoint)
     */
    public Object doLoadAll(ProceedingJoinPoint pjp) throws Throwable
    {
        Collection<?> list = (Collection<?>)pjp.proceed();

        for (Object thing : list)
        {
            writeEntity(thing);
        }

        return list;
    }

    /**
     * Join point around the Clarity client's {@code find()} method. Runs the search
     * and records the search terms and results in a file in the search directory.
     *
     * @param <E> The type of entity being searched for.
     *
     * @param pjp The join point.
     * @return The result of the search (a list of links).
     *
     * @throws Throwable if there is an error invoking the underlying method.
     */
    public <E extends Locatable> List<LimsLink<E>> doFind(ProceedingJoinPoint pjp) throws Throwable
    {
        @SuppressWarnings("unchecked")
        Map<String, ?> searchTerms = (Map<String, ?>)pjp.getArgs()[0];

        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>)pjp.getArgs()[1];

        Object reply = pjp.proceed();

        @SuppressWarnings("unchecked")
        List<LimsLink<E>> results = (List<LimsLink<E>>)reply;

        try
        {
            Search<E> search = new Search<E>(searchTerms, entityClass);
            search.setResults(results);

            // Create the search directory if it's not there and is under the messages directory.
            if (!searchDirectory.exists() && searchDirectory.getParentFile().equals(messageDirectory))
            {
                if (!searchDirectory.mkdir())
                {
                    throw new IOException("Cannot create search directory " + searchDirectory.getAbsolutePath());
                }
            }

            File searchFile = new File(searchDirectory, Integer.toHexString(search.getSearchTerms().hashCode()) + ".xml");

            Writer out = new FileWriterWithEncoding(searchFile, ASCII, true);
            try
            {
                xstream.toXML(search, out);

                // Doesn't write a final end of line.
                out.write(EOL);
            }
            finally
            {
                IOUtils.closeQuietly(out);
            }
        }
        catch (IOException e)
        {
            logger.warn("Could not record search: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Method that writes the given entity to a suitably named file.
     * If there is an error writing the entity to the file, the file will not
     * be written and there is no logging of the error. It is quietly ignored.
     *
     * @param thing The entity to write. Quietly ignores {@code null}.
     */
    private void writeEntity(Object thing)
    {
        if (thing != null)
        {
            try
            {
                String id;
                if (thing instanceof LimsEntity<?>)
                {
                    id = ((LimsEntity<?>)thing).getLimsid();
                }
                else
                {
                    id = ((Locatable)thing).getUri().toString();
                    int lastSlash = id.lastIndexOf('/');
                    id = id.substring(lastSlash + 1);
                }

                String fileName = ClassUtils.getShortClassName(thing.getClass()) + "-" + id + ".xml";
                File file = new File(messageDirectory, fileName);

                jaxbMarshaller.marshal(thing, new StreamResult(file));
            }
            catch (Exception e)
            {
                // Ignore.
            }
        }
    }
}
