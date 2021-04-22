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

package org.cruk.genologics.api.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.genologics.ri.LimsLink;
import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.artifact.ArtifactLink;
import com.genologics.ri.artifact.SampleLink;
import com.genologics.ri.sample.Sample;

public class SearchTest
{
    final String baseS = "http://localhost/api/v2/samples/";
    final String baseA = "http://localhost/api/v2/artifacts/";

    public SearchTest()
    {
    }

    @Test
    public void testMerge() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<LimsLink<Sample>>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search<Sample> search1 = new Search<Sample>(terms1, Sample.class);
        search1.setResults(results1);

        Map<String, Object> terms2 = new HashMap<String, Object>();
        terms2.put("A", "asdfg");
        terms2.put("B", 87);

        List<LimsLink<Sample>> results2 = new ArrayList<LimsLink<Sample>>();
        results2.add(new SampleLink(new URI(baseS + "BOW123")));
        results2.add(new SampleLink(new URI(baseS + "SAW543")));

        Search<Sample> search2 = new Search<Sample>(terms2, Sample.class);
        search2.setResults(results2);

        boolean merged = search1.merge(search2);

        assertTrue("Merge has resulted in a change.", merged);

        assertEquals("Wrong number of links in merged result", 3, search1.getResults().size());

        Map<URI, LimsLink<Sample>> map = new HashMap<URI, LimsLink<Sample>>();
        for (LimsLink<Sample> link : search1.getResults())
        {
            map.put(link.getUri(), link);
        }

        assertTrue("Results doesn't contain BOW123", map.containsKey(new URI(baseS + "BOW123")));
        assertTrue("Results doesn't contain CAR876", map.containsKey(new URI(baseS + "CAR876")));
        assertTrue("Results doesn't contain SAW543", map.containsKey(new URI(baseS + "SAW543")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMergeDifferentClass() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink> results1 = new ArrayList<LimsLink>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search search1 = new Search(terms1, Sample.class);
        search1.setResults(results1);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);

        List<LimsLink> results2 = new ArrayList<LimsLink>();
        results2.add(new ArtifactLink(new URI(baseA + "BOW123")));
        results2.add(new ArtifactLink(new URI(baseA + "SAW543")));

        Search search2 = new Search(terms2, Artifact.class);
        search2.setResults(results2);

        try
        {
            search1.merge(search2);
        }
        catch (IllegalArgumentException e)
        {
            // This is correct.
            assertTrue("Error not as expected.", e.getMessage().startsWith("Can't merge searches for different entity types."));
        }
    }

    @Test
    public void testMergeNullSelf() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<LimsLink<Sample>>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search<Sample> search1 = new Search<Sample>(terms1, Sample.class);
        search1.setResults(results1);

        assertFalse("Merge null says work is done.", search1.merge(null));
        assertFalse("Merge self says work is done.", search1.merge(search1));
    }

    @Test
    public void testMergeEmptyOther() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<LimsLink<Sample>>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search<Sample> search1 = new Search<Sample>(terms1, Sample.class);
        search1.setResults(results1);

        Map<String, Object> terms2 = new HashMap<String, Object>();
        terms2.put("A", "asdfg");
        terms2.put("B", 87);

        List<LimsLink<Sample>> results2 = new ArrayList<LimsLink<Sample>>();

        // Null results

        Search<Sample> search2 = new Search<Sample>(terms2, Sample.class);

        assertFalse("Merge with null results in other says work is done.", search1.merge(search2));

        // Empty results

        search2.setResults(results2);

        assertFalse("Merge with empty results in other says work is done.", search1.merge(search2));
    }


    @Test
    public void testMergeNullThis() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<LimsLink<Sample>>();

        Search<Sample> search1 = new Search<Sample>(terms1, Sample.class);

        Map<String, Object> terms2 = new HashMap<String, Object>();
        terms2.put("A", "asdfg");
        terms2.put("B", 87);

        List<LimsLink<Sample>> results2 = new ArrayList<LimsLink<Sample>>();
        results2.add(new SampleLink(new URI(baseS + "BOW123")));
        results2.add(new SampleLink(new URI(baseS + "SAW543")));

        Search<Sample> search2 = new Search<Sample>(terms2, Sample.class);
        search2.setResults(results2);

        // Null results in this

        assertTrue("Merge with null results in other says work is not done.", search1.merge(search2));

        // Empty results

        search1.setResults(results1);

        assertTrue("Merge with empty results in other says work is not done.", search1.merge(search2));
    }
}
