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
import java.text.MessageFormat;
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
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.genologics.ri.Batch;
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
     * Template for the file name pattern.
     */
    public static final String FILENAME_PATTERN = "{0}-{1}.xml";

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
     * A record of the type of Batch object to create with all the links when
     * intercepting list calls.
     *
     * @see #doList(ProceedingJoinPoint)
     */
    private ThreadLocal<Class<?>> listIntercept = new ThreadLocal<Class<?>>();


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

            File searchFile = new File(searchDirectory, Search.getSearchFileName(search.getSearchTerms()));

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
     * Join point around the {@code listAll} and {@code listSome} methods that recreate
     * the {@code Batch} object that holds the list of links to the real things and
     * writes that list to an XML file in the messages directory.
     *
     * <p>
     * Only one list is saved for each type, so if there are multiple calls to either
     * list method in the calling code, the final file will be the latest list. It is
     * envisaged that most of the time, each thing will be only listed once so this
     * shouldn't in practice be an issue.
     * </p>
     *
     * @param pjp The join point.
     * @return The result of the search (a list of links).
     *
     * @throws Throwable if there is an error invoking the underlying method.
     */
    @SuppressWarnings("unchecked")
    public List<LimsLink<?>> doList(ProceedingJoinPoint pjp) throws Throwable
    {
        List result;
        try
        {
            listIntercept.set(null);
            result = (List)pjp.proceed();

            try
            {
                Class batchClass = listIntercept.get();
                if (batchClass != null)
                {
                    Batch batch = (Batch)batchClass.newInstance();
                    batch.getList().addAll(result);
                    writeList(batch);
                }
            }
            catch (Exception e)
            {
                Class<?> what = (Class<?>)pjp.getArgs()[0];

                logger.warn("Could not record list of {}: {}", ClassUtils.getShortClassName(what), e.getMessage());
            }
        }
        finally
        {
            listIntercept.set(null);
        }

        return result;
    }

    /**
     * Join point around the REST template's {@code getForEntity} methods used by the
     * {@code GenologicsAPI.doList) method. When invoked with a class that implements
     * {@code Batch}, it creates a new empty object of the same type and stores it in
     * the {@code listIntercept} local. This is used by the join point around the
     * API's list methods to know what object to put the list of links returned from
     * the API call into, so that writing to the file will have a class that can be
     * marshalled to XML.
     *
     * @param pjp The join point.
     * @return The result of the REST call (a {@code ResponseEntity}.
     *
     * @throws Throwable if there is an error invoking the underlying method.
     *
     * @see #doList(ProceedingJoinPoint)
     */
    @SuppressWarnings("unchecked")
    public Object interceptGetForEntity(ProceedingJoinPoint pjp) throws Throwable
    {
        Object result = pjp.proceed();

        Class<?> entityClass = (Class<?>)pjp.getArgs()[1];

        if (Batch.class.isAssignableFrom(entityClass) && listIntercept.get() == null)
        {
            ResponseEntity<Batch> entity = (ResponseEntity<Batch>)result;
            listIntercept.set(entity.getBody().getClass());
        }

        return result;
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
                File file = getFileForEntity(thing);

                jaxbMarshaller.marshal(thing, new StreamResult(file));
            }
            catch (Exception e)
            {
                // Ignore.
            }
        }
    }

    /**
     * Convenience method to get the file the given entity would be written to.
     *
     * @param thing The entity to store.
     *
     * @return The file for this entity.
     */
    private File getFileForEntity(Object thing)
    {
        assert thing != null : "Cannot get a name for null";

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

        String name = MessageFormat.format(FILENAME_PATTERN, ClassUtils.getShortClassName(thing.getClass()), id);

        return new File(messageDirectory, name);
    }

    /**
     * Method that writes a list of links to a suitably named file.
     * If there is an error writing the batch object to the file, the file will not
     * be written and there is no logging of the error. It is quietly ignored.
     *
     * @param list The batch object to write. Quietly ignores {@code null}.
     */
    private void writeList(Batch<?> list)
    {
        if (list != null)
        {
            try
            {
                String name = ClassUtils.getShortClassName(list.getClass()) + ".xml";

                File file = new File(messageDirectory, name);

                jaxbMarshaller.marshal(list, new StreamResult(file));
            }
            catch (Exception e)
            {
                // Ignore.
            }
        }
    }
}
