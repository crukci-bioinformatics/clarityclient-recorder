package org.cruk.genologics.api.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.genologics.ri.Locatable;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("terms")
public class SearchTerms<E extends Locatable> implements Serializable
{
    private static final long serialVersionUID = 6086837837496759045L;

    @XStreamAlias("params")
    private Map<String, Object> searchTerms;

    @XStreamAlias("entity")
    private Class<E> entityClass;


    public SearchTerms(Map<String, ?> searchTerms, Class<E> entityClass)
    {
        this.entityClass = entityClass;

        this.searchTerms = new HashMap<String, Object>();
        for (Map.Entry<String, ?> entry : searchTerms.entrySet())
        {
            Object store;
            if (entry.getValue() instanceof Collection)
            {
                store = new ArrayList<Object>((Collection<?>)entry.getValue());
            }
            else if (entry.getValue().getClass().isArray())
            {
                Object[] values = (Object[])entry.getValue();
                store = new ArrayList<Object>(Arrays.asList(values));
            }
            else
            {
                store = entry.getValue();
            }

            this.searchTerms.put(entry.getKey(), store);
        }
    }

    public Map<String, Object> getSearchTerms()
    {
        return searchTerms;
    }

    public Class<E> getEntityClass()
    {
        return entityClass;
    }
}
