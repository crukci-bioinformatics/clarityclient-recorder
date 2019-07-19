package org.cruk.genologics.api.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.genologics.ri.LimsLink;
import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("search")
public class Search<E extends Locatable> implements Serializable
{
    public static final String SEARCH_FILENAME = "searches.xml";

    private static final long serialVersionUID = -6611443224550823943L;

    @XStreamAlias("terms")
    private SearchTerms<E> searchTerms;

    private List<LimsLink<E>> results;


    public Search(Map<String, ?> searchTerms, Class<E> entityClass)
    {
        this.searchTerms = new SearchTerms<E>(searchTerms, entityClass);
    }

    public List<LimsLink<E>> getResults()
    {
        return results;
    }

    public void setResults(List<LimsLink<E>> results)
    {
        this.results = results;
    }

    public SearchTerms<E> getSearchTerms()
    {
        return searchTerms;
    }

}
