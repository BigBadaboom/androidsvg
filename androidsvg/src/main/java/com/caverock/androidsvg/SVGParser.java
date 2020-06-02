/*
   Copyright 2013 Paul LeBeau, Cave Rock Software Ltd.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.caverock.androidsvg;

import java.io.InputStream;

public interface SVGParser
{
    /**
     * Try to parse the stream contents to an {@link SVG} instance.
     *
     * @param is Stream to parse
     * @since 1.5
     */
    SVG parseStream(InputStream is) throws SVGParseException;

    /**
     * Tells the parser whether to allow the expansion of internal entities.
     * An example of a document containing an internal entities is:
     *
     * {@code
     * <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.0//EN" "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd" [
     *   <!ENTITY hello "Hello World!">
     * ]>
     * <svg>
     *    <text>&hello;</text>
     * </svg>
     * }
     *
     * Entities are useful in some circumstances, but SVG files that use them are quite rare.  Note
     * also that enabling entity expansion makes you vulnerable to the
     * <a href="https://en.wikipedia.org/wiki/Billion_laughs_attack">Billion Laughs Attack</a>
     *
     * Entity expansion is enabled by default.
     *
     * @param enable Set true if you want to enable entity expansion by the parser.
     * @since 1.5
     */
    SVGParser setInternalEntitiesEnabled(boolean enable);

    /**
     * Register an {@link SVGExternalFileResolver} instance that the parser should use when resolving
     * external references such as images, fonts, and CSS stylesheets.
     *
     * @param fileResolver the resolver to use.
     * @since 1.5
     */
    SVGParser setExternalFileResolver(SVGExternalFileResolver fileResolver);
}