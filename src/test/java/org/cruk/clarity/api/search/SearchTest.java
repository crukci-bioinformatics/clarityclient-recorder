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

package org.cruk.clarity.api.search;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
        Map<String, Object> terms1 = new HashMap<>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search<Sample> search1 = new Search<>(terms1, Sample.class);
        search1.setResults(results1);

        Map<String, Object> terms2 = new HashMap<>();
        terms2.put("A", "asdfg");
        terms2.put("B", 87);

        List<LimsLink<Sample>> results2 = new ArrayList<>();
        results2.add(new SampleLink(new URI(baseS + "BOW123")));
        results2.add(new SampleLink(new URI(baseS + "SAW543")));

        Search<Sample> search2 = new Search<>(terms2, Sample.class);
        search2.setResults(results2);

        boolean merged = search1.merge(search2);

        assertTrue(merged, "Merge has resulted in a change.");

        assertEquals(3, search1.getResults().size(), "Wrong number of links in merged result");

        Map<URI, LimsLink<Sample>> map = new HashMap<>();
        for (LimsLink<Sample> link : search1.getResults())
        {
            map.put(link.getUri(), link);
        }

        assertTrue(map.containsKey(new URI(baseS + "BOW123")), "Results doesn't contain BOW123");
        assertTrue(map.containsKey(new URI(baseS + "CAR876")), "Results doesn't contain CAR876");
        assertTrue(map.containsKey(new URI(baseS + "SAW543")), "Results doesn't contain SAW543");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testMergeDifferentClass() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink> results1 = new ArrayList<LimsLink>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search search1 = new Search(terms1, Sample.class);
        search1.setResults(results1);

        Map<String, Object> terms2 = new HashMap<>(terms1);

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
            assertTrue(e.getMessage().startsWith("Can't merge searches for different entity types."), "Error not as expected.");
        }
    }

    @Test
    public void testMergeNullSelf() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search<Sample> search1 = new Search<>(terms1, Sample.class);
        search1.setResults(results1);

        assertFalse(search1.merge(null), "Merge null says work is done.");
        assertFalse(search1.merge(search1), "Merge self says work is done.");
    }

    @Test
    public void testMergeEmptyOther() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<>();
        results1.add(new SampleLink(new URI(baseS + "BOW123")));
        results1.add(new SampleLink(new URI(baseS + "CAR876")));

        Search<Sample> search1 = new Search<>(terms1, Sample.class);
        search1.setResults(results1);

        Map<String, Object> terms2 = new HashMap<>();
        terms2.put("A", "asdfg");
        terms2.put("B", 87);

        List<LimsLink<Sample>> results2 = new ArrayList<>();

        // Null results

        Search<Sample> search2 = new Search<>(terms2, Sample.class);

        assertFalse(search1.merge(search2), "Merge with null results in other says work is done.");

        // Empty results

        search2.setResults(results2);

        assertFalse(search1.merge(search2), "Merge with empty results in other says work is done.");
    }


    @Test
    public void testMergeNullThis() throws URISyntaxException
    {
        Map<String, Object> terms1 = new HashMap<>();
        terms1.put("A", "qwerty");
        terms1.put("B", 67);

        List<LimsLink<Sample>> results1 = new ArrayList<>();

        Search<Sample> search1 = new Search<>(terms1, Sample.class);

        Map<String, Object> terms2 = new HashMap<>();
        terms2.put("A", "asdfg");
        terms2.put("B", 87);

        List<LimsLink<Sample>> results2 = new ArrayList<>();
        results2.add(new SampleLink(new URI(baseS + "BOW123")));
        results2.add(new SampleLink(new URI(baseS + "SAW543")));

        Search<Sample> search2 = new Search<>(terms2, Sample.class);
        search2.setResults(results2);

        // Null results in this

        assertTrue(search1.merge(search2), "Merge with null results in other says work is not done.");

        // Empty results

        search1.setResults(results1);

        assertTrue(search1.merge(search2), "Merge with empty results in other says work is not done.");
    }
}
