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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import jakarta.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.cruk.clarity.api.ClarityAPI;
import org.cruk.clarity.api.ClarityException;
import org.cruk.clarity.api.unittests.ClarityClientRecorderPlaybackTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.Unmarshaller;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.ResourceAccessException;

import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.artifact.ArtifactLink;
import com.genologics.ri.artifact.Demux;
import com.genologics.ri.container.Container;
import com.genologics.ri.containertype.ContainerType;
import com.genologics.ri.instrument.Instrument;
import com.genologics.ri.lab.Lab;
import com.genologics.ri.permission.Permission;
import com.genologics.ri.process.ClarityProcess;
import com.genologics.ri.project.Project;
import com.genologics.ri.protocolconfiguration.Protocol;
import com.genologics.ri.reagenttype.ReagentType;
import com.genologics.ri.researcher.Researcher;
import com.genologics.ri.role.Role;
import com.genologics.ri.sample.Sample;
import com.genologics.ri.sample.SampleLink;
import com.genologics.ri.stage.Stage;
import com.genologics.ri.stepconfiguration.ProtocolStep;
import com.genologics.ri.workflowconfiguration.Workflow;

@SpringJUnitConfig(classes = ClarityClientRecorderPlaybackTestConfiguration.class)
public class ClarityAPIPlaybackAspectTest
{
    @Autowired
    @Qualifier("clarityJaxbUnmarshaller")
    private Unmarshaller unmarshaller;

    @Autowired
    private ClarityAPI api;

    @Autowired
    private ClarityAPIPlaybackAspect aspect;

    private File messageDirectory = new File("src/test/messages");
    private File updateDirectory = new File("target/updates");

    public ClarityAPIPlaybackAspectTest()
    {
    }

    @PostConstruct
    public void completeWiring() throws MalformedURLException
    {
        // To prove it's from the recording.
        api.setServer(new URL("http://localhost"));

        aspect.setMessageDirectory(messageDirectory);
        aspect.setUpdatesDirectory(updateDirectory);
    }

    @BeforeEach
    public void setup() throws IOException
    {
        FileUtils.deleteQuietly(updateDirectory);
        FileUtils.forceMkdir(updateDirectory);

        aspect.setFailOnMissingSearch(false);
    }

    @AfterEach
    public void cleanup()
    {
        FileUtils.deleteQuietly(updateDirectory);
    }

    /**
     * Additional test for instrument to cover mismatch between lims id and URI.
     *
     * @see <a href="https://redmine-bioinformatics.cruk.cam.ac.uk/issues/7273">Redmine 7273</a>
     */
    @Test
    public void testReplayInstrumentShort()
    {
        Instrument i = testReplay("5", Instrument.class);
        assertEquals("Luke-Leia [HWI-ST230]", i.getName(), "Instrument name wrong");
    }

    /**
     * Additional test for instrument to cover mismatch between lims id and URI.
     *
     * @see <a href="https://redmine-bioinformatics.cruk.cam.ac.uk/issues/7273">Redmine 7273</a>
     */
    //@Test
    public void testReplayInstrumentLimsId()
    {
        Instrument i = testReplay("55-5", Instrument.class);
        assertEquals("Luke-Leia [HWI-ST230]", i.getName(), "Instrument name wrong");
    }

    @Test
    public void testReplayContainer()
    {
        Container c = testReplay("27-340091", Container.class);
        assertEquals("HFTC7BBXX", c.getName(), "Container name wrong");
    }

    @Test
    public void testReplayContainerType()
    {
        ContainerType ct = testReplay("204", ContainerType.class);
        assertEquals("Illumina HiSeq 4000 Flow Cell", ct.getName(), "Container type name wrong");
    }

    @Test
    public void testReplayArtifact()
    {
        Artifact a = testReplay("2-5898189", Artifact.class);
        assertEquals("SLX-12321_NORM-1", a.getName(), "Artifact name wrong");
    }

    @Test
    public void testReplayDemux()
    {
        Demux demux = testReplay("2-5898189", Demux.class);
        assertEquals("Accept SLX", demux.getDemuxDetails().getPoolStep().getName(), "Demux process type wrong.");
    }

    @Test
    public void testReplaySample()
    {
        Sample s = testReplay("GAO9862A146", Sample.class);
        assertEquals("34_a", s.getName(), "Sample name wrong");
    }

    @Test
    public void testReplayProject()
    {
        Project p = testReplay("GAO9862", Project.class);
        assertEquals("Poseidon-NGTAS-201611", p.getName(), "Project name wrong");
    }

    @Test
    public void testReplayResearcher()
    {
        Researcher r = testReplay("5356", Researcher.class);
        assertEquals("Meiling", r.getFirstName(), "Researcher name wrong");
    }

    @Test
    public void testReplayLab()
    {
        Lab l = testReplay("18", Lab.class);
        assertEquals("CRUKCI", l.getName(), "Lab name wrong");
    }

    @Test
    public void testReplayReagentType()
    {
        ReagentType rt = testReplay("374", ReagentType.class);
        assertEquals("Fluidigm", rt.getReagentCategory(), "Reagent category wrong");
    }

    @Test
    public void testReplayRole()
    {
        Role role = testReplay("3", Role.class);
        assertEquals("Collaborator", role.getName(), "Role name wrong");
    }

    @Test
    public void testReplayPermission()
    {
        Permission perm = testReplay("5", Permission.class);
        assertEquals("Project", perm.getName(), "Permission name wrong");
    }

    @Test
    public void testReplayProtocol()
    {
        Protocol protocol = testReplay("1", Protocol.class);
        assertEquals("LPS: Accept LPS", protocol.getName(), "Protocol name wrong.");
    }

    @Test
    public void testReplayProtocolStep()
    {
        ProtocolStep step = testReplay("1", "1", ProtocolStep.class);
        assertEquals("LPS Reagents In", step.getName(), "Protocol step name wrong.");
    }

    @Test
    public void testReplayWorkflow()
    {
        Workflow workflow = testReplay("1601", Workflow.class);
        assertEquals("LPS: 10X Single Cell ATAC v1", workflow.getName(), "Workflow name wrong.");
    }

    @Test
    public void testReplayStage()
    {
        Stage stage = testReplay("1601", "2902", Stage.class);
        assertEquals("Transposition (10X)", stage.getName(), "Workflow stage name wrong.");
    }

    private <L extends Locatable> L testReplay(String id, Class<L> type)
    {
        L thing = null;
        try
        {
            thing = api.load(id, type);
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
        return thing;
    }

    private <L extends Locatable> L testReplay(String id1, String id2, Class<L> type)
    {
        L thing = null;
        try
        {
            thing = api.load(id1, id2, type);
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
        return thing;
    }

    @Test
    public void testLoadAll() throws Exception
    {
        try
        {
            List<LimsLink<Artifact>> alinks = new ArrayList<>();
            alinks.add(new ArtifactLink(new URI("https://limsdev.cruk.cam.ac.uk/api/v2/artifacts/2-5898189")));
            alinks.add(new ArtifactLink(new URI("https://limsdev.cruk.cam.ac.uk/api/v2/artifacts/2-6764648")));

            api.loadAll(alinks);

            List<LimsLink<Sample>> slinks = new ArrayList<>();
            slinks.add(new SampleLink(new URI("https://limsdev.cruk.cam.ac.uk/api/v2/samples/GAO9862A146")));
            slinks.add(new SampleLink(new URI("https://limsdev.cruk.cam.ac.uk/api/v2/samples/LEU10792A392")));

            api.loadAll(slinks);
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testNotRecorded()
    {
        try
        {
            Sample s = api.load("0000", Sample.class);
            assertNull(s, "Got something back from sample when expecting a ClarityException.");
        }
        catch (ClarityException e)
        {
            e.throwUnlessNotFound();
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testReplaySearch1()
    {
        aspect.setFailOnMissingSearch(true);

        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("inputartifactlimsid", "2-1108999");
            List<LimsLink<ClarityProcess>> processes = api.find(terms, ClarityProcess.class);
            assertNotNull(processes, "Nothing returned from search.");
            assertEquals(4, processes.size(), "Wrong number of processes returned from search.");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testReplaySearch2()
    {
        aspect.setFailOnMissingSearch(true);

        Map<String, Object> terms = new HashMap<String, Object>();
        terms.put("projectlimsid", new HashSet<String>(Arrays.asList("COH605", "SER1015")));
        List<LimsLink<Sample>> samples = api.find(terms, Sample.class);
        assertNotNull(samples, "Nothing returned from search.");
        assertEquals(8, samples.size(), "Wrong number of samples returned from search.");
    }

    @Test
    public void testReplaySearchNotRecordedWithFail() throws Throwable
    {
        aspect.setFailOnMissingSearch(true);

        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("name", "SLX-7230_NORM");

            api.find(terms, Artifact.class);
            fail("Got a result when a search was not recorded.");
        }
        catch (NoRecordingException e)
        {
            // Expected.
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testReplaySearchNotRecordedNoResults() throws Throwable
    {
        aspect.setFailOnMissingSearch(false);

        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("name", "SLX-7230_NORM");

            var reply = api.find(terms, Artifact.class);

            assertEquals(0, reply.size(), "Received the wrong number of links for a search that is not recorded.");
        }
        catch (NoRecordingException e)
        {
            fail("Got NoRecordingException when we should have just not got any results.");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testUpdate() throws IOException
    {
        try
        {
            Sample s = api.load("GAO9862A146", Sample.class);

            s.setName("Name change one");
            api.update(s);

            File update1File = new File(updateDirectory, "Sample-GAO9862A146.000.xml");
            assertTrue(update1File.exists(), "Updated sample not written to " + update1File.getName());

            s.setName("Second name change");
            api.update(s);

            File update2File = new File(updateDirectory, "Sample-GAO9862A146.001.xml");
            assertTrue(update2File.exists(), "Updated sample not written to " + update2File.getName());

            Sample sv1 = (Sample)unmarshaller.unmarshal(new StreamSource(update1File));
            assertEquals("Name change one", sv1.getName(), "Version zero name wrong");

            Sample sv2 = (Sample)unmarshaller.unmarshal(new StreamSource(update2File));
            assertEquals("Second name change", sv2.getName(), "Version zero name wrong");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testList()
    {
        try
        {
            List<LimsLink<ContainerType>> containerTypes = api.listAll(ContainerType.class);
            assertEquals(23, containerTypes.size(), "Wrong number of container types returned.");

            List<LimsLink<ReagentType>> reagentTypes = api.listSome(ReagentType.class, 20, 50);
            assertEquals(120, reagentTypes.size(), "Wrong number of reagent types returned.");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    private void realServerAccess(ResourceAccessException rae)
    {
        try
        {
            throw rae.getCause();
        }
        catch (HttpHostConnectException hhce)
        {
            fail("Tried to access the real server " + hhce.getHost() + " during playback.");
        }
        catch (Throwable e)
        {
        }
        fail("Tried to access a real server during playback: " + rae.getMessage());
    }
}
