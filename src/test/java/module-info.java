/**
 * The Clarity Java client recorder for Clarity 6+.
 */
open module org.cruk.clarity.api.recorder
{
    requires transitive org.cruk.clarity.api;

    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.aspectj.weaver;
    requires org.slf4j;

    requires org.apache.httpcomponents.core5.httpcore5;

    requires static org.junit.jupiter;
    requires static org.junit.jupiter.api;
    requires static org.junit.jupiter.engine;
    requires static org.junit.jupiter.params;
    requires static org.glassfish.jaxb.core;
    requires static org.glassfish.jaxb.runtime;
}
