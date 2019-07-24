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

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cruk.genologics.api.GenologicsAPI;

import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A class holding both the parameters of a search and the links returned
 * from that search.
 *
 * @param <E> The type of object the search is for.
 *
 * @see GenologicsAPI#find(Map, Class)
 */
@XStreamAlias("search")
public class Search<E extends Locatable> implements Serializable
{
    /**
     * The default name for search files. The parameter should be filled
     * with the hexadecimal value of the search term's hash code.
     *
     * @see #getSearchFileName(SearchTerms)
     */
    public static final String SEARCH_FILE_PATTERN = "search_{0}.xml";

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = -6611443224550823943L;

    /**
     * The search terms used in this search.
     */
    @XStreamAlias("terms")
    private SearchTerms searchTerms;

    /**
     * The results of the search.
     */
    private List<LimsLink<E>> results;


    /**
     * Constructor that takes the parameters from a call to the API's
     * {@code find} method.
     *
     * @param searchTerms The search parameters.
     * @param entityClass The type of object being searched for.
     */
    public Search(Map<String, ?> searchTerms, Class<E> entityClass)
    {
        this(new SearchTerms(searchTerms, entityClass));
    }

    /**
     * Constructor that accepts an already created {@code SearchTerms} object.
     *
     * @param searchTerms The search terms.
     *
     * @throws IllegalArgumentException if {@code searchTerms} is null.
     */
    public Search(SearchTerms searchTerms)
    {
        if (searchTerms == null)
        {
            throw new IllegalArgumentException();
        }
        this.searchTerms = searchTerms;
    }

    /**
     * Get the parameters of this search.
     *
     * @return The search terms.
     */
    public SearchTerms getSearchTerms()
    {
        return searchTerms;
    }

    /**
     * Get the results of the search.
     *
     * @return A list of links to the entities searched for.
     */
    public List<LimsLink<E>> getResults()
    {
        return results;
    }

    /**
     * Set the results of the search.
     *
     * @param results The links that are the result of the search.
     */
    public void setResults(List<LimsLink<E>> results)
    {
        this.results = results;
    }

    /**
     * Get a human readable representation of this object. Shows the search terms
     * and, if results are set, the number of links in the results.
     *
     * @return A printable representation of this object.
     */
    @Override
    public String toString()
    {
        ToStringBuilder b = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        b.append("searchTerms", searchTerms);

        if (results != null)
        {
            b.append("#results", results.size());
        }

        return b.toString();
    }


    /**
     * Get the name of the file that will store the result of this search.
     *
     * @return The name of the file the search will be stored in.
     */
    public String getSearchFileName()
    {
        return getSearchFileName(searchTerms);
    }

    /**
     * Get the name of the file that will store the result of the given search.
     * Convenience method here meaning all calls for record and playback will be
     * consistent.
     *
     * @param terms The search terms.
     *
     * @return The name of the file the search will be stored in.
     */
    public static String getSearchFileName(SearchTerms terms)
    {
        return MessageFormat.format(SEARCH_FILE_PATTERN, Integer.toHexString(terms.hashCode()));
    }
}
