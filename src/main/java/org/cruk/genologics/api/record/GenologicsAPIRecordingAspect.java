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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
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
import org.cruk.genologics.api.impl.GenologicsAPIInternal;
import org.cruk.genologics.api.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.genologics.ri.Batch;
import com.genologics.ri.LimsEntity;
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

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
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

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
     * The JAXB marshaller used to directly marshal the API entities into XML files.
     */
    private Jaxb2Marshaller jaxbMarshaller;

    /**
     * Access to the API, but through its internal interface.
     */
    private GenologicsAPIInternal apiInternal;

    /**
     * XStream XML serialiser.
     */
    @Autowired
    @Qualifier("searchXStream")
    private XStream xstream;


    /**
     * Constructor.
     */
    public GenologicsAPIRecordingAspect()
    {
        this(new File("serverexchanges"));
    }

    /**
     * Constructor.
     *
     * @param messageDirectory The message directory.
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
        this.messageDirectory = messageDirectory;
    }

    /**
     * Inject the JAXB marshaller. This is required.
     *
     * @param jaxbMarshaller The marshaller.
     */
    @Autowired
    @Qualifier("genologicsJaxbMarshaller")
    public void setJaxbMarshaller(Jaxb2Marshaller jaxbMarshaller)
    {
        this.jaxbMarshaller = jaxbMarshaller;
    }

    /**
     * Set the internal interface access to the API.
     *
     * @param internalApi The API bean, but through its internal interface.
     */
    @Autowired
    @Qualifier("genologicsAPI")
    public void setInternalGenologicsAPI(GenologicsAPIInternal internalApi)
    {
        this.apiInternal = internalApi;
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

            File searchFile = new File(messageDirectory, search.getSearchFileName());

            if (checkAndMergeWithExisting(search, searchFile))
            {
                Writer out = new FileWriterWithEncoding(searchFile, US_ASCII, false);
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
        }
        catch (IOException e)
        {
            logger.warn("Could not record search: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Checks whether a search results file already exists and, if so, whether it contains the
     * same search as that given. If the search terms are the same, merge the results found by
     * this search and that already recorded. If they don't match, log a warning and replace
     * the search with this one.
     *
     * @param <E> The type of entity being searched for.
     *
     * @param search The current search object.
     * @param searchFile The file the search will be serialized to.
     *
     * @return true if the search result file needs to be rewritten, false if the previous
     * search has resulted in the same links (so no need to rewrite the file).
     */
    private <E extends Locatable> boolean checkAndMergeWithExisting(Search<E> search, File searchFile)
    {
        if (!searchFile.exists())
        {
            return true;
        }

        try
        {
            Reader reader = new InputStreamReader(new FileInputStream(searchFile), US_ASCII);
            try
            {
                Search<?> previousSearch = (Search<?>)xstream.fromXML(reader);

                if (!previousSearch.getSearchTerms().equals(search.getSearchTerms()))
                {
                    logger.error("Have two incompatible searches that reduce to the same hash:");
                    logger.error(search.getSearchTerms().toString());
                    logger.error(previousSearch.getSearchTerms().toString());
                    return true;
                }

                // Merge search results. The search terms can only be equal if the
                // class of entity searched for is the same in both searches, so the
                // test above ensures they are for the same class.

                @SuppressWarnings("unchecked")
                Search<E> previousTypedSearch = (Search<E>)previousSearch;

                return search.merge(previousTypedSearch);
            }
            catch (XStreamException xse)
            {
                Throwable t = xse;
                while (t.getCause() != null)
                {
                    t = t.getCause();
                }
                logger.warn("Could not reload previous search: {}", t.getMessage());
            }
            finally
            {
                IOUtils.closeQuietly(reader);
            }
        }
        catch (IOException e)
        {
            logger.warn("Could not reload previous search: {}", e.getMessage());
        }

        return true;
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
     * @param <E> The type of entity to list.
     * @param <L> The type of link to the entity returned from the API.
     * @param <BH> The batch class that is used to hold the links returned from the API.
     *
     * @param pjp The join point.
     * @return The result of the search (a list of links).
     *
     * @throws Throwable if there is an error invoking the underlying method.
     */
    public <E extends Locatable, L extends LimsLink<E>, BH extends Batch<L>>
    List<L> doList(ProceedingJoinPoint pjp) throws Throwable
    {
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>)pjp.getArgs()[0];

        @SuppressWarnings("unchecked")
        List<L> links = (List<L>)pjp.proceed();

        try
        {
            Class<BH> batchClass = apiInternal.getQueryResultsClassForEntity(entityClass);

            if (batchClass == null)
            {
                logger.warn("{} is not returned by any known Batch class.", entityClass.getName());
            }
            else
            {
                Constructor<BH> batchConstructor = batchClass.getConstructor();
                BH batch = batchConstructor.newInstance();
                batch.getList().addAll(links);
                writeList(batch);
            }
        }
        catch (Exception e)
        {
            logger.warn("Could not record list of {}: {}", ClassUtils.getShortClassName(entityClass), e.getMessage());
        }

        return links;
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
