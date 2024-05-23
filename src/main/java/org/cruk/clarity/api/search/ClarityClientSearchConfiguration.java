package org.cruk.clarity.api.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cruk.clarity.api.ClarityAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import jakarta.xml.bind.Marshaller;

@Configuration
public class ClarityClientSearchConfiguration
{
    public ClarityClientSearchConfiguration()
    {
    }

    @Bean
    public Jaxb2Marshaller claritySearchMarshaller()
    {
        Module module = ClarityAPI.class.getModule();
        List<String> packages = module.getPackages().stream()
                    .filter(p -> p.startsWith("com.genologics.ri"))
                    .collect(Collectors.toList());
        packages.add(Search.class.getPackage().getName());

        String[] packageArray = packages.toArray(new String[packages.size()]);

        Map<String, Object> marshallerProps = new HashMap<>();
        marshallerProps.put(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshallerProps.put(Marshaller.JAXB_ENCODING, "UTF-8");

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(packageArray);
        marshaller.setMarshallerProperties(marshallerProps);
        return marshaller;
    }
}
