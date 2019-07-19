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
import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cruk.genologics.api.GenologicsAPI;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.genologics.ri.LimsLink;
import com.genologics.ri.artifact.Artifact;
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

public class GenologicsAPIPlaybackAspectTest
{
    private GenologicsAPI api;

    private File messageDirectory = new File("src/test/messages");

    public GenologicsAPIPlaybackAspectTest() throws MalformedURLException
    {
        ApplicationContext ctx = getPlaybackApplicationContext();
        api = ctx.getBean("genologicsAPI", GenologicsAPI.class);

        // To prove it's from the recording.
        api.setServer(new URL("http://localhost"));

        GenologicsAPIPlaybackAspect aspect = ctx.getBean(GenologicsAPIPlaybackAspect.class);
        aspect.setMessageDirectory(messageDirectory);
    }

    @Test
    public void testReplay()
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

    @Test
    public void testNotRecorded()
    {
        Sample s = api.load("0000", Sample.class);
        assertNull("Got something back from sample when expecting null.", s);
    }

    @Test
    public void testReplySearch1()
    {
        Map<String, Object> terms = new HashMap<String, Object>();
        terms.put("inputartifactlimsid", "2-1108999");
        List<LimsLink<GenologicsProcess>> processes = api.find(terms, GenologicsProcess.class);
        assertNotNull("Nothing returned from search.", processes);
        assertEquals("Wrong number of processes returned from search.", 4, processes.size());
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
    public void testReplySearch3()
    {
        Map<String, Object> terms = new HashMap<String, Object>();
        terms.put("name", "SLX-7230_NORM");

        List<LimsLink<Artifact>> artifacts = api.find(terms, Artifact.class);
        assertNull("Got a result when a search was not recorded.", artifacts);
    }
}
