/**
 * The Clarity Java client recorder for Clarity 6+.
 */
module org.cruk.genologics.api.recorder
{
    requires transitive org.cruk.genologics.api;

    requires transitive org.aspectj.runtime;
    requires org.slf4j;
    requires org.apache.commons.io;
    requires xstream;

    exports org.cruk.genologics.api.playback;
    exports org.cruk.genologics.api.record;
    exports org.cruk.genologics.api.search;
}
