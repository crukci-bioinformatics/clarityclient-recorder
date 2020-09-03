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

import static org.cruk.genologics.api.unittests.UnitTestApplicationContextFactory.getPlaybackApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.cruk.genologics.api.GenologicsAPI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
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

public class GenologicsAPIPlaybackAspectTest
{
    private Jaxb2Marshaller marshaller;
    private GenologicsAPI api;
    private GenologicsAPIPlaybackAspect aspect;

    private File messageDirectory = new File("src/test/messages");
    private File updateDirectory = new File("target/updates");

    public GenologicsAPIPlaybackAspectTest() throws MalformedURLException
    {
        ApplicationContext ctx = getPlaybackApplicationContext();
        marshaller = ctx.getBean("genologicsJaxbMarshaller", Jaxb2Marshaller.class);
        api = ctx.getBean("genologicsAPI", GenologicsAPI.class);

        // To prove it's from the recording.
        api.setServer(new URL("http://localhost"));

        aspect = ctx.getBean(GenologicsAPIPlaybackAspect.class);
        aspect.setMessageDirectory(messageDirectory);
        aspect.setUpdatesDirectory(updateDirectory);
    }

    @Before
    public void setup() throws IOException
    {
        FileUtils.deleteQuietly(updateDirectory);
        FileUtils.forceMkdir(updateDirectory);
    }

    @After
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
            assertEquals("Container name wrong", "HFTC7BBXX", c.getName());

            ContainerType ct = api.load(c.getContainerType());
            assertEquals("Container type name wrong", "Illumina HiSeq 4000 Flow Cell", ct.getName());

            Artifact a = api.load("2-5898189", Artifact.class);
            assertEquals("Artifact name wrong", "SLX-12321_NORM-1", a.getName());

            Sample s = api.load("GAO9862A146", Sample.class);
            assertEquals("Sample name wrong", "34_a", s.getName());

            Project p = api.load(s.getProject());
            assertEquals("Project name wrong", "Poseidon-NGTAS-201611", p.getName());

            Researcher r = api.load(p.getResearcher());
            assertEquals("Researcher name wrong", "Meiling", r.getFirstName());

            Lab l = api.load(r.getLab());
            assertEquals("Lab name wrong", "CRUKCI", l.getName());

            ReagentType rt = api.load("374", ReagentType.class);
            assertEquals("Reagent category wrong", "Fluidigm", rt.getReagentCategory());

            Role role = api.load("3", Role.class);
            assertEquals("Role name wrong", "Collaborator", role.getName());

            Permission perm = api.load("5", Permission.class);
            assertEquals("Permission name wrong", "Project", perm.getName());
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
            assertNull("Got something back from sample when expecting null.", s);
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
            assertNotNull("Nothing returned from search.", processes);
            assertEquals("Wrong number of processes returned from search.", 4, processes.size());
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
        assertNotNull("Nothing returned from search.", samples);
        assertEquals("Wrong number of samples returned from search.", 8, samples.size());
    }

    @Test
    public void testReplySearch3() throws Throwable
    {
        try
        {
            Map<String, Object> terms = new HashMap<String, Object>();
            terms.put("name", "SLX-7230_NORM");

            List<LimsLink<Artifact>> artifacts = api.find(terms, Artifact.class);
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
            assertTrue("Updated sample not written to " + update1File.getName(), update1File.exists());

            s.setName("Second name change");
            api.update(s);

            File update2File = new File(updateDirectory, "Sample-GAO9862A146.001.xml");
            assertTrue("Updated sample not written to " + update2File.getName(), update2File.exists());

            Sample sv1 = (Sample)marshaller.unmarshal(new StreamSource(update1File));
            assertEquals("Version zero name wrong", "Name change one", sv1.getName());

            Sample sv2 = (Sample)marshaller.unmarshal(new StreamSource(update2File));
            assertEquals("Version zero name wrong", "Second name change", sv2.getName());
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
            assertEquals("Wrong number of container types returned.", 23, containerTypes.size());

            List<LimsLink<ReagentType>> reagentTypes = api.listSome(ReagentType.class, 20, 50);
            assertEquals("Wrong number of reagent types returned.", 120, reagentTypes.size());
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
