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

package org.cruk.genologics.api.record;

import static org.cruk.genologics.api.unittests.UnitTestApplicationContextFactory.checkCredentialsFileExists;
import static org.cruk.genologics.api.unittests.UnitTestApplicationContextFactory.getRecordingApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.cruk.genologics.api.GenologicsAPI;
import org.cruk.genologics.api.search.Search;
import org.cruk.genologics.api.search.SearchTerms;
import org.cruk.genologics.api.unittests.UnitTestApplicationContextFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.genologics.ri.LimsEntity;
import com.genologics.ri.LimsEntityLinkable;
import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
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

public class GenologicsAPIRecordingAspectTest
{
    private GenologicsAPI api;
    private GenologicsAPIRecordingAspect aspect;

    private File messageDirectory = new File("target/messages");

    public GenologicsAPIRecordingAspectTest()
    {
        ApplicationContext ctx = getRecordingApplicationContext();
        api = ctx.getBean(GenologicsAPI.class);

        aspect = ctx.getBean(GenologicsAPIRecordingAspect.class);
        aspect.setMessageDirectory(messageDirectory);
    }

    @Before
    public void setup() throws IOException
    {
        FileUtils.deleteQuietly(messageDirectory);
        FileUtils.forceMkdir(messageDirectory);
    }

    @After
    public void cleanup()
    {
        FileUtils.deleteQuietly(messageDirectory);
    }

    @Test
    public void testRecording()
    {
        Assume.assumeTrue("Can only run the recording tests as written in CRUK-CI.", UnitTestApplicationContextFactory.inCrukCI());
        checkCredentialsFileExists();

        Container c = api.load("27-340091", Container.class);
        assertRecorded(c);

        ContainerType ct = api.load(c.getContainerType());
        assertRecorded(ct);

        Collections.sort(c.getPlacements());

        Artifact pool = api.load(c.getPlacements().get(4));
        assertEquals("Mismatched ids", "2-5898189", pool.getLimsid());
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

    @Test
    public void testRecordList()
    {
        Assume.assumeTrue("Can only run the recording tests as written in CRUK-CI.", UnitTestApplicationContextFactory.inCrukCI());
        checkCredentialsFileExists();

        api.listAll(ContainerType.class);

        File containerTypesFile = new File(aspect.getMessageDirectory(), "ContainerTypes.xml");
        assertTrue("Container types not recorded.", containerTypesFile.exists());

        List<LimsLink<ReagentType>> rtLinks = api.listSome(ReagentType.class, 1, 119);

        assertEquals("Wrong number of ReagentType links returned.", 119, rtLinks.size());

        File reagentTypesFile = new File(aspect.getMessageDirectory(), "ReagentTypes.xml");
        assertTrue("Reagent types not recorded.", reagentTypesFile.exists());
    }

    @Test
    public void testRecordSearch()
    {
        Assume.assumeTrue("Can only run the recording tests as written in CRUK-CI.", UnitTestApplicationContextFactory.inCrukCI());
        checkCredentialsFileExists();

        Map<String, Object> terms = new HashMap<String, Object>();
        terms.put("inputartifactlimsid", "2-1108999");
        api.find(terms, GenologicsProcess.class);

        SearchTerms st1 = new SearchTerms(terms, GenologicsProcess.class);
        assertSearchRecorded(st1);

        terms.clear();
        terms.put("projectlimsid", new HashSet<String>(Arrays.asList("COH605", "SER1015")));
        api.find(terms, Sample.class);

        SearchTerms st2 = new SearchTerms(terms, Sample.class);
        assertSearchRecorded(st2);
    }

    private <E extends LimsEntity<E>, L extends LimsEntityLinkable<E>> void assertRecorded(L entity)
    {
        String className = ClassUtils.getShortClassName(entity.getClass());
        File entityFile = new File(aspect.getMessageDirectory(), className + "-" + entity.getLimsid() + ".xml");
        assertTrue("Have not recorded " + className + " " + entity.getLimsid(), entityFile.exists());
    }

    private <L extends Locatable> void assertRecorded(L object)
    {
        String className = ClassUtils.getShortClassName(object.getClass());

        String id = object.getUri().toString();
        int lastSlash = id.lastIndexOf('/');
        id = id.substring(lastSlash + 1);

        File entityFile = new File(aspect.getMessageDirectory(), className + "-" + id + ".xml");
        assertTrue("Have not recorded " + className + " " + id, entityFile.exists());
    }

    private void assertSearchRecorded(SearchTerms terms)
    {
        File searchFile = new File(aspect.getSearchDirectory(), Search.getSearchFileName(terms));
        assertTrue("Have not recorded search.", searchFile.exists());
    }

}
