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

package org.cruk.clarity.api.record;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.cruk.clarity.api.ClarityAPI;
import org.cruk.clarity.api.impl.ClarityAPIInternal;
import org.cruk.clarity.api.playback.ClarityAPIPlaybackAspect;
import org.cruk.clarity.api.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.stereotype.Component;

import com.genologics.ri.Batch;
import com.genologics.ri.ClarityEntity;
import com.genologics.ri.LimsEntity;
import com.genologics.ri.LimsEntityLink;
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;

/**
 * Aspect for recording server exchanges with a real Clarity server as XML files
 * to a directory on disk.
 */
@Aspect
@Component("clarityRecordingAspect")
@SuppressWarnings("exports")
public class ClarityAPIRecordingAspect
{
    /**
     * Template for the file name pattern.
     */
    public static final String FILENAME_PATTERN = "{0}-{1}.xml";

    /**
     * Logger.
     */
    Logger logger = LoggerFactory.getLogger(ClarityAPI.class);

    /**
     * The directory to write the messages to.
     */
    private File messageDirectory;

    /**
     * Whether to record a search that returns no results.
     *
     * @since 2.31.6
     */
    private boolean recordSearchesWithoutResults = true;

    /**
     * The JAXB unmarshaller used to unmarshal previously recorded searches for merging.
     */
    private Unmarshaller unmarshaller;

    /**
     * The JAXB marshaller used to marshal object into XML for recording updates.
     */
    private Marshaller marshaller;

    /**
     * Access to the API, but through its internal interface.
     */
    private ClarityAPIInternal apiInternal;


    /**
     * Constructor.
     */
    public ClarityAPIRecordingAspect()
    {
        this(new File("serverexchanges"));
    }

    /**
     * Constructor.
     *
     * @param messageDirectory The message directory.
     */
    public ClarityAPIRecordingAspect(File messageDirectory)
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
     * Whether to record a search that returns no results. The playback aspect
     * can be set up to ignore missing search results and just return nothing
     * from the call rather than requiring the search file is present. This is
     * the other side of that: should we write a search file if the search
     * has returned nothing.
     *
     * @return Whether to record searches that brought back zero results or not.
     *
     * @see ClarityAPIPlaybackAspect#isFailOnMissingSearch()
     *
     * @since 2.31.6
     */
    public boolean isRecordSearchesWithoutResults()
    {
        return recordSearchesWithoutResults;
    }

    /**
     * Set whether to record a search with zero results or not.
     *
     * @param recordSearchesWithoutResults true to save a search regardless of
     * the results, false to not save when there are no results.
     *
     * @since 2.31.6
     */
    public void setRecordSearchesWithoutResults(boolean recordSearchesWithoutResults)
    {
        this.recordSearchesWithoutResults = recordSearchesWithoutResults;
    }

    /**
     * Inject the JAXB unmarshaller.
     *
     * @param unmarshaller The unmarshaller.
     */
    @Autowired
    @Qualifier("clarityJaxbUnmarshaller")
    public void setClarityUnmarshaller(Unmarshaller unmarshaller)
    {
        this.unmarshaller = unmarshaller;
    }

    /**
     * Inject the JAXB marshaller.
     *
     * @param marshaller The marshaller.
     */
    @Autowired
    @Qualifier("clarityJaxbMarshaller")
    public void setClarityMarshaller(Marshaller marshaller)
    {
        this.marshaller = marshaller;
    }

    /**
     * Set the internal interface access to the API.
     *
     * @param internalApi The API bean, but through its internal interface.
     */
    @Autowired
    @Qualifier("clarityAPI")
    public void setInternalClarityAPI(ClarityAPIInternal internalApi)
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
    @Around("(execution(public * retrieve(..)) or execution(public * load(..))) and bean(clarityAPI)")
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
    @Around("execution(public * loadAll(..)) and bean(clarityAPI)")
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
    @Around("execution(public * find(..)) and bean(clarityAPI)")
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

            if (recordSearchesWithoutResults || !results.isEmpty())
            {
                File searchFile = new File(messageDirectory, search.getSearchFileName());

                if (checkAndMergeWithExisting(search, searchFile))
                {
                    serialiseSearch(search, searchFile);
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
     * Write a search object to file with XStream.
     *
     * @param <E> The type of entity being searched for.
     *
     * @param search The current search object.
     * @param searchFile The file the search will be serialized to.
     *
     * @throws IOException if there is an error writing the file.
     */
    <E extends Locatable> void serialiseSearch(Search<E> search, File searchFile) throws IOException
    {
        try (Writer writer = new BufferedWriter(new FileWriter(searchFile, UTF_8)))
        {
            marshaller.marshal(search, new StreamResult(writer));
        }
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
    <E extends Locatable> boolean checkAndMergeWithExisting(Search<E> search, File searchFile)
    {
        if (!searchFile.exists())
        {
            return true;
        }

        try
        {
            try (Reader reader = new BufferedReader(new FileReader(searchFile, UTF_8)))
            {
                @SuppressWarnings("unchecked")
                Search<E> previousSearch = search.getClass().cast(unmarshaller.unmarshal(new StreamSource(reader)));

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

                return search.merge(previousSearch);
            }
            catch (XmlMappingException xse)
            {
                Throwable t = xse;
                while (t.getCause() != null)
                {
                    t = t.getCause();
                }
                logger.warn("Could not reload previous search: {}", t.getMessage());
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
    @Around("execution(public * list*(..)) and bean(clarityAPI)")
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

                marshaller.marshal(thing, new StreamResult(file));
            }
            catch (Exception e)
            {
                // Ignore.
            }
        }
    }

    /**
     * Shared method to get the LIMS id out of a URI, public to let the playback aspect
     * use it. Removes trailing sections on the URI from classes like Demux and StepDetails.
     *
     * @param clazz The type of entity to obtain the LIMS id for.
     * @param uri The URI in string form.
     *
     * @return The (single) LIMS id from the URI.
     */
    public static String limsIdFromUri(Class<?> clazz, String uri)
    {
        ClarityEntity anno = clazz.getAnnotation(ClarityEntity.class);

        int fromIndex = uri.length();
        if (anno != null && isNotEmpty(anno.uriSubsection()))
        {
            fromIndex -= anno.uriSubsection().length() + 1;
        }

        int lastSlash = uri.lastIndexOf('/', fromIndex - 1);
        return uri.substring(lastSlash + 1, fromIndex);
    }

    /**
     * Shared method to get the LIMS id from an object, which must at least implement
     * {@code Locatable}.
     *
     * @param thing The entity to get an id for.
     *
     * @return The LIMS id for the object.
     */
    public static String limsIdFromObject(Object thing)
    {
        assert thing != null : "Cannot get a name for null";

        String id = null;
        Class<?> entityType = thing.getClass();

        if (thing instanceof LimsEntity<?> entity)
        {
            id = entity.getLimsid();
        }
        else if (thing instanceof LimsEntityLink<?> link)
        {
            id = link.getLimsid();
            entityType = link.getEntityClass();
        }

        if (id == null)
        {
            Locatable item = (Locatable)thing;
            id = limsIdFromUri(entityType, item.getUri().getPath());
        }

        return id;
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
        String id = limsIdFromObject(thing);

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

                marshaller.marshal(list, new StreamResult(file));
            }
            catch (Exception e)
            {
                // Ignore.
            }
        }
    }
}
