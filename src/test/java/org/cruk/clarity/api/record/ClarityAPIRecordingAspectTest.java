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

import static org.cruk.clarity.api.record.ClarityAPIRecordingAspect.limsIdFromUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.hc.client5.http.HttpHostConnectException;
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
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.artifact.Demux;
import com.genologics.ri.container.Container;
import com.genologics.ri.containertype.ContainerType;
import com.genologics.ri.containertype.ContainerTypes;
import com.genologics.ri.instrument.Instrument;
import com.genologics.ri.lab.Lab;
import com.genologics.ri.permission.Permission;
import com.genologics.ri.process.ClarityProcess;
import com.genologics.ri.project.Project;
import com.genologics.ri.protocolconfiguration.Protocol;
import com.genologics.ri.reagenttype.ReagentType;
import com.genologics.ri.reagenttype.ReagentTypes;
import com.genologics.ri.researcher.Researcher;
import com.genologics.ri.role.Role;
import com.genologics.ri.sample.Sample;
import com.genologics.ri.stage.Stage;
import com.genologics.ri.step.ProcessStep;
import com.genologics.ri.step.StepDetails;
import com.genologics.ri.stepconfiguration.ProtocolStep;
import com.genologics.ri.workflowconfiguration.Workflow;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBException;

@SpringJUnitConfig(classes = ClarityClientRecorderRecordTestConfiguration.class)
public class ClarityAPIRecordingAspectTest
{
    @Autowired
    private ClarityAPI api;

    @Autowired
    @Qualifier("claritySearchMarshaller")
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
        //FileUtils.deleteQuietly(messageDirectory);
    }

    private void checkCredentialsFileExists()
    {
        assumeTrue(httpRequestFactory.getCredentials() != null,
                   "Could not set credentials for the API, which is needed for this test. " +
                   "Put a \"testcredentials.properties\" file on the class path.");
    }

    @Test
    public void testLimsIdFromUri()
    {
        String id = "2-41";

        String uri = api.limsIdToUri(id, Artifact.class).toString();

        assertEquals(id, limsIdFromUri(Artifact.class, uri), "Artifact id from URI wrong.");

        uri = api.limsIdToUri(id, Demux.class).toString();

        assertEquals(id, limsIdFromUri(Demux.class, uri), "Demux id from URI wrong.");

        uri = api.limsIdToUri(id, ProcessStep.class).toString();

        assertEquals(id, limsIdFromUri(ProcessStep.class, uri), "ProcessStep id from URI wrong.");

        uri = api.limsIdToUri(id, StepDetails.class).toString();

        assertEquals(id, limsIdFromUri(StepDetails.class, uri), "StepDetails id from URI wrong.");
    }

    @Test
    public void testRecordInstrument()
    {
        testRecording("5", Instrument.class);
    }

    @Test
    public void testRecordContainer()
    {
        testRecording("27-340091", Container.class);
    }

    @Test
    public void testRecordContainerType()
    {
        testRecording("204", ContainerType.class);
    }

    @Test
    public void testRecordArtifact()
    {
        testRecording("2-5898189", Artifact.class);
    }

    @Test
    public void testRecordDemux()
    {
        testRecording("2-5898189", Demux.class);
    }

    @Test
    public void testRecordSample()
    {
        testRecording("GAO9862A146", Sample.class);
    }

    @Test
    public void testRecordProject()
    {
        testRecording("GAO9862", Project.class);
    }

    @Test
    public void testRecordResearcher()
    {
        testRecording("5356", Researcher.class);
    }

    @Test
    public void testRecordLab()
    {
        testRecording("18", Lab.class);
    }

    @Test
    public void testRecordReagentType()
    {
        testRecording("374", ReagentType.class);
    }

    @Test
    public void testRecordRole()
    {
        testRecording("3", Role.class);
    }

    @Test
    public void testRecordPermission()
    {
        testRecording("5", Permission.class);
    }

    @Test
    public void testRecordProtocol()
    {
        testRecording("1", Protocol.class);
    }

    @Test
    public void testRecordProtocolStep()
    {
        testRecording("1", "1", ProtocolStep.class);
    }

    @Test
    public void testRecordWorkflow()
    {
        testRecording("1601", Workflow.class);
    }

    @Test
    public void testRecordStage()
    {
        testRecording("1601", "2902", Stage.class);
    }

    private <L extends Locatable> void testRecording(String id, Class<L> type)
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        try
        {
            L thing = api.load(id, type);
            assertRecorded(thing);
        }
        catch (ResourceAccessException e)
        {
            realServerDown(e);
        }
    }

    private <L extends Locatable> void testRecording(String id1, String id2, Class<L> type)
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        try
        {
            L thing = api.load(id1, id2, type);
            assertRecorded(thing);
        }
        catch (ResourceAccessException e)
        {
            realServerDown(e);
        }
    }

    @Test
    public void testRecordList() throws JAXBException
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        try
        {
            List<LimsLink<ContainerType>> ctLinks = api.listAll(ContainerType.class);

            File containerTypesFile = new File(messageDirectory, "ContainerTypes.xml");
            assertTrue(containerTypesFile.exists(), "Container types not recorded.");

            Batch<? extends LimsLink<ContainerType>> ctBatch =
                    marshaller.createUnmarshaller().unmarshal(new StreamSource(containerTypesFile), ContainerTypes.class).getValue();

            assertEquals(ctLinks.size(), ctBatch.getSize(), "Serialised container type links don't match the original.");

            List<LimsLink<ReagentType>> rtLinks = api.listSome(ReagentType.class, 0, 120);

            assertEquals(120, rtLinks.size(), "Wrong number of ReagentType links returned.");

            File reagentTypesFile = new File(messageDirectory, "ReagentTypes.xml");
            assertTrue(reagentTypesFile.exists(), "Reagent types not recorded.");

            @SuppressWarnings("unchecked")
            Batch<? extends LimsLink<ReagentType>> rtBatch =
                    marshaller.createUnmarshaller().unmarshal(new StreamSource(reagentTypesFile), ReagentTypes.class).getValue();

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

    @Test
    public void testSearchRecordNoResultsRecording()
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        Map<String, Object> params = new HashMap<>();
        params.put("name", "HELLO_I_DONT_EXIST");

        Search<Sample> search = new Search<>(params, Sample.class);
        search.setResults(Collections.emptyList());

        aspect.setRecordSearchesWithoutResults(true);

        api.find(params, Sample.class);

        File recorded = new File(aspect.getMessageDirectory(), search.getSearchFileName());
        assertTrue(recorded.exists(), "Didn't record search " + search);
    }

    @Test
    public void testSearchRecordNoResultsDoNotRecord()
    {
        CRUKCICheck.assumeInCrukCI();
        checkCredentialsFileExists();

        Map<String, Object> params = new HashMap<>();
        params.put("name", "HELLO_I_DONT_EXIST");

        Search<Sample> search = new Search<>(params, Sample.class);
        search.setResults(Collections.emptyList());

        aspect.setRecordSearchesWithoutResults(false);

        api.find(params, Sample.class);

        File recorded = new File(aspect.getMessageDirectory(), search.getSearchFileName());
        assertFalse(recorded.exists(), "Recorded search " + search);
    }

    private <L extends Locatable> File assertRecorded(L object)
    {
        String className = ClassUtils.getShortClassName(object.getClass());

        String id = ClarityAPIRecordingAspect.limsIdFromObject(object);

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
