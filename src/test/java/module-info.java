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
open module org.cruk.clarity.api.recorder
{
    requires transitive org.cruk.clarity.api;

    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.aspectj.weaver;
    requires org.slf4j;

    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;

    requires static org.junit.jupiter;
    requires static org.junit.jupiter.api;
    requires static org.junit.jupiter.engine;
    requires static org.junit.jupiter.params;
    requires static org.glassfish.jaxb.core;
    requires static org.glassfish.jaxb.runtime;
}
