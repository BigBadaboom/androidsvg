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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xml.sax.SAXException;

import android.util.Log;

import com.caverock.androidsvg.SVG.SvgContainer;
import com.caverock.androidsvg.SVG.SvgElementBase;
import com.caverock.androidsvg.SVGParser.TextScanner;

/**
 * A very simple CSS parser that is not very compliant with the CSS spec but
 * hopefully parses almost all the CSS we are likely to strike in an SVG file.
 * The main goals are to (a) be small, and (b) parse the CSS in a Corel Draw SVG file.
 */
public class CSSParser
{
   private static final String  TAG = "AndroidSVG CSSParser";

   private static final String  ID = "id";
   private static final String  CLASS = "class";

   private boolean  acceptableMediaType = true;


   public enum MediaType
   {
      all,
      aural,
      braille,
      embossed,
      handheld,
      print,
      projection,
      screen,
      tty,
      tv
   }

   private enum Combinator
   {
      descendant, // E F
      me,         // E.F
      child,      // E > F
      follows     // E + F
   }

   private static class SimpleSelector
   {
      public String  tag = null;       // null means "*"
      public String  attrName = null;
      //public Operator  attrOperator
      public String  attrValue = null;

      public SimpleSelector(String tag)
      {
         this.tag = tag;
      }

      public SimpleSelector  addAttrib(String attrName, String attrValue)
      {
         this.attrName = attrName;
         this.attrValue = attrValue;
         return this;
      }

      @Override
      public String toString()
      {
         StringBuffer sb = new StringBuffer();
         sb.append((tag == null) ? "*" : tag);
         if (attrName != null) {
            sb.append('[').append(attrName);
            if (attrValue != null)
               sb.append("=").append(attrValue);
            sb.append(']');
         }
         return sb.toString();
      }
   }

   public static class Rule
   {
      public List<SimpleSelector>  selector = null;
      public SVG.Style     style = null;
      
      public Rule(List<SimpleSelector> selector, SVG.Style style)
      {
         this.selector = selector;
         this.style = style;
      }
   }


   public List<Rule>  parse(SVG svg, String sheet) throws SAXException
   {
/**/debug(">>>"+sheet);
      CSSTextScanner  scan = new CSSTextScanner(sheet);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         if (scan.consume("<!--")) {
            while (!scan.empty() && !scan.consume("-->")) {
               scan.nextChar();
            }
            scan.skipWhitespace();
         }
         else if (scan.consume('@'))
            parseAtRule(scan);
         else
            svg.addCSSRules(parseRuleset(scan));
      }
      return null;
   }


   public static boolean mediaMatches(String mediaListStr, MediaType rendererMediaType) throws SAXException
   {
      CSSTextScanner  scan = new CSSTextScanner(mediaListStr);
      scan.skipWhitespace();
      List<MediaType>  mediaList = parseMediaList(scan);
      if (!scan.empty())
         throw new SAXException("Invalid @media type list");
      return mediaMatches(mediaList, rendererMediaType);
   }


   //==============================================================================


   private void  warn(String format, Object... args)
   {
      Log.w(TAG, String.format(format, args));
   }


   private void  error(String format, Object... args)
   {
      Log.e(TAG, String.format(format, args));
   }


   private void  debug(String format, Object... args)
   {
      if (LibConfig.DEBUG)
         Log.d(TAG, String.format(format, args));
   }


   //==============================================================================
   
   
   private static class CSSTextScanner extends TextScanner
   {
      public CSSTextScanner(String input)
      {
         super(input);
      }

      /*
       * Scans for a CSS 'ident' identifier.
       */
      public String  nextCSSIdentifier()
      {
         int  end = scanForIdentifier();
         if (end == position)
            return null;
         String result = input.substring(position, end);
         position = end;
         return result;
      }
         
      private int  scanForIdentifier()
      {
         if (empty())
            return position;
         int  start = position;
         int  lastValidPos = position;

         int  ch = input.charAt(position);
         if (ch == '-')
            ch = advanceChar();
         // nmstart
         if ((ch >= 'a' && ch <= 'z') || (ch == '_'))
         {
            ch = advanceChar();
            // nmchar
            while ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch == '-') || (ch == '_')) {
               ch = advanceChar();
            }
            lastValidPos = position;
         }
         position = start;
         return lastValidPos;
      }

      /*
       * Scans for a CSS 'selector'.
       */
      public SimpleSelector  nextCSSSelector()
      {
         if (empty())
            return null;

         int     start = position;

/**/debug();
         SimpleSelector  result = consume('*') ? new SimpleSelector(null) : new SimpleSelector(nextCSSIdentifier());
         String    attr = null,
                   value = null;
/**/debug();

         if (!empty())
         {
            if (consume('.'))
            {
               attr = CLASS;           // ".foo" is equivalent to *[class="foo"]
               value = nextCSSAttribValue();
               return (value != null) ? result.addAttrib(attr, value) : null;
            }
            else if (consume('#'))
            {
               attr = ID;              // "#foo" is equivalent to *[id="foo"]
               value = nextCSSAttribValue();
               return (value != null) ? result.addAttrib(attr, value) : null;
            }
            else if (consume('['))
            {
               // TODO
               return null;
            }
         }

         // If there was a tag name found, return this selector   
         if (result.tag != null)
            return result;

         // Otherwise 'fail'
         position = start;
         return null;
      }

      private String  nextCSSAttribValue()
      {
         if (empty())
            return null;

         String  result = nextQuotedString();
         if (result != null)
            return result;
         return nextCSSIdentifier();
      }

      /*
       * Scans for a CSS property value.
       */
      public String  nextCSSPropertyValue()
      {
         if (empty())
            return null;
         int  start = position;

         int  ch = input.charAt(position);
         while (ch != -1 && ch != ';' & ch != '}') {
            ch = advanceChar();
         }
         if (position > start)
            return input.substring(start, position);
         position = start;
         return null;
      }

      public void  debug()
      {
         StringBuffer sb = new StringBuffer();
         for (int i = Math.max(0, position-3); i<Math.min(position+3, input.length()); i++) {
            if (i==position)
               sb.append('>');
            sb.append(input.charAt(i));
         }   
         Log.d("TS", sb.toString());
      }
   }


   //==============================================================================


   // Returns true if 'rendererMediaType' matches one of the media types in 'mediaList'
   private static boolean mediaMatches(List<MediaType> mediaList, MediaType rendererMediaType)
   {
      for (MediaType type: mediaList) {
         if (type == MediaType.all || type == rendererMediaType)
            return true;
      }
      return false;
   }


   private static List<MediaType> parseMediaList(CSSTextScanner scan) throws SAXException
   {
      ArrayList<MediaType>  typeList = new ArrayList<MediaType>();
      while (!scan.empty()) {
         String  type = scan.nextToken(',');
         try {
            typeList.add(MediaType.valueOf(type));
         } catch (IllegalArgumentException e) {
            throw new SAXException("Invalid @media type list");
         }
         // If there is a comma, keep looping, otherwise break
         if (!scan.skipCommaWhitespace())
            break;
      }
      return typeList;
   }


   private void  parseAtRule(CSSTextScanner scan) throws SAXException
   {
      String  atKeyword = scan.nextCSSIdentifier();
      scan.skipWhitespace();
      if (atKeyword == null)
         throw new SAXException("Invalid '@' rule in <style> element");
      if (atKeyword.equals("charset")) {
         
      //} else if (atKeyword.equals("media")) {
      //} else if (atKeyword.equals("import")) {
      } else {
         // Unknown/unsupported at-rule
         debug("Ignoring @%s rule", atKeyword);
         skipAtRule(scan);
      }
      scan.skipWhitespace();
      /*
      String param1 = scan.nextToken("{;");
      if (atKeyword == null || param1 == null)
      if (scan.consume('{')) {
         
      } else if (scan.consume(';')) {
         
      }
      */
   }


   // Skip an unsupported at-rule: "ignore everything up to and including the next semicolon or block".
   private void  skipAtRule(CSSTextScanner scan)
   {
debug("skipAtRule");
      int depth = 0;
      while (!scan.empty())
      {
         int ch = scan.nextChar();
//debug("{depth=%d ch='%c'", depth, ch);
         if (ch == ';' && depth == 0)
            return;
         if (ch == '{')
            depth++;
         else if (ch == '}' && depth > 0) {
            if (--depth == 0)
               return;
         }
      }
debug("}done");
   }


   private List<Rule>  parseRuleset(CSSTextScanner scan) throws SAXException
   {
//debug("parseRuleset");
      ArrayList<Rule>  ruleset = new ArrayList<Rule>(); 
      while (!scan.empty()) {
         parseRule(ruleset, scan);
      }
/**/warn("Found %d rules", ruleset.size());
      return ruleset;
   }


   private void  parseRule(List<Rule> ruleset, CSSTextScanner scan) throws SAXException
   {
//debug("parseRule");
      List<List<SimpleSelector>>  selectors = parseSelector(scan);
//debug("selectors: %s",selectors.toString());
      if (!selectors.isEmpty())
      {
//debug("doing ruleset block");
         if (!scan.consume('{'))
            throw new SAXException("Malformed rule block in <style> element: missing '{'");
         scan.skipWhitespace();
         SVG.Style  ruleStyle = parseDeclarations(scan);
         scan.skipWhitespace();
         for (List<SimpleSelector> selector: selectors) {
            ruleset.add( new Rule(selector, ruleStyle) );
         }
      }
      else
         throw new SAXException("Malformed <style> element: no selector found");
   }


   /*
    * parse a selector (F), or selector group (E, F)
    */
   private List<List<SimpleSelector>>  parseSelector(CSSTextScanner scan) throws SAXException
   {
//debug("parseSelector");
      ArrayList<List<SimpleSelector>>  selectorGroup = new ArrayList<List<SimpleSelector>>(1);
      ArrayList<SimpleSelector>  selector = new ArrayList<SimpleSelector>();
      while (!scan.empty())
      {
         SimpleSelector  selectorPart = scan.nextCSSSelector();
         if (selectorPart != null)
         {
debug("selector: %s",selectorPart);
            selector.add(selectorPart);
            // If there is a comma, keep looping, otherwise break
            if (!scan.skipCommaWhitespace())
               break;
            selectorGroup.add(selector);
            selector = new ArrayList<SimpleSelector>();
         }
         else
            break;
      }
      if (!selector.isEmpty())
         selectorGroup.add(selector);
      return selectorGroup;
   }


   // Parse a list of
   private SVG.Style  parseDeclarations(CSSTextScanner scan) throws SAXException
   {
//debug("parseDeclarations");
      SVG.Style  ruleStyle = new SVG.Style();
      while (true)
      {
         String  propertyName = scan.nextCSSIdentifier();
         scan.skipWhitespace();
         if (!scan.consume(':'))
            break;  // Syntax error. Stop processing CSS rules.
         scan.skipWhitespace();
         String  propertyValue = scan.nextCSSPropertyValue();
         if (propertyValue == null)
            break;  // Syntax error
         scan.skipWhitespace();
         scan.consume(';');
         SVGParser.processStyleProperty(ruleStyle, propertyName, propertyValue);
/**/warn("   %s:%s",propertyName,propertyValue);
         scan.skipWhitespace();
         if (scan.consume('}'))
            return ruleStyle;
         if (scan.empty())
            break;
      }
      throw new SAXException("Malformed rule set in <style> element");
   }


   /*
    * Used by SVGParser to parse the "class" attribute.
    */
   protected static List<String>  parseClassAttribute(String val) throws SAXException
   {
      CSSTextScanner  scan = new CSSTextScanner(val);
      List<String>    classNameList = null;

      while (!scan.empty())
      {
         String  className = scan.nextCSSIdentifier();
         if (className == null)
            throw new SAXException("Invalid value for \"class\" attribute: "+val);
         if (classNameList == null)
            classNameList = new ArrayList<String>();
         classNameList.add(className);
         scan.skipWhitespace();
      }
      return classNameList;
   }


   /*
    * Used by renderer to check if a CSS rule matches the current element.
    */
   protected static boolean  ruleMatch(List<SimpleSelector> selector, Stack<SvgContainer> parentStack, SvgElementBase obj)
   {
      // Check the most common case first.
      if (selector.size() == 1 && selectorMatch(selector.get(0), obj))
         return true;
      
      // FIXME full matching
      return false;
   }


   private static boolean selectorMatch(SimpleSelector sel, SvgElementBase obj)
   {
      // Check tag name. tag==null means tag is "*" which matches everything.
      if (sel.tag != null && !sel.tag.equalsIgnoreCase(obj.getClass().getSimpleName()))
         return false;
      // If here, then tag part matched
      if (sel.attrName == null)
         return true;

      if (sel.attrName == ID)
         return sel.attrValue.equals(obj.id);
      if (sel.attrName == CLASS) {
         if (obj.classNames == null)
            return false;
         return obj.classNames.contains(sel.attrValue);
      }
      // TODO? add support for other attributes. For now we will fail the match.
      return false;
   }


}
