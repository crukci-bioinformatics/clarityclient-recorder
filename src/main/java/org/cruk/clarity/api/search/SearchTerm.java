package org.cruk.clarity.api.search;

import static org.apache.commons.lang3.StringUtils.join;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.util.CollectionUtils;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "term", propOrder = { "param", "values" })
public class SearchTerm implements Serializable
{
    private static final long serialVersionUID = -8406721186516589781L;

    @XmlElement(name = "param", required = true)
    private String param;

    @XmlElement(name = "value")
    private List<String> values;

    SearchTerm()
    {
    }

    public SearchTerm(String param)
    {
        setParam(param);
    }

    public SearchTerm(String param, Collection<? extends Object> values)
    {
        setParam(param);
        setValues(values);
    }

    public String getParam()
    {
        return param;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public List<String> getValues()
    {
        if (values == null)
        {
            values = new ArrayList<>();
        }
        return values;
    }

    public void setValues(Stream<? extends Object> values)
    {
        this.values = values.filter(i -> i != null).map(i -> i.toString()).collect(Collectors.toList());
    }

    public void setValues(Collection<? extends Object> values)
    {
        setValues(CollectionUtils.isEmpty(values) ? Stream.empty() : values.stream());
    }

    public void setValues(Object[] values)
    {
        setValues(Stream.of(values));
    }

    public void setValue(Object value)
    {
        setValues(Stream.of(value));
    }

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

    private int size()
    {
        return values == null ? 0 : values.size();
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

    void toString(ToStringBuilder b)
    {
        b.append(param, values == null ? null : join(values, ","));
    }
}
