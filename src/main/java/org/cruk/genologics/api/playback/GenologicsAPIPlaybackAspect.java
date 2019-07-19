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

package org.cruk.genologics.api.playback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ClassUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.cruk.genologics.api.GenologicsAPI;
import org.cruk.genologics.api.search.Search;
import org.cruk.genologics.api.search.SearchTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.genologics.ri.reagenttype.ReagentTypes;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

/**
 * Aspect for replaying server exchanges from a directory containing XML representations
 * of entities as would be returned from a real Clarity server.
 */
@Aspect
public class GenologicsAPIPlaybackAspect
{
    /**
     * ASCII character set.
     */
    private static final Charset ASCII = Charset.forName("US-ASCII");

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(GenologicsAPI.class);

    /**
     * The directory containing the prerecorded messages.
     */
    private File messageDirectory;

    /**
     * The directory containing prerecorded searches.
     */
    private File searchDirectory;

    /**
     * The JAXB marshaller used to directly unmarshal the XML files into objects.
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
    public GenologicsAPIPlaybackAspect()
    {
        this(new File("serverexchanges"));
    }

    /**
     * Constructor.
     */
    public GenologicsAPIPlaybackAspect(File messageDirectory)
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
     * Join point around the Spring REST client's {@code getForObject()} methods.
     * Looks for a file named with the required class's short name (no package) plus
     * either its LIMS id (if there is one) or the identifier given at the end of
     * the path of the URI.
     *
     * @param pjp The join point.
     * @return The unmarshalled entity from the file found.
     *
     * @throws FileNotFoundException if there is no file recorded for the object.
     *
     * @throws Throwable if there is anything else that fails.
     */
    public Object doGet(ProceedingJoinPoint pjp) throws Throwable
    {
        Object uriObj = pjp.getArgs()[0];
        Class<?> type = (Class<?>)pjp.getArgs()[1];

        File file;

        if (type.equals(ReagentTypes.class))
        {
            file = new File(messageDirectory, "ReagentTypes.xml");
        }
        else
        {
            URI uri;
            try
            {
                uri = (URI)uriObj;
            }
            catch (ClassCastException e)
            {
                uri = new URI(uriObj.toString());
            }

            String path = uri.getPath();

            String limsid = path.substring(path.lastIndexOf('/') + 1);

            String filename = ClassUtils.getShortClassName(type) + "-" + limsid + ".xml";
            file = new File(messageDirectory, filename);
        }

        if (!file.exists())
        {
            throw new FileNotFoundException("There is no file " + file.getName() + " recorded.");
        }

        Object thing = jaxbMarshaller.unmarshal(new StreamSource(file));

        return thing;
    }

    /**
     * Join point around the Spring REST client's {@code getForEntity()} methods.
     * Works as {@link #doGet(ProceedingJoinPoint)}, except rather than throwing an
     * error when the file does not exist, {@code null} is returned.
     *
     * @param pjp The join point.
     *
     * @return A ResponseEntity object containing the unmarshalled entity and an "OK"
     * code if the file exists, or one with no body and a "not found" status if it
     * does not.
     *
     * @throws Throwable if there is anything fails.
     */
    public ResponseEntity<?> doGetEntity(ProceedingJoinPoint pjp) throws Throwable
    {
        ResponseEntity<?> response;
        try
        {
            Object thing = doGet(pjp);

            response = new ResponseEntity<Object>(thing, HttpStatus.OK);
        }
        catch (FileNotFoundException e)
        {
            response = new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    /**
     * Join point around the Clarity client's {@code find()} method. Tries to find
     * a prerecorded search in the search directory that matches the search parameters
     * of this call.
     *
     * @param pjp The join point.
     * @return The result of the search (a list of links). Will return null if there
     * is no matching search recorded.
     */
    public List<?> doFind(ProceedingJoinPoint pjp)
    {
        @SuppressWarnings("unchecked")
        Map<String, ?> searchTerms = (Map<String, ?>)pjp.getArgs()[0];

        @SuppressWarnings("unchecked")
        Class<?> entityClass = (Class<?>)pjp.getArgs()[1];

        SearchTerms terms = new SearchTerms(searchTerms, entityClass);

        Search<?> search = loadSearch(terms);

        if (search == null)
        {
            logger.warn("There is no recorded search with the parameters given:\n{}", searchTerms);
            return null;
        }

        return search.getResults();
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
     * Load the prerecorded searches from the search directory. Finds files in there
     * that match the expected file name format and brings them all into memory.
     */
    private Search<?> loadSearch(SearchTerms terms)
    {
        File searchFile = new File(searchDirectory, Search.getSearchFileName(terms));

        try
        {
            Reader reader = new InputStreamReader(new FileInputStream(searchFile), ASCII);
            try
            {
                return (Search<?>)xstream.fromXML(reader);
            }
            catch (XStreamException xse)
            {
                if (xse.getCause() != null)
                {
                    try
                    {
                        throw xse.getCause();
                    }
                    catch (IOException e)
                    {
                        throw e;
                    }
                    catch (Throwable t)
                    {
                    }
                }
                throw xse;
            }
            finally
            {
                IOUtils.closeQuietly(reader);
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
}
