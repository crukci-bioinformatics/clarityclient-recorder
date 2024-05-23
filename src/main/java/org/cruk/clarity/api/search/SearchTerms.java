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

import static org.apache.commons.lang3.ClassUtils.getShortClassName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cruk.clarity.api.ClarityAPI;

import com.genologics.ri.Locatable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Class recording the parameters and entity class of a call to the API's
 * {@code find} method.
 *
 * @param <E> The type of object the search terms are for.
 *
 * @see ClarityAPI#find(Map, Class)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "terms", propOrder = { "searchTerms", "entityClass" })
public class SearchTerms<E extends Locatable> implements Serializable
{
    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 6086837837496759045L;

    /**
     * The search terms (parameters) of the search.
     */
    @XmlElement(name = "term")
    private List<SearchTerm> searchTerms;

    /**
     * The class of the objects being searched for.
     */
    @XmlElement(name = "entity")
    private Class<E> entityClass;


    /**
     * Package level default constructor for JAXB.
     */
    SearchTerms()
    {
    }

    /**
     * Creates a new SearchTerms object with the given values (as have
     * been passed to find).
     *
     * @param searchTerms The search parameters.
     * @param entityClass The type of object being searched for.
     */
    public SearchTerms(Map<String, ?> searchTerms, Class<E> entityClass)
    {
        this.entityClass = entityClass;

        this.searchTerms = new ArrayList<>(searchTerms.size());
        for (Map.Entry<String, ?> entry : searchTerms.entrySet())
        {
            SearchTerm store = new SearchTerm(entry.getKey());
            if (entry.getValue() instanceof Collection<?> collection)
            {
                store.setValues(collection);
            }
            else if (entry.getValue().getClass().isArray())
            {
                Object[] values = (Object[])entry.getValue();
                store.setValues(values);
            }
            else
            {
                store.setValue(entry.getValue());
            }

            this.searchTerms.add(store);
        }
    }

    /**
     * Get the parameters of the search.
     *
     * @return A list of individual search terms.
     */
    public List<SearchTerm> getSearchTerms()
    {
        return searchTerms;
    }

    /**
     * Get the class being searched for.
     *
     * @return The entity class.
     */
    public Class<E> getEntityClass()
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
        // Note: don't get the hash from the class object. Use its name. Class
        // uses the system identity hash code.

        int hash = entityClass.getName().hashCode();

        for (SearchTerm term : searchTerms)
        {
            hash ^= term.hashCode();
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

                SearchTerms<?> other = (SearchTerms<?>)obj;

                b.append(entityClass, other.entityClass);
                b.append(searchTerms.size(), other.searchTerms.size());

                var iter = searchTerms.iterator();
                while (b.isEquals() && iter.hasNext())
                {
                    // Don't want the order of the terms to matter.
                    b.append(true, other.searchTerms.contains(iter.next()));
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
        b.append("entityClass", getShortClassName(entityClass));

        for (SearchTerm term : searchTerms)
        {
            term.toString(b);
        }

        return b.toString();
    }
}
