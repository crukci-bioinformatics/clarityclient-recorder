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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cruk.genologics.api.GenologicsAPI;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Class recording the parameters and entity class of a call to the API's
 * {@code find} method.
 *
 * @see GenologicsAPI#find(Map, Class)
 */
@XStreamAlias("terms")
public class SearchTerms implements Serializable
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 6086837837496759045L;

    /**
     * The search terms (parameters) of the search.
     */
    @XStreamAlias("params")
    private Map<String, Object> searchTerms;

    /**
     * The class of the objects being searched for.
     */
    @XStreamAlias("entity")
    private Class<?> entityClass;


    /**
     * Creates a new SearchTerms object with the given values (as have
     * been passed to find).
     *
     * @param searchTerms The search parameters.
     * @param entityClass The type of object being searched for.
     */
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

    /**
     * Get the parameters of the search.
     *
     * @return A map of parameter name to value or values.
     */
    public Map<String, Object> getSearchTerms()
    {
        return searchTerms;
    }

    /**
     * Get the class being searched for.
     *
     * @return The entity class.
     * @return
     */
    public Class<?> getEntityClass()
    {
        return entityClass;
    }

    /**
     * Override of hash code. The hash should be based on the class being searched
     * for along with the names of the parameters and their individual types and
     * values. The order of values in parameter values is not important.
     *
     * @return A hash code for these search terms.
     */
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

    /**
     * Test whether the given object is equal to these SearchTerms. It will
     * be if it is also a SearchTerms object that records a search for the
     * same type of object as this; if it has the same number of parameters
     * with the same names; and if all the values of the parameters are the
     * same (order unimportant for parameters that are collections).
     *
     * @param obj The object to compare to.
     *
     * @return True if this object is value equal to {@code obj}, false if not.
     */
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

    /**
     * Get a human readable representation of this object. Gives the type of objects
     * being searched for and the parameters used in the search.
     *
     * @return A printable representation of this object.
     */
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
