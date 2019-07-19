package org.cruk.genologics.api.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("terms")
public class SearchTerms implements Serializable
{
    private static final long serialVersionUID = 6086837837496759045L;

    @XStreamAlias("params")
    private Map<String, Object> searchTerms;

    @XStreamAlias("entity")
    private Class<?> entityClass;


    public SearchTerms(Map<String, ?> searchTerms, Class<?> entityClass)
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

    public Class<?> getEntityClass()
    {
        return entityClass;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder b = new HashCodeBuilder();
        b.append(entityClass);

        for (Map.Entry<String, ?> entry : searchTerms.entrySet())
        {
            b.append(entry.getKey());

            // In the copy of the collection, we only have collections or simple values.

            if (entry.getValue() instanceof Collection)
            {
                Collection<?> c = (Collection<?>)entry.getValue();
                for (Object v : c)
                {
                    b.append(v);
                }
            }
            else
            {
                b.append(entry.getValue());
            }
        }

        return b.toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equal = obj == this;
        if (!equal)
        {
            if (getClass().equals(obj.getClass()))
            {
                EqualsBuilder b = new EqualsBuilder();

                SearchTerms other = (SearchTerms)obj;

                b.append(entityClass, other.entityClass);
                b.append(searchTerms.size(), other.searchTerms.size());

                Iterator<String> meIter = searchTerms.keySet().iterator();
                while (meIter.hasNext() && b.isEquals())
                {
                    String term = meIter.next();

                    Object myValue = searchTerms.get(term);
                    Object otherValue = other.searchTerms.get(term);

                    b.append(true, otherValue != null);
                    b.append(myValue instanceof Collection, otherValue instanceof Collection);

                    if (b.isEquals())
                    {
                        if (myValue instanceof Collection)
                        {
                            Collection<?> myCollection = (Collection<?>)myValue;
                            Collection<?> otherCollection = (Collection<?>)otherValue;

                            b.append(myCollection.size(), otherCollection.size());

                            // Don't want the order of the values to matter.
                            for (Object value : myCollection)
                            {
                                b.append(true, otherCollection.contains(value));
                            }
                        }
                        else
                        {
                            b.append(myValue, otherValue);
                        }
                    }
                }

                equal = b.isEquals();
            }
        }
        return equal;
    }
}
