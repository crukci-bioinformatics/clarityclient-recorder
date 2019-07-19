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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("search")
public class Search<E extends Locatable> implements Serializable
{
    public static final String DEFAULT_SEARCH_DIRECTORY = "searches";

    private static final long serialVersionUID = -6611443224550823943L;

    @XStreamAlias("terms")
    private SearchTerms searchTerms;

    private List<LimsLink<E>> results;


    public Search(Map<String, ?> searchTerms, Class<E> entityClass)
    {
        this.searchTerms = new SearchTerms(searchTerms, entityClass);
    }

    public List<LimsLink<E>> getResults()
    {
        return results;
    }

    public void setResults(List<LimsLink<E>> results)
    {
        this.results = results;
    }

    public SearchTerms getSearchTerms()
    {
        return searchTerms;
    }

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
}
