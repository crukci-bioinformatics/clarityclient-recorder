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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.join;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cruk.clarity.api.ClarityAPI;
import org.springframework.util.CollectionUtils;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Class recording an individual parameter and its value or values of a
 * call to the API's {@code find} method.
 *
 * @see ClarityAPI#find(Map, Class)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "term", propOrder = { "param", "values" })
public class SearchTerm implements Serializable
{
    private static final long serialVersionUID = -8406721186516589781L;

    /**
     * The parameter name.
     */
    @XmlElement(name = "param", required = true)
    private String param;

    /**
     * The parameter's values to search for.
     */
    @XmlElement(name = "value")
    private List<String> values;


    /**
     * Package level default constructor for JAXB.
     */
    SearchTerm()
    {
    }

    /**
     * Create a search term with no values.
     *
     * @param param The parameter name.
     *
     * @throws IllegalArgumentException if {@code param} is null or empty.
     */
    public SearchTerm(String param)
    {
        setParam(param);
    }

    /**
     * Create a search term with the given values.
     *
     * @param param The parameter name.
     * @param values A collection of values to search for with this parameter.
     *
     * @throws IllegalArgumentException if {@code param} is null or empty.
     */
    public SearchTerm(String param, Collection<? extends Object> values)
    {
        setParam(param);
        setValues(values);
    }

    /**
     * Get the parameter name.
     *
     * @return The parameter name.
     */
    public String getParam()
    {
        return param;
    }

    /**
     * Set the parameter name.
     *
     * @param param The parameter name.
     *
     * @throws IllegalArgumentException if {@code param} is null or empty.
     */
    public void setParam(String param)
    {
        if (isBlank(param))
        {
            throw new IllegalArgumentException("param cannot be set to null or empty.");
        }
        this.param = param;
    }

    /**
     * Get the values to search for for this parameter.
     *
     * @return A list of values.
     */
    public List<String> getValues()
    {
        if (values == null)
        {
            values = new ArrayList<>();
        }
        return values;
    }

    /**
     * Set the values to search for from a stream.
     *
     * @param values The values to search for. Any null or empty values are filtered out.
     */
    public void setValues(Stream<? extends Object> values)
    {
        this.values = values.filter(i -> i != null).map(i -> i.toString()).filter(i -> isNotEmpty(i)).distinct().collect(Collectors.toList());
    }

    /**
     * Set the values to search for from a collection.
     *
     * @param values The values to search for. Any null or empty values are filtered out.
     */
    public void setValues(Collection<? extends Object> values)
    {
        setValues(CollectionUtils.isEmpty(values) ? Stream.empty() : values.stream());
    }

    /**
     * Set the values to search for from an array.
     *
     * @param values The values to search for. Any null or empty values are filtered out.
     */
    public void setValues(Object[] values)
    {
        setValues(Stream.of(values));
    }

    /**
     * Set the single value to search for.
     *
     * @param value The value.
     */
    public void setValue(Object value)
    {
        setValues(Stream.of(value));
    }

    /**
     * Override of hash code. The hash should be based on the parameter name and
     * the values. The order of values is not important.
     *
     * @return A hash code for this search term.
     */
    @Override
    public int hashCode()
    {
        // HashCodeBuilder is fussy about the order of addition. For this class,
        // we don't want to deal with that. If a collection has the same values
        // in a different order, it should be considered the same.

        int hash = param.hashCode();

        if (values != null)
        {
            for (String v : values)
            {
                hash ^= v.hashCode();
            }
        }

        return hash;
    }

    /**
     * A helper method to return a null-safe size for the values.
     *
     * @return The number of values.
     */
    private int size()
    {
        return values == null ? 0 : values.size();
    }

    /**
     * Test whether the given object is equal to this SearchTerm. It will
     * be if it is also a SearchTerm object that has the same parameter name
     * with the same search values (order unimportant).
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

                SearchTerm other = (SearchTerm)obj;

                b.append(param, other.param);
                b.append(size(), other.size());
                if (values != null && other.values != null)
                {
                    var iter = values.iterator();
                    while (b.isEquals() && iter.hasNext())
                    {
                        // Don't want the order of the values to matter.
                        b.append(true, other.values.contains(iter.next()));
                    }
                }

                equal = b.isEquals();
            }
        }
        return equal;
    }

    /**
     * Get a human readable representation of this object. Gives the parameter name and values.
     *
     * @return A printable representation of this object.
     */
    @Override
    public String toString()
    {
        ToStringBuilder b = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        b.append("param", param);
        if (values != null)
        {
            b.append("values", join(values, ','));
        }
        return b.toString();
    }

    /**
     * Helper to {@link SearchTerms#toString()} to add a parameter and its values to the
     * existing ToStringBuilder.
     *
     * @param b The ToStringBuilder to add to.
     */
    void toString(ToStringBuilder b)
    {
        b.append(param, values == null ? null : join(values, ","));
    }
}
