/**
 * The Clarity Java client recorder for Clarity 6+.
 */
module org.cruk.clarity.api.recorder
{
    requires transitive org.cruk.clarity.api;

    requires transitive org.aspectj.runtime;
    requires org.slf4j;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires xstream;

    exports org.cruk.clarity.api.playback;
    exports org.cruk.clarity.api.record;
    exports org.cruk.clarity.api.search;

    opens org.cruk.clarity.api.search to xstream;
}
