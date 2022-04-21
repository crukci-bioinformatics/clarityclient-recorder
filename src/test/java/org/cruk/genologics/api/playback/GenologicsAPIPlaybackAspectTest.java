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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.cruk.genologics.api.GenologicsAPI;
import org.cruk.genologics.api.unittests.ClarityClientRecorderPlaybackTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.ResourceAccessException;

import com.genologics.ri.LimsLink;
import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.artifact.ArtifactLink;
import com.genologics.ri.container.Container;
import com.genologics.ri.containertype.ContainerType;
import com.genologics.ri.lab.Lab;
import com.genologics.ri.permission.Permission;
import com.genologics.ri.process.GenologicsProcess;
import com.genologics.ri.project.Project;
import com.genologics.ri.reagenttype.ReagentType;
import com.genologics.ri.researcher.Researcher;
import com.genologics.ri.role.Role;
import com.genologics.ri.sample.Sample;
import com.genologics.ri.sample.SampleLink;

@SpringJUnitConfig(classes = ClarityClientRecorderPlaybackTestConfiguration.class)
public class GenologicsAPIPlaybackAspectTest
{
    @Autowired
    private Jaxb2Marshaller marshaller;

    @Autowired
    private GenologicsAPI api;

    @Autowired
    private GenologicsAPIPlaybackAspect aspect;

    private File messageDirectory = new File("src/test/messages");
    private File updateDirectory = new File("target/updates");

    public GenologicsAPIPlaybackAspectTest()
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
    }

    @AfterEach
    public void cleanup()
    {
        FileUtils.deleteQuietly(updateDirectory);
    }

    @Test
    public void testReplay()
    {
        try
        {
            Container c = api.load("27-340091", Container.class);
            assertEquals("HFTC7BBXX", c.getName(), "Container name wrong");

            ContainerType ct = api.load(c.getContainerType());
            assertEquals("Illumina HiSeq 4000 Flow Cell", ct.getName(), "Container type name wrong");

            Artifact a = api.load("2-5898189", Artifact.class);
            assertEquals("SLX-12321_NORM-1", a.getName(), "Artifact name wrong");

            Sample s = api.load("GAO9862A146", Sample.class);
            assertEquals("34_a", s.getName(), "Sample name wrong");

            Project p = api.load(s.getProject());
            assertEquals("Poseidon-NGTAS-201611", p.getName(), "Project name wrong");

            Researcher r = api.load(p.getResearcher());
            assertEquals("Meiling", r.getFirstName(), "Researcher name wrong");

            Lab l = api.load(r.getLab());
            assertEquals("CRUKCI", l.getName(), "Lab name wrong");

            ReagentType rt = api.load("374", ReagentType.class);
            assertEquals("Fluidigm", rt.getReagentCategory(), "Reagent category wrong");

            Role role = api.load("3", Role.class);
            assertEquals("Collaborator", role.getName(), "Role name wrong");

            Permission perm = api.load("5", Permission.class);
            assertEquals("Project", perm.getName(), "Permission name wrong");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testLoadAll() throws Exception
    {
        try
        {
            List<LimsLink<Artifact>> alinks = new ArrayList<LimsLink<Artifact>>();
            alinks.add(new ArtifactLink(new URI("https://limsdev.cruk.cam.ac.uk/api/v2/artifacts/2-5898189")));
            alinks.add(new ArtifactLink(new URI("https://limsdev.cruk.cam.ac.uk/api/v2/artifacts/2-6764648")));

            api.loadAll(alinks);

            List<LimsLink<Sample>> slinks = new ArrayList<LimsLink<Sample>>();
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
            assertNull(s, "Got something back from sample when expecting null.");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testReplySearch1()
    {
        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("inputartifactlimsid", "2-1108999");
            List<LimsLink<GenologicsProcess>> processes = api.find(terms, GenologicsProcess.class);
            assertNotNull(processes, "Nothing returned from search.");
            assertEquals(4, processes.size(), "Wrong number of processes returned from search.");
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testReplySearch2()
    {
        Map<String, Object> terms = new HashMap<String, Object>();
        terms.put("projectlimsid", new HashSet<String>(Arrays.asList("COH605", "SER1015")));
        List<LimsLink<Sample>> samples = api.find(terms, Sample.class);
        assertNotNull(samples, "Nothing returned from search.");
        assertEquals(8, samples.size(), "Wrong number of samples returned from search.");
    }

    @Test
    public void testReplySearch3() throws Throwable
    {
        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("name", "SLX-7230_NORM");

            api.find(terms, Artifact.class);
            fail("Got a result when a search was not recorded.");
        }
        catch (UndeclaredThrowableException e1)
        {
            try
            {
                throw e1.getUndeclaredThrowable();
            }
            catch (FileNotFoundException e2)
            {
                // Expected.
            }
            catch (Throwable e2)
            {
                throw e2;
            }
        }
        catch (ResourceAccessException e)
        {
            realServerAccess(e);
        }
    }

    @Test
    public void testUpdate()
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

            Sample sv1 = (Sample)marshaller.unmarshal(new StreamSource(update1File));
            assertEquals("Name change one", sv1.getName(), "Version zero name wrong");

            Sample sv2 = (Sample)marshaller.unmarshal(new StreamSource(update2File));
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
