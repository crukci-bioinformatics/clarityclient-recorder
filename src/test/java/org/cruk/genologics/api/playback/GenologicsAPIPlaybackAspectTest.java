package org.cruk.genologics.api.playback;

import static org.cruk.genologics.api.unittests.UnitTestApplicationContextFactory.getPlaybackApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.cruk.genologics.api.GenologicsAPI;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.container.Container;
import com.genologics.ri.containertype.ContainerType;
import com.genologics.ri.lab.Lab;
import com.genologics.ri.permission.Permission;
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
}
