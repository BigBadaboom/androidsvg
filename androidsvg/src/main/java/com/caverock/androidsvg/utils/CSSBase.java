package com.caverock.androidsvg.utils;

/*
    This is just a link to CSSParser class. As CSSParser is package-protected and we don't want it
    to leak as a public API, we just gaining access through this inheritance.
 */
public class CSSBase {
    protected CSSParser.Ruleset cssRuleset;

    protected CSSBase(String css) {
        this.cssRuleset = new CSSParser(CSSParser.Source.RenderOptions, null).parse(css);
    }
}
