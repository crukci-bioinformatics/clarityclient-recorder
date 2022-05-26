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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.cruk.clarity.api.ClarityAPI;
import org.cruk.clarity.api.http.AuthenticatingClientHttpRequestFactory;
import org.cruk.clarity.api.search.Search;
import org.cruk.clarity.api.search.SearchTerms;
import org.cruk.clarity.api.unittests.CRUKCICheck;
import org.cruk.clarity.api.unittests.ClarityClientRecorderRecordTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.ResourceAccessException;

import com.genologics.ri.Batch;
import com.genologics.ri.LimsEntity;
import com.genologics.ri.LimsEntityLinkable;
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.container.Container;
import com.genologics.ri.containertype.ContainerType;
import com.genologics.ri.lab.Lab;
import com.genologics.ri.permission.Permission;
import com.genologics.ri.process.ClarityProcess;
import com.genologics.ri.project.Project;
import com.genologics.ri.reagenttype.ReagentType;
import com.genologics.ri.researcher.Researcher;
import com.genologics.ri.role.Role;
import com.genologics.ri.sample.Sample;

@SpringJUnitConfig(classes = ClarityClientRecorderRecordTestConfiguration.class)
public class ClarityAPIRecordingAspectTest
{
    @Autowired
    private ClarityAPI api;

    @Autowired
    private Jaxb2Marshaller marshaller;

    @Autowired
    private ClarityAPIRecordingAspect aspect;

    @Autowired
    @Qualifier("clarityClientHttpRequestFactory")
    protected AuthenticatingClientHttpRequestFactory httpRequestFactory;

    private File messageDirectory = new File("target/messages");

    public ClarityAPIRecordingAspectTest()
    {
    }

    @PostConstruct
    public void completeWiring()
    {
        aspect.setMessageDirectory(messageDirectory);
    }

    @BeforeEach
    public void setup() throws IOException
    {
        FileUtils.deleteQuietly(messageDirectory);
        FileUtils.forceMkdir(messageDirectory);
    }

    @AfterEach
    public void cleanup()
    {
        FileUtils.deleteQuietly(messageDirectory);
    }

    private void checkCredentialsFileExists()
    {
        assumeTrue(httpRequestFactory.getCredentials() != null,
                   "Could not set credentials for the API, which is needed for this test. " +
                   "Put a \"testcredentials.properties\" file on the class path.");
    }

    @Test
    public void testRecording()
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        try
        {
            Container c = api.load("27-340091", Container.class);
            assertRecorded(c);

            ContainerType ct = api.load(c.getContainerType());
            assertRecorded(ct);

            Collections.sort(c.getPlacements());

            Artifact pool = api.load(c.getPlacements().get(4));
            assertEquals("2-5898189", pool.getLimsid(), "Mismatched ids");
            assertRecorded(pool);

            Sample s = api.load("GAO9862A146", Sample.class);
            assertRecorded(s);

            Project p = api.load(s.getProject());
            assertRecorded(p);

            Researcher r = api.load(p.getResearcher());
            assertRecorded(r);

            Lab l = api.load(r.getLab());
            assertRecorded(l);

            ReagentType rg = api.load("374", ReagentType.class);
            assertRecorded(rg);

            Role role = api.load(r.getCredentials().getRoles().get(0));
            assertRecorded(role);

            Permission perm = api.load("5", Permission.class);
            assertRecorded(perm);
        }
        catch (ResourceAccessException e)
        {
            realServerDown(e);
        }
    }

    @Test
    public void testRecordList()
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        try
        {
            List<LimsLink<ContainerType>> ctLinks = api.listAll(ContainerType.class);

            File containerTypesFile = new File(messageDirectory, "ContainerTypes.xml");
            assertTrue(containerTypesFile.exists(), "Container types not recorded.");

            @SuppressWarnings("unchecked")
            Batch<? extends LimsLink<ContainerType>> ctBatch =
                    (Batch<? extends LimsLink<ContainerType>>)marshaller.unmarshal(new StreamSource(containerTypesFile));

            assertEquals(ctLinks.size(), ctBatch.getSize(), "Serialised container type links don't match the original.");


            List<LimsLink<ReagentType>> rtLinks = api.listSome(ReagentType.class, 0, 120);

            assertEquals(120, rtLinks.size(), "Wrong number of ReagentType links returned.");

            File reagentTypesFile = new File(messageDirectory, "ReagentTypes.xml");
            assertTrue(reagentTypesFile.exists(), "Reagent types not recorded.");

            @SuppressWarnings("unchecked")
            Batch<? extends LimsLink<ReagentType>> rtBatch =
                    (Batch<? extends LimsLink<ReagentType>>)marshaller.unmarshal(new StreamSource(reagentTypesFile));

            assertEquals(rtLinks.size(), rtBatch.getSize(), "Serialised reagent type links don't match the original.");
        }
        catch (ResourceAccessException e)
        {
            realServerDown(e);
        }
    }

    @Test
    public void testRecordSearch()
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("inputartifactlimsid", "2-1108999");
            api.find(terms, ClarityProcess.class);

            SearchTerms<ClarityProcess> st1 = new SearchTerms<ClarityProcess>(terms, ClarityProcess.class);
            assertSearchRecorded(st1);

            terms.clear();
            terms.put("projectlimsid", new HashSet<String>(Arrays.asList("COH605", "SER1015")));
            api.find(terms, Sample.class);

            SearchTerms<Sample> st2 = new SearchTerms<Sample>(terms, Sample.class);
            assertSearchRecorded(st2);
        }
        catch (ResourceAccessException e)
        {
            realServerDown(e);
        }
    }

    @Test
    public void testSearchCheckAndMergeDifferentParams() throws IOException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("inputartifactlimsid", "2-1108999");

        Search<ClarityProcess> s1 = new Search<ClarityProcess>(terms1, ClarityProcess.class);

        File s1File = new File(messageDirectory, s1.getSearchFileName());

        aspect.serialiseSearch(s1, s1File);

        File s1Written = assertSearchRecorded(s1);
        assertEquals(s1File, s1Written, "Written in wrong file");

        // We'll fix this to rename the file as if it is an incompatible search which a hash clash.

        Map<String, Object> terms2 = new HashMap<String, Object>();
        terms1.put("inputartifactlimsid", "2-746813");

        Search<ClarityProcess> s2 = new Search<ClarityProcess>(terms2, ClarityProcess.class);

        File s2File = new File(messageDirectory, s2.getSearchFileName());

        assertTrue(s1File.renameTo(s2File), "Could not change file name for test.");

        Logger realLogger = aspect.logger;
        try
        {
            Logger mockLogger = mock(Logger.class);

            aspect.logger = mockLogger;

            boolean mergeResult = aspect.checkAndMergeWithExisting(s2, s2File);

            assertTrue(mergeResult, "Incompatible merging says not to do anything");

            verify(mockLogger, times(1)).error("Have two incompatible searches that reduce to the same hash:");
            verify(mockLogger, times(1)).error(s2.getSearchTerms().toString());
            verify(mockLogger, times(1)).error(s1.getSearchTerms().toString());
        }
        finally
        {
            aspect.logger = realLogger;
        }
    }

    @Test
    public void testSearchCheckAndMergeDifferentTypes() throws IOException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("inputartifactlimsid", "2-1108999");

        Search<ClarityProcess> s1 = new Search<ClarityProcess>(terms1, ClarityProcess.class);

        File s1File = new File(messageDirectory, s1.getSearchFileName());

        aspect.serialiseSearch(s1, s1File);

        File s1Written = assertSearchRecorded(s1);
        assertEquals(s1File, s1Written, "Written in wrong file");

        // We'll fix this to rename the file as if it is an incompatible search which a hash clash.

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);

        Search<Artifact> s2 = new Search<Artifact>(terms2, Artifact.class);

        File s2File = new File(messageDirectory, s2.getSearchFileName());

        assertTrue(s1File.renameTo(s2File), "Could not change file name for test.");

        Logger realLogger = aspect.logger;
        try
        {
            Logger mockLogger = mock(Logger.class);

            aspect.logger = mockLogger;

            boolean mergeResult = aspect.checkAndMergeWithExisting(s2, s2File);

            assertTrue(mergeResult, "Incompatible merging says not to do anything");

            verify(mockLogger, times(1)).error("Have two incompatible searches that reduce to the same hash:");
            verify(mockLogger, times(1)).error(s2.getSearchTerms().toString());
            verify(mockLogger, times(1)).error(s1.getSearchTerms().toString());
        }
        finally
        {
            aspect.logger = realLogger;
        }
    }

    private <E extends LimsEntity<E>, L extends LimsEntityLinkable<E>> File assertRecorded(L entity)
    {
        String className = ClassUtils.getShortClassName(entity.getClass());
        File entityFile = new File(messageDirectory, className + "-" + entity.getLimsid() + ".xml");
        assertTrue(entityFile.exists(), "Have not recorded " + className + " " + entity.getLimsid());
        return entityFile;
    }

    private <L extends Locatable> File assertRecorded(L object)
    {
        String className = ClassUtils.getShortClassName(object.getClass());

        String id = object.getUri().toString();
        int lastSlash = id.lastIndexOf('/');
        id = id.substring(lastSlash + 1);

        File entityFile = new File(messageDirectory, className + "-" + id + ".xml");
        assertTrue(entityFile.exists(), "Have not recorded " + className + " " + id);

        return entityFile;
    }

    private File assertSearchRecorded(Search<?> search)
    {
        return assertSearchRecorded(search.getSearchTerms());
    }

    private File assertSearchRecorded(SearchTerms<?> terms)
    {
        File searchFile = new File(messageDirectory, Search.getSearchFileName(terms));
        assertTrue(searchFile.exists(), "Have not recorded search.");
        return searchFile;
    }

    private void realServerDown(ResourceAccessException rae) throws ResourceAccessException
    {
        try
        {
            throw rae.getCause();
        }
        catch (HttpHostConnectException hhce)
        {
            assumeTrue(false, "The server " + hhce.getHost() + " is not available. Test cannot run.");
        }
        catch (Throwable e)
        {
        }

        throw rae;
    }
}
