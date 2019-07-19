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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
        // HashCodeBuilder is fussy about the order of addition. For this class,
        // we don't want to deal with that. If a collection has the same values
        // in a different order, it should be considered the same.

        int hash = entityClass.hashCode();

        for (Map.Entry<String, ?> entry : searchTerms.entrySet())
        {
            hash ^= entry.getKey().hashCode();

            // In the copy of the collection, we only have collections or simple values. No arrays.

            if (entry.getValue() instanceof Collection)
            {
                Collection<?> c = (Collection<?>)entry.getValue();
                for (Object v : c)
                {
                    if (v != null)
                    {
                        hash ^= v.getClass().hashCode();
                        hash ^= v.hashCode();
                    }
                }
            }
            else
            {
                if (entry.getValue() != null)
                {
                    Object v = entry.getValue();
                    hash ^= v.getClass().hashCode();
                    hash ^= v.hashCode();
                }
            }
        }

        return hash;
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

                    b.append(myValue != null, otherValue != null);
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

    @Override
    public String toString()
    {
        ToStringBuilder b = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        b.append("entityClass", ClassUtils.getShortClassName(entityClass));

        for (Map.Entry<String, ?> entry : searchTerms.entrySet())
        {
            if (entry.getValue() instanceof Collection)
            {
                b.append(entry.getKey(), StringUtils.join((Collection<?>)entry.getValue(), ","));
            }
            else
            {
                b.append(entry.getKey(), entry.getValue());
            }
        }

        return b.toString();
    }
}
