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

package org.cruk.clarity.api.playback;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.cruk.clarity.api.record.ClarityAPIRecordingAspect.FILENAME_PATTERN;
import static org.cruk.clarity.api.record.ClarityAPIRecordingAspect.limsIdFromObject;
import static org.cruk.clarity.api.record.ClarityAPIRecordingAspect.limsIdFromUri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.cruk.clarity.api.ClarityAPI;
import org.cruk.clarity.api.ClarityException;
import org.cruk.clarity.api.InvalidURIException;
import org.cruk.clarity.api.impl.ClarityAPIInternal;
import org.cruk.clarity.api.search.Search;
import org.cruk.clarity.api.search.SearchTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.genologics.ri.Batch;
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

/**
 * Aspect for replaying server exchanges from a directory containing XML representations
 * of entities as would be returned from a real Clarity server.
 */
@Aspect
public class ClarityAPIPlaybackAspect
{
    /**
     * Formatter for the version of entities saved.
     */
    private static final NumberFormat VERSION_FORMAT = NumberFormat.getIntegerInstance();

    /**
     * Template for the file name pattern for updated entities.
     */
    private static final String UPDATE_FILENAME_PATTERN = "{0}-{1}.{2}.xml";

    /**
     * An object to synchronized on while finding a version of a file to use.
     */
    private static final Object FILE_ALLOCATION_LOCK = new Object();

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(ClarityAPI.class);

    /**
     * The directory containing the prerecorded messages.
     */
    private File messageDirectory;

    /**
     * The directory to write updated entities to.
     */
    private File updatesDirectory;

    /**
     * The JAXB marshaller used to directly unmarshal the XML files into objects.
     */
    private Jaxb2Marshaller jaxbMarshaller;

    /**
     * Access to the API through its public interface.
     */
    private ClarityAPI api;

    /**
     * Access to the API, but through its internal interface.
     */
    private ClarityAPIInternal apiInternal;

    /**
     * XStream XML serialiser.
     */
    @Autowired
    @Qualifier("claritySearchXStream")
    private XStream xstream;


    /**
     * Static initialiser. Set up VERSION_FORMAT.
     */
    static
    {
        VERSION_FORMAT.setGroupingUsed(false);
        VERSION_FORMAT.setMinimumIntegerDigits(3);
    }

    /**
     * Constructor.
     */
    public ClarityAPIPlaybackAspect()
    {
        this(new File("serverexchanges"));
    }

    /**
     * Constructor.
     *
     * @param messageDirectory The message directory.
     */
    public ClarityAPIPlaybackAspect(File messageDirectory)
    {
        setMessageDirectory(messageDirectory);
    }

    /**
     * Get the directory the messages are being read from.
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
     * Get the directory the updated entities are being written to.
     *
     * @return The updates directory.
     */
    public File getUpdatesDirectory()
    {
        return updatesDirectory;
    }

    /**
     * Set the directory the updated entities are being written to.
     * If this is set to null, no updates will be written. By default
     * this is the case: one needs to set the directory to write them.
     *
     * @param updatesDirectory The updates directory.
     */
    public void setUpdatesDirectory(File updatesDirectory)
    {
        this.updatesDirectory = updatesDirectory;
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
     * Set the public interface access to the API.
     *
     * @param api The API bean, through its public interface.
     */
    @Autowired
    @Qualifier("clarityAPI")
    public void setClarityAPI(ClarityAPI api)
    {
        this.api = api;
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
     * Join point around the Spring REST client's {@code getForObject()} methods.
     * Looks for a file named with the required class's short name (no package) plus
     * either its LIMS id (if there is one) or the identifier given at the end of
     * the path of the URI.
     *
     * @param pjp The join point.
     * @return The unmarshalled entity from the file found.
     *
     * @throws NoRecordingException if there is no file recorded for the object.
     *
     * @throws Throwable if there is anything else that fails.
     */
    public Object doGet(ProceedingJoinPoint pjp) throws Throwable
    {
        Object uriObj = pjp.getArgs()[0];
        Class<?> type = (Class<?>)pjp.getArgs()[1];

        File file = getFileForEntity(type, uriObj);

        if (!file.exists())
        {
            throw new NoRecordingException("There is no file " + file.getName() + " recorded.");
        }

        Object thing = jaxbMarshaller.unmarshal(new StreamSource(file));

        return thing;
    }

    /**
     * Join point around the Spring REST client's {@code getForEntity()} methods.
     * Works as {@link #doGet(ProceedingJoinPoint)}, except rather than throwing a
     * {@code NoRecordingException}, it throws a {@code ClarityException} with a
     * not found status. This is what would happen if the call was made to a real
     * API.
     *
     * @param pjp The join point.
     *
     * @return A ResponseEntity object containing the unmarshalled entity and an "OK"
     * code if the file exists.
     *
     * @throws ClarityException with a "not found" status if there is no recorded
     * file for the entity.
     *
     * @throws Throwable if there is anything that fails.
     */
    public ResponseEntity<?> doGetEntity(ProceedingJoinPoint pjp) throws Throwable
    {
        ResponseEntity<?> response;
        try
        {
            Object thing = doGet(pjp);

            response = new ResponseEntity<Object>(thing, HttpStatus.OK);
        }
        catch (NoRecordingException e)
        {
            com.genologics.ri.exception.Exception ce = new com.genologics.ri.exception.Exception();
            ce.setCode(HttpStatus.NOT_FOUND.name());
            ce.setMessage(e.getMessage());
            throw new ClarityException(ce, HttpStatus.NOT_FOUND);
        }

        return response;
    }

    /**
     * Join point around calls to the API's {@code loadAll} method. Iterates
     * through the links asking the API for them individually, before returning
     * a collection of the entities.
     *
     * <p>
     * This slightly convoluted route allows the cache to return cache hits
     * if it is in use. The actual loading, when there is a cache miss, comes
     * from the call coming through this object again but via the
     * {@link #doGet(ProceedingJoinPoint)} method, which loads the entity from
     * file.
     * </p>
     *
     * @param pjp The join point.
     *
     * @return A list of the entities loaded from the links.
     *
     * @throws NoRecordingException if there is no entity file recorded for any of the links.
     *
     * @throws Throwable if there is anything else that fails.
     */
    public List<?> doLoadAll(ProceedingJoinPoint pjp) throws Throwable
    {
        Collection<?> links = (Collection<?>)pjp.getArgs()[0];
        List<Object> replies = new ArrayList<Object>(links.size());
        Iterator<?> iter = links.iterator();
        while (iter.hasNext())
        {
            LimsLink<?> link = (LimsLink<?>)iter.next();

            // Call through to the API again for this link only.
            // This will come through this class again, but through the doGet interceptors.
            // It will also go through the cache too.
            replies.add(api.load(link));
        }
        return replies;
    }

    /**
     * Join point around the Clarity client's {@code find()} method. Tries to find
     * a prerecorded search in the search directory that matches the search parameters
     * of this call.
     *
     * @param pjp The join point.
     *
     * @return The result of the search (a list of links).
     *
     * @throws NoRecordingException if there is no search recorded for the parameters
     * given.
     *
     * @throws Throwable if there is anything else that fails.
     */
    public List<?> doFind(ProceedingJoinPoint pjp) throws Throwable
    {
        @SuppressWarnings("unchecked")
        Map<String, ?> searchTerms = (Map<String, ?>)pjp.getArgs()[0];

        @SuppressWarnings("unchecked")
        Class<?> entityClass = (Class<?>)pjp.getArgs()[1];

        @SuppressWarnings({"unchecked", "rawtypes"})
        SearchTerms<?> terms = new SearchTerms(searchTerms, entityClass);

        Search<?> search = loadSearch(terms);

        if (search == null)
        {
            throw new NoRecordingException("There is no recorded search with the parameters given:\n" + searchTerms);
        }

        return search.getResults();
    }

    /**
     * Join point around the {@code listAll} and {@code listSome} methods load the list
     * of links from a serialised {@code Batch} object in XML file in the messages directory.
     *
     * <p>
     * No reference is made to the numbers in the {@code listSome} parameters. All the links
     * stored in the file are returned regardless.
     * </p>
     *
     * @param <E> The type of entity to list.
     * @param <L> The type of link to the entity returned from the API.
     * @param <BH> The batch class that is used to hold the links returned from the API.
     *
     * @param pjp The join point.
     * @return The result of the search (a list of links). If there was no file recorded
     * for a list of these entities, an empty list is returned.
     *
     * @throws NoRecordingException if there is no list recorded for this type of entity.
     *
     * @throws Throwable if there is an error invoking the underlying method.
     */
    public <E extends Locatable, L extends LimsLink<E>, BH extends Batch<L>>
    List<L> doList(ProceedingJoinPoint pjp) throws Throwable
    {
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>)pjp.getArgs()[0];

        Class<BH> batchClass = apiInternal.getQueryResultsClassForEntity(entityClass);

        List<L> list = Collections.emptyList();

        if (batchClass == null)
        {
            logger.warn("{} is not returned by any known Batch class.", entityClass.getName());
        }
        else
        {
            String listFileName = ClassUtils.getShortClassName(batchClass) + ".xml";
            File listFile = new File(messageDirectory, listFileName);

            if (listFile.exists())
            {
                @SuppressWarnings("unchecked")
                BH batch = (BH)jaxbMarshaller.unmarshal(new StreamSource(listFile));
                list = batch.getList();
            }
            else
            {
                throw new NoRecordingException("There is no list file " + listFile.getName() + " recorded.");
            }
        }

        return list;
    }

    /**
     * Join point around methods that would cause a change in Clarity
     * (create, update, delete, upload). These methods are not helpful when running
     * from prerecorded messages, and should be quietly ignored (a warning is logged).
     *
     * @param pjp The join point.
     */
    public void blockWrite(ProceedingJoinPoint pjp)
    {
        logger.warn("Call to {} blocked.", pjp.getSignature().getName());
    }

    /**
     * Join point around the Clarity client's {@code update()} method.
     * Writes to the message directory this new version of the entity. There will
     * be versions of the entity written to the directory, incrementing with each
     * call.
     *
     * @param pjp The join point.
     *
     * @throws Throwable if there is anything fails.
     */
    public void doUpdate(ProceedingJoinPoint pjp) throws Throwable
    {
        if (updatesDirectory != null)
        {
            Object entity = pjp.getArgs()[0];

            writeEntity(entity);
        }
        else
        {
            blockWrite(pjp);
        }
    }

    /**
     * Join point around the Clarity client's {@code updateAll()} method.
     * Writes to the message directory the new versions of the entities. There will
     * be versions of each entity written to the directory, incrementing with each
     * call (numbers per entity, not an overall counter).
     *
     * @param pjp The join point.
     *
     * @throws Throwable if there is anything fails.
     */
    public void doUpdateAll(ProceedingJoinPoint pjp) throws Throwable
    {
        if (updatesDirectory != null)
        {
            Collection<?> list = (Collection<?>)pjp.getArgs()[0];

            for (Object thing : list)
            {
                writeEntity(thing);
            }
        }
        else
        {
            blockWrite(pjp);
        }
    }

    /**
     * Load the prerecorded searches from the search directory. Finds files in there
     * that match the expected file name format and brings them all into memory.
     */
    private Search<?> loadSearch(SearchTerms<?> terms)
    {
        File searchFile = new File(messageDirectory, Search.getSearchFileName(terms));

        try
        {
            try (Reader reader = new InputStreamReader(new FileInputStream(searchFile), US_ASCII))
            {
                return (Search<?>)xstream.fromXML(reader);
            }
            catch (XStreamException xse)
            {
                Throwable t = xse;
                while (t.getCause() != null)
                {
                    t = t.getCause();
                }
                try
                {
                    throw t;
                }
                catch (IOException e)
                {
                    throw e;
                }
                catch (Throwable t2)
                {
                    throw xse;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            logger.debug("Search file {} does not exist.", searchFile.getName());
        }
        catch (IOException e)
        {
            logger.warn("Cannot read from {}.", searchFile.getAbsolutePath());
        }

        return null;
    }


    /**
     * Convenience method to get the file the target of a URI would be stored in.
     *
     * @param type The class of the thing to retrieve.
     * @param uriObj The untyped URI to the object.
     *
     * @return The file for the given entity.
     *
     * @throws InvalidURIException if the string value of {@code uriObj} cannot form
     * a valid URI.
     */
    private File getFileForEntity(Class<?> type, Object uriObj)
    {
        assert uriObj != null : "Cannot get a name for null";

        URI uri;
        try
        {
            uri = (URI)uriObj;
        }
        catch (ClassCastException e)
        {
            try
            {
                uri = new URI(uriObj.toString());
            }
            catch (URISyntaxException e2)
            {
                throw new InvalidURIException(e2);
            }
        }

        String limsid = limsIdFromUri(type, uri.getPath());

        String name = MessageFormat.format(FILENAME_PATTERN, ClassUtils.getShortClassName(type), limsid);

        return new File(messageDirectory, name);
    }

    /**
     * Method that writes the given updated entity to a suitably named file.
     * If the update message directory does not exist, an error will be logged
     * but otherwise errors are ignored. If the updates directory is not set,
     * nothing will be written.
     *
     * @param thing The entity to write. Quietly ignores {@code null}.
     */
    private void writeEntity(Object thing)
    {
        if (thing != null && updatesDirectory != null)
        {
            if (updatesDirectory.exists())
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
            else
            {
                logger.error("Update message directory {} does not exist. Cannot write updated entity.", messageDirectory.getAbsolutePath());
            }
        }
    }

    /**
     * Convenience method to get the name of the file the updated entity would be written to.
     *
     * @param thing The entity to store.
     *
     * @return The file for this entity, including a version.
     *
     * @throws IOException if the file allocated for this entity cannot be created (reserved).
     */
    private File getFileForEntity(Object thing) throws IOException
    {
        String id = limsIdFromObject(thing);

        // Want to save each version that is updated.

        File file;
        synchronized (FILE_ALLOCATION_LOCK)
        {
            int counter = 0;
            do
            {
                String fileName = MessageFormat.format(UPDATE_FILENAME_PATTERN,
                                            ClassUtils.getShortClassName(thing.getClass()),
                                            id, VERSION_FORMAT.format(counter++));
                file = new File(updatesDirectory, fileName);
            }
            while (file.exists());

            file.createNewFile();
        }

        return file;
    }
}
