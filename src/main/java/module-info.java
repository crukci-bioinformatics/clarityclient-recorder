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

/**
 * The Clarity Java client recorder for Clarity 6+.
 */
module org.cruk.clarity.api.recorder
{
    requires transitive org.cruk.clarity.api;

    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.aspectj.weaver;
    requires org.slf4j;

    exports org.cruk.clarity.api.playback;
    exports org.cruk.clarity.api.playback.spring;
    exports org.cruk.clarity.api.record;
    exports org.cruk.clarity.api.record.spring;
    exports org.cruk.clarity.api.search;

    opens org.cruk.clarity.api.playback.spring to spring.core;
    opens org.cruk.clarity.api.record.spring to spring.core;
    opens org.cruk.clarity.api.search to spring.core, jakarta.xml.bind;
}
