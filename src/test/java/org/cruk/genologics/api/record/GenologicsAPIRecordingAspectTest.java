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

import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.cruk.genologics.api.GenologicsAPI;
import org.cruk.genologics.api.http.AuthenticatingClientHttpRequestFactory;
import org.cruk.genologics.api.search.Search;
import org.cruk.genologics.api.search.SearchTerms;
import org.cruk.genologics.api.unittests.CRUKCICheck;
import org.cruk.genologics.api.unittests.ClarityClientRecorderRecordTestConfiguration;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
import com.genologics.ri.process.GenologicsProcess;
import com.genologics.ri.project.Project;
import com.genologics.ri.reagenttype.ReagentType;
import com.genologics.ri.researcher.Researcher;
import com.genologics.ri.role.Role;
import com.genologics.ri.sample.Sample;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ClarityClientRecorderRecordTestConfiguration.class)
public class GenologicsAPIRecordingAspectTest
{
    @Autowired
    private GenologicsAPI api;

    @Autowired
    private Jaxb2Marshaller marshaller;

    @Autowired
    private GenologicsAPIRecordingAspect aspect;

    @Autowired
    @Qualifier("genologicsClientHttpRequestFactory")
    protected AuthenticatingClientHttpRequestFactory httpRequestFactory;

    private File messageDirectory = new File("target/messages");

    public GenologicsAPIRecordingAspectTest()
    {
    }

    @PostConstruct
    public void completeWiring()
    {
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

    private void checkCredentialsFileExists()
    {
        Assume.assumeTrue("Could not set credentials for the API, which is needed for this test. " +
                          "Put a \"testcredentials.properties\" file on the class path.",
                          httpRequestFactory.getCredentials() != null);
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
            assertTrue("Container types not recorded.", containerTypesFile.exists());

            @SuppressWarnings("unchecked")
            Batch<? extends LimsLink<ContainerType>> ctBatch =
                    (Batch<? extends LimsLink<ContainerType>>)marshaller.unmarshal(new StreamSource(containerTypesFile));

            assertEquals("Serialised container type links don't match the original.", ctLinks.size(), ctBatch.getSize());


            List<LimsLink<ReagentType>> rtLinks = api.listSome(ReagentType.class, 0, 120);

            assertEquals("Wrong number of ReagentType links returned.", 120, rtLinks.size());

            File reagentTypesFile = new File(messageDirectory, "ReagentTypes.xml");
            assertTrue("Reagent types not recorded.", reagentTypesFile.exists());

            @SuppressWarnings("unchecked")
            Batch<? extends LimsLink<ReagentType>> rtBatch =
                    (Batch<? extends LimsLink<ReagentType>>)marshaller.unmarshal(new StreamSource(reagentTypesFile));

            assertEquals("Serialised reagent type links don't match the original.", rtLinks.size(), rtBatch.getSize());
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
            api.find(terms, GenologicsProcess.class);

            SearchTerms st1 = new SearchTerms(terms, GenologicsProcess.class);
            assertSearchRecorded(st1);

            terms.clear();
            terms.put("projectlimsid", new HashSet<String>(Arrays.asList("COH605", "SER1015")));
            api.find(terms, Sample.class);

            SearchTerms st2 = new SearchTerms(terms, Sample.class);
            assertSearchRecorded(st2);
        }
        catch (ResourceAccessException e)
        {
            realServerDown(e);
        }
    }

    private <E extends LimsEntity<E>, L extends LimsEntityLinkable<E>> void assertRecorded(L entity)
    {
        String className = ClassUtils.getShortClassName(entity.getClass());
        File entityFile = new File(messageDirectory, className + "-" + entity.getLimsid() + ".xml");
        assertTrue("Have not recorded " + className + " " + entity.getLimsid(), entityFile.exists());
    }

    private <L extends Locatable> void assertRecorded(L object)
    {
        String className = ClassUtils.getShortClassName(object.getClass());

        String id = object.getUri().toString();
        int lastSlash = id.lastIndexOf('/');
        id = id.substring(lastSlash + 1);

        File entityFile = new File(messageDirectory, className + "-" + id + ".xml");
        assertTrue("Have not recorded " + className + " " + id, entityFile.exists());
    }

    private void assertSearchRecorded(SearchTerms terms)
    {
        File searchFile = new File(messageDirectory, Search.getSearchFileName(terms));
        assertTrue("Have not recorded search.", searchFile.exists());
    }

    private void realServerDown(ResourceAccessException rae) throws ResourceAccessException
    {
        try
        {
            throw rae.getCause();
        }
        catch (HttpHostConnectException hhce)
        {
            Assume.assumeNoException("The server " + hhce.getHost() + " is not available. Test cannot run.", hhce);
        }
        catch (Throwable e)
        {
        }

        throw rae;
    }
}
