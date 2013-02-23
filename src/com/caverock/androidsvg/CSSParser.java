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

   private enum AttribOp
   {
      EXISTS,     // *[foo]
      EQUALS,     // *[foo=bar]
      INCLUDES,   // *[foo~=bar]
      DASHMATCH,  // *[foo|=bar]
   }

   public static class Attrib
   {
      public String    name = null;
      public AttribOp  operation;
      public String    value = null;
      
      public Attrib(String name, AttribOp op, String value)
      {
         this.name = name;
         this.operation = op;
         this.value = value;
      }
   }

   private static class SimpleSelector
   {
      public String  tag = null;       // null means "*"
      public List<Attrib>  attribs = null;
      public List<String>  pseudos = null;

      public SimpleSelector(String tag)
      {
         this.tag = tag;
      }

      public void  addAttrib(String attrName, AttribOp op, String attrValue)
      {
         if (attribs == null)
            attribs = new ArrayList<Attrib>();
         attribs.add(new Attrib(attrName, op, attrValue));
      }

      public void  addPseudo(String pseudo)
      {
         if (pseudos == null)
            pseudos = new ArrayList<String>();
         pseudos.add(pseudo);
      }

      @Override
      public String toString()
      {
         StringBuffer sb = new StringBuffer();
         sb.append((tag == null) ? "*" : tag);
         if (attribs != null) {
            for (Attrib attr: attribs) {
               sb.append('[').append(attr.name);
               switch(attr.operation) {
                  case EQUALS: sb.append('=').append(attr.value); break;
                  case INCLUDES: sb.append("~=").append(attr.value); break;
                  case DASHMATCH: sb.append("|=").append(attr.value); break;
                  default: break;
               }
               sb.append(']');
            }
         }
         if (pseudos != null) {
            for (String pseu: pseudos)
               sb.append(':').append(pseu);
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
      CSSTextScanner  scan = new CSSTextScanner(sheet);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         if (scan.consume("<!--"))
            continue;
         if (scan.consume("-->"))
            continue;

         if (scan.consume('@'))
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
         super(input.replaceAll("/\\*.*?\\*/", ""));  // strip all block comments
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
         if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch == '_'))
         {
            ch = advanceChar();
            // nmchar
            while ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch == '-') || (ch == '_')) {
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
      public SimpleSelector  nextCSSSelector() throws SAXException
      {
         if (empty())
            return null;

         int     start = position;

         SimpleSelector  result = null;
         if (consume('*')) {
            result = new SimpleSelector(null);
         } else {
            String tag = nextCSSIdentifier();
            if (tag != null)
               result = new SimpleSelector(tag);
         }

         while (!empty())
         {
            if (consume('.'))
            {
               // ".foo" is equivalent to *[class="foo"]
               if (result == null)
                  result = new SimpleSelector(null);
               String  value = nextCSSIdentifier();
               if (value == null) {
                  position = start;
                  return null;
               }
               result.addAttrib(CLASS, AttribOp.EQUALS, value);
               continue;
            }

            if (consume('#'))
            {
               // "#foo" is equivalent to *[id="foo"]
               if (result == null)
                  result = new SimpleSelector(null);
               String  value = nextCSSIdentifier();
               if (value == null) {
                  position = start;
                  return null;
               }
               result.addAttrib(ID, AttribOp.EQUALS, value);
            }

            if (result == null)
               break;

            // Now check for attribute selection and pseudo selectors   
            if (consume('['))
            {
               skipWhitespace();
               String  attrName = nextCSSIdentifier();
               String  attrValue = null;
               if (attrName == null)
                  throw new SAXException("Invalid attribute selector in <style> element");
               skipWhitespace();
               AttribOp  op = null;
               if (consume('='))
                  op = AttribOp.EQUALS;
               else if (consume("~="))
                  op = AttribOp.INCLUDES;
               else if (consume("|="))
                  op = AttribOp.DASHMATCH;
               if (op != null) {
                  skipWhitespace();
                  attrValue = nextCSSAttribValue();
                  if (attrValue == null)
                     throw new SAXException("Invalid attribute selector in <style> element");
                  skipWhitespace();
               }
               if (!consume(']'))
                  throw new SAXException("Invalid attribute selector in <style> element");
               result.addAttrib(attrName, (op == null) ? AttribOp.EXISTS : op, attrValue);
               continue;
            }

            if (consume(':'))
            {
               // skip pseudo
               int  pseudoStart = position;
               if (nextCSSIdentifier() != null) {
                  if (consume('(')) {
                     skipWhitespace();
                     if (nextCSSIdentifier() != null) {
                        skipWhitespace();
                        if (!consume(')')) {
                           position = pseudoStart - 1;
                           break;
                        }
                     }
                  }
                  result.addPseudo(input.substring(pseudoStart, position));
               }
            }

            break;
         }

         if (result != null)
            return result;

         // Otherwise 'fail'
         position = start;
         return null;
      }

      /*
       * The value (bar) part of "[foo="bar"]".
       */
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
         int  lastValidPos = position;

         int  ch = input.charAt(position);
         while (ch != -1 && ch != ';' && ch != '}' && !isEOL(ch)) {
            if (!isWhitespace(ch))  // don't include an spaces at the end
               lastValidPos = position + 1;
            ch = advanceChar();
         }
         if (position > start)
            return input.substring(start, lastValidPos);
         position = start;
         return null;
      }


      /* for debugging - returns the characters surrounding 'position'. */
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
/**/if (obj.id!= null && obj.id.equals("right-eye")) Log.w("**** sM", ""+sel);
//Log.w("sM", "test: "+sel+"  class="+obj.classNames);
      // Check tag name. tag==null means tag is "*" which matches everything.
      if (sel.tag != null && !sel.tag.equalsIgnoreCase(obj.getClass().getSimpleName()))
         return false;
      // If here, then tag part matched

      // Check the attributes
      if (sel.attribs != null)
      {
         for (Attrib attr: sel.attribs)
         {
//Log.w("sM", "====: "+attr.name);
            if (attr.name == ID)
            {
//Log.w("sM", "iiiii: "+attr.value);
               if (!attr.value.equals(obj.id))
                  return false;
            }
            else if (attr.name == CLASS)
            {
//Log.w("sM", ">>>>: "+attr.value);
               if (obj.classNames == null)
                  return false;
               if (!obj.classNames.contains(attr.value))
                  return false;
            }
            else
            {
               // Other attribute selector not yet supported
               return false;
            }
         }
      }

      // Check the pseudo classes
      // Not yet supported - so fail match
/**/if (obj.id!= null && obj.id.equals("nose")) Log.w("sM", "NOSE "+sel);
      if (sel.pseudos != null)
         return false;

/**/Log.w("sM", "return true");
      // If w reached this point, the selector matched
      return true;
   }


}
