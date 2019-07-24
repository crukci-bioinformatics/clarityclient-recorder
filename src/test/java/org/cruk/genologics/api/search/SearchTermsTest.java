package org.cruk.genologics.api.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.genologics.ri.artifact.Artifact;
import com.genologics.ri.sample.Sample;

public class SearchTermsTest
{

    public SearchTermsTest()
    {
    }

    @Test
    public void exactlyEqual()
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("p1", "Hello");
        terms1.put("p2", 9L);
        terms1.put("p3", Arrays.asList("First", "Second"));

        SearchTerms st1 = new SearchTerms(terms1, Artifact.class);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);
        terms2.put("p1", "Hello");
        terms2.put("p2", 9L);
        terms2.put("p3", Arrays.asList("First", "Second"));

        SearchTerms st2 = new SearchTerms(terms2, Artifact.class);

        assertEquals("Search terms do not match", st1, st2);
        assertEquals("Search terms hashes do not match", st1.hashCode(), st2.hashCode());
    }

    @Test
    public void differentOrder()
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("var", Arrays.asList("First", "Second"));

        SearchTerms st1 = new SearchTerms(terms1, Artifact.class);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);
        terms2.put("var", Arrays.asList("Second", "First"));

        SearchTerms st2 = new SearchTerms(terms2, Artifact.class);

        assertEquals("Search terms do not match", st1, st2);
        assertEquals("Search terms hashes do not match", st1.hashCode(), st2.hashCode());
    }

    @Test
    public void differentClass()
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("p1", "Hello");

        SearchTerms st1 = new SearchTerms(terms1, Artifact.class);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);
        terms2.put("p1", "Hello");

        SearchTerms st2 = new SearchTerms(terms2, Sample.class);

        assertNotEquals("Search terms match", st1, st2);
        assertNotEquals("Search terms hashes match", st1.hashCode(), st2.hashCode());
    }

    @Test
    public void differentValue()
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("p1", "Hello");

        SearchTerms st1 = new SearchTerms(terms1, Artifact.class);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);
        terms2.put("p1", "Goodbye");

        SearchTerms st2 = new SearchTerms(terms2, Artifact.class);

        assertNotEquals("Search terms match", st1, st2);
        assertNotEquals("Search terms hashes match", st1.hashCode(), st2.hashCode());
    }

    @Test
    public void differentParameterClass()
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("p2", 9L);

        SearchTerms st1 = new SearchTerms(terms1, Artifact.class);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);
        terms2.put("p2", 9);

        SearchTerms st2 = new SearchTerms(terms2, Artifact.class);

        assertNotEquals("Search terms match", st1, st2);
        assertNotEquals("Search terms hashes match", st1.hashCode(), st2.hashCode());
    }

    @Test
    public void differentParameters()
    {
        Map<String, Object> terms1 = new HashMap<String, Object>();
        terms1.put("p1", "Hello");
        terms1.put("p2", 9L);
        terms1.put("p3", Arrays.asList("First", "Second"));

        SearchTerms st1 = new SearchTerms(terms1, Artifact.class);

        Map<String, Object> terms2 = new HashMap<String, Object>(terms1);
        terms2.put("v1", "Hello");
        terms2.put("v2", 9L);
        terms2.put("v3", Arrays.asList("First", "Second"));

        SearchTerms st2 = new SearchTerms(terms2, Artifact.class);

        assertNotEquals("Search terms match", st1, st2);
        assertNotEquals("Search terms hashes match", st1.hashCode(), st2.hashCode());
    }

}
