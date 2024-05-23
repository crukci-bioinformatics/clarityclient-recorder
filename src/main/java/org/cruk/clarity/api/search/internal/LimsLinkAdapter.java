package org.cruk.clarity.api.search.internal;

import com.genologics.ri.LimsLink;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class LimsLinkAdapter extends XmlAdapter<Object, LimsLink<?>>
{
    public LimsLinkAdapter()
    {
    }

    @Override
    public LimsLink<?> unmarshal(Object v) throws Exception
    {
        return LimsLink.class.cast(v);
    }

    @Override
    public Object marshal(LimsLink<?> v) throws Exception
    {
        return v;
    }

}
