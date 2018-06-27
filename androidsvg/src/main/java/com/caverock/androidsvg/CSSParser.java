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

import android.util.Log;

import com.caverock.androidsvg.SVG.SvgContainer;
import com.caverock.androidsvg.SVG.SvgElementBase;
import com.caverock.androidsvg.SVG.SvgObject;
import com.caverock.androidsvg.SVGParser.TextScanner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * A very simple CSS parser that is not entirely compliant with the CSS spec but
 * hopefully parses almost all the CSS we are likely to strike in an SVG file.
 */
public class CSSParser
{
   private static final String  TAG = "AndroidSVG CSSParser";

   static final String  CSS_MIME_TYPE = "text/css";

   private static final String  ID = "id";
   private static final String  CLASS = "class";

   private MediaType  deviceMediaType = null;
   private Source     source = null;    // Where these rules came from (Parser or RenderOptions)

   private boolean  inMediaRule = false;


   @SuppressWarnings("unused")
   enum MediaType
   {
      all,
      aural,       // deprecated
      braille,     // deprecated
      embossed,    // deprecated
      handheld,    // deprecated
      print,
      projection,  // deprecated
      screen,
      speech,
      tty,         // deprecated
      tv           // deprecated
   }

   private enum Combinator
   {
      DESCENDANT,  // E F
      CHILD,       // E > F
      FOLLOWS      // E + F
   }

   private enum AttribOp
   {
      EXISTS,     // *[foo]
      EQUALS,     // *[foo=bar]
      INCLUDES,   // *[foo~=bar]
      DASHMATCH,  // *[foo|=bar]
   }

   private static class Attrib
   {
      final public String    name;
      final        AttribOp  operation;
      final public String    value;
      
      Attrib(String name, AttribOp op, String value)
      {
         this.name = name;
         this.operation = op;
         this.value = value;
      }
   }

   private static class SimpleSelector
   {
      Combinator    combinator = null;
      String        tag = null;       // null means "*"
      List<Attrib>  attribs = null;
      List<String>  pseudos = null;

      SimpleSelector(Combinator combinator, String tag)
      {
         this.combinator = (combinator != null) ? combinator : Combinator.DESCENDANT;
         this.tag = tag;
      }

      void  addAttrib(String attrName, AttribOp op, String attrValue)
      {
         if (attribs == null)
            attribs = new ArrayList<>();
         attribs.add(new Attrib(attrName, op, attrValue));
      }

      void  addPseudo(String pseudo)
      {
         if (pseudos == null)
            pseudos = new ArrayList<>();
         pseudos.add(pseudo);
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         if (combinator == Combinator.CHILD)
            sb.append("> ");
         else if (combinator == Combinator.FOLLOWS)
            sb.append("+ ");
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

   static class  Ruleset
   {
      private List<Rule>  rules = null;

      // Add a rule to the ruleset. The position at which it is inserted is determined by its specificity value.
      void  add(Rule rule)
      {
         if (this.rules == null)
            this.rules = new ArrayList<>();
         for (int i = 0; i < rules.size(); i++)
         {
            Rule  nextRule = rules.get(i);
            if (nextRule.selector.specificity > rule.selector.specificity) {
               rules.add(i, rule);
               return;
            }
         }
         rules.add(rule);
      }

      void  addAll(Ruleset rules)
      {
         if (rules.rules == null)
            return;
         if (this.rules == null)
            this.rules = new ArrayList<>(rules.rules.size());
         for (Rule rule: rules.rules) {
            this.add(rule);
         }
      }

      List<Rule>  getRules()
      {
         return this.rules;
      }

      boolean  isEmpty()
      {
         return this.rules == null || this.rules.isEmpty();
      }

      /*
       * Remove all rules that were addres from a give Source.
       */
      void  removeFromSource(Source sourceToBeRemoved)
      {
         if (this.rules == null)
            return;
         Iterator<Rule> iter = this.rules.iterator();
         while (iter.hasNext()) {
            if (iter.next().source == sourceToBeRemoved)
               iter.remove();
         }
      }

      @Override
      public String toString()
      {
         if (rules == null)
            return "";
         StringBuilder sb = new StringBuilder();
         for (Rule rule: rules)
            sb.append(rule.toString()).append('\n');
         return sb.toString();
      }
   }


   static enum  Source
   {
      Parser,
      RenderOptions
   }


   static class  Rule
   {
      Selector   selector = null;
      SVG.Style  style = null;
      Source     source;
      
      Rule(Selector selector, SVG.Style style, Source source)
      {
         this.selector = selector;
         this.style = style;
         this.source = source;
      }

      @Override
      public String toString()
      {
         return String.valueOf(selector) + " {}";
      }
   }


   private static class Selector
   {
      List<SimpleSelector>  selector = null;
      int                   specificity = 0;
      
      void  add(SimpleSelector part)
      {
         if (this.selector == null)
            this.selector = new ArrayList<>();
         this.selector.add(part);
      }

      int size()
      {
         return (this.selector == null) ? 0 : this.selector.size();
      }

      SimpleSelector get(int i)
      {
         return this.selector.get(i);
      }

      boolean isEmpty()
      {
         return (this.selector == null) || this.selector.isEmpty();
      }

      // Methods for accumulating a specificity value as SimpleSelector entries are added.
      void  addedIdAttribute()
      {
         specificity += 10000;
      }

      void  addedAttributeOrPseudo()
      {
         specificity += 100;
      }

      void  addedElement()
      {
         specificity += 1;
      }

      @Override
      public String toString()
      {
         StringBuilder  sb = new StringBuilder();
         for (SimpleSelector sel: selector)
            sb.append(sel).append(' ');
         return sb.append('(').append(specificity).append(')').toString();
      }
   }


   //===========================================================================================


   
   CSSParser()
   {
      this(MediaType.screen, Source.Parser);
   }


   CSSParser(Source source)
   {
      this(MediaType.screen, source);
   }


   CSSParser(MediaType rendererMediaType, Source source)
   {
      this.deviceMediaType = rendererMediaType;
      this.source = source;
   }


   Ruleset  parse(String sheet)
   {
      CSSTextScanner  scan = new CSSTextScanner(sheet);
      scan.skipWhitespace();

      return parseRuleset(scan);
   }


   static boolean mediaMatches(String mediaListStr, MediaType rendererMediaType)
   {
      CSSTextScanner  scan = new CSSTextScanner(mediaListStr);
      scan.skipWhitespace();
      List<MediaType>  mediaList = parseMediaList(scan);
      return mediaMatches(mediaList, rendererMediaType);
   }


   //==============================================================================


   private static void  warn(String format, Object... args)
   {
      Log.w(TAG, String.format(format, args));
   }


   /*
   private static void  error(String format, Object... args)
   {
      Log.e(TAG, String.format(format, args));
   }


   private static void  debug(String format, Object... args)
   {
      if (LibConfig.DEBUG)
         Log.d(TAG, String.format(format, args));
   }
   */


   //==============================================================================
   
   
   private static class CSSTextScanner extends TextScanner
   {
      CSSTextScanner(String input)
      {
         super(input.replaceAll("(?s)/\\*.*?\\*/", ""));  // strip all block comments
      }

      /*
       * Scans for a CSS 'ident' identifier.
       */
      String  nextIdentifier()
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
       * Scans for a CSS 'simple selector'.
       * Returns true if it found one.
       * Returns false if there was an error or the input is empty.
       */
      boolean  nextSimpleSelector(Selector selector) throws CSSParseException
      {
         if (empty())
            return false;

         int             start = position;
         Combinator      combinator = null;
         SimpleSelector  selectorPart = null;

         if (!selector.isEmpty())
         {
            if (consume('>')) {
               combinator = Combinator.CHILD;
               skipWhitespace();
            } else if (consume('+')) {
               combinator = Combinator.FOLLOWS;
               skipWhitespace();
            }
         }

         if (consume('*')) {
            selectorPart = new SimpleSelector(combinator, null);
         } else {
            String tag = nextIdentifier();
            if (tag != null) {
               selectorPart = new SimpleSelector(combinator, tag);
               selector.addedElement();
            }
         }

         while (!empty())
         {
            if (consume('.'))
            {
               // ".foo" is equivalent to *[class="foo"]
               if (selectorPart == null)
                  selectorPart = new SimpleSelector(combinator, null);
               String  value = nextIdentifier();
               if (value == null)
                  throw new CSSParseException("Invalid \".class\" selector");
               selectorPart.addAttrib(CLASS, AttribOp.EQUALS, value);
               selector.addedAttributeOrPseudo();
               continue;
            }

            if (consume('#'))
            {
               // "#foo" is equivalent to *[id="foo"]
               if (selectorPart == null)
                  selectorPart = new SimpleSelector(combinator, null);
               String  value = nextIdentifier();
               if (value == null)
                  throw new CSSParseException("Invalid \"#id\" selector");
               selectorPart.addAttrib(ID, AttribOp.EQUALS, value);
               selector.addedIdAttribute();
            }

            if (selectorPart == null)
               break;

            // Now check for attribute selection and pseudo selectors   
            if (consume('['))
            {
               skipWhitespace();
               String  attrName = nextIdentifier();
               String  attrValue = null;
               if (attrName == null)
                  throw new CSSParseException("Invalid attribute selector");
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
                  attrValue = nextAttribValue();
                  if (attrValue == null)
                     throw new CSSParseException("Invalid attribute selector");
                  skipWhitespace();
               }
               if (!consume(']'))
                  throw new CSSParseException("Invalid attribute selector");
               selectorPart.addAttrib(attrName, (op == null) ? AttribOp.EXISTS : op, attrValue);
               selector.addedAttributeOrPseudo();
               continue;
            }

            if (consume(':'))
            {
               // skip pseudo
               int  pseudoStart = position;
               if (nextIdentifier() != null) {
                  if (consume('(')) {
                     skipWhitespace();
                     if (nextIdentifier() != null) {
                        skipWhitespace();
                        if (!consume(')')) {
                           position = pseudoStart - 1;
                           break;
                        }
                     }
                  }
                  selectorPart.addPseudo(input.substring(pseudoStart, position));
                  selector.addedAttributeOrPseudo();
               }
            }

            break;
         }

         if (selectorPart != null)
         {
            selector.add(selectorPart);
            return true;
         }

         // Otherwise 'fail'
         position = start;
         return false;
      }

      /*
       * The value (bar) part of "[foo="bar"]".
       */
      private String  nextAttribValue()
      {
         if (empty())
            return null;

         String  result = nextQuotedString();
         if (result != null)
            return result;
         return nextIdentifier();
      }

      /*
       * Scans for a CSS property value.
       */
      String  nextPropertyValue()
      {
         if (empty())
            return null;
         int  start = position;
         int  lastValidPos = position;

         int  ch = input.charAt(position);
         while (ch != -1 && ch != ';' && ch != '}' && ch != '!' && !isEOL(ch)) {
            if (!isWhitespace(ch))  // don't include an spaces at the end
               lastValidPos = position + 1;
            ch = advanceChar();
         }
         if (position > start)
            return input.substring(start, lastValidPos);
         position = start;
         return null;
      }

      /*
       * Scans for a string token
       */
      String  nextCSSString()
      {
         if (empty())
            return null;
         int  ch = input.charAt(position);
         int  endQuote = ch;
         if (ch != '\'' && ch != '"')
            return null;

         StringBuffer  sb = new StringBuffer();
         position++;
         ch = nextChar();
         while (ch != -1 && ch != endQuote)
         {
            if (ch == '\\') {
              // Escaped char sequence
               ch = nextChar();
               if (ch == -1)    // EOF: do nothing
                  continue;
               if (ch == '\n' || ch == '\r' || ch == '\f') {  // a CSS newline
                  ch = nextChar();
                  continue;     // Newline: consume it
               }
               int  hc = hexChar(ch);
               if (hc != -1) {
                  int  codepoint = hc;
                  for (int i=1; i<=5; i++) {
                     ch = nextChar();
                     hc = hexChar(ch);
                     if (hc == -1)
                        break;
                     codepoint = codepoint * 16 + hc;
                  }
                  sb.append((char) codepoint);
                  continue;
               }
               // Other chars just unescape to themselves
               // Fall through to append
            }
            sb.append((char) ch);
            ch = nextChar();
         }
         return sb.toString();
      }


      private int  hexChar(int ch)
      {
         if (ch >= '0' && ch <= '9')
            return ((int)ch - (int)'0');
         if (ch >= 'A' && ch <= 'F')
            return ((int)ch - (int)'A') + 10;
         if (ch >= 'a' && ch <= 'f')
            return ((int)ch - (int)'a') + 10;
         return -1;
      }


      /*
       * Scans for a url("...")
       * Called a <url> in the CSS spec.
       */
      String  nextURL()
      {
         if (empty())
            return null;
         int  start = position;
         if (!consume("url("))
            return null;

         skipWhitespace();

         String url = nextCSSString();
         if (url == null)
            url = nextLegacyURL();  // legacy quote-less url(...).  Called a <url-token> in the CSS3 spec.

         if (url == null) {
            position = start;
            return null;
         }

         skipWhitespace();

         if (empty() || consume(")"))
            return url;

         position = start;
         return null;
      }


      /*
       * Scans for a legacy URL string
       * See nextURLToken().
       */
      String  nextLegacyURL()
      {
         StringBuffer  sb = new StringBuffer();

         while (!empty())
         {
            int  ch = input.charAt(position);

            if (ch == '\'' || ch == '"' || ch == '(' || ch == ')' || isWhitespace(ch) || Character.isISOControl(ch))
               break;

            position++;
            if (ch == '\\')
            {
               if (empty())    // EOF: do nothing
                  continue;
               // Escaped char sequence
               ch = input.charAt(position++);
               if (ch == '\n' || ch == '\r' || ch == '\f') {  // a CSS newline
                  continue;     // Newline: consume it
               }
               int  hc = hexChar(ch);
               if (hc != -1) {
                  int  codepoint = hc;
                  for (int i=1; i<=5; i++) {
                     if (empty())
                        break;
                     hc = hexChar( input.charAt(position) );
                     if (hc == -1)  // Not a hex char
                        break;
                     position++;
                     codepoint = codepoint * 16 + hc;
                  }
                  sb.append((char) codepoint);
                  continue;
               }
               // Other chars just unescape to themselves
               // Fall through to append
            }
            sb.append((char) ch);
         }
         if (sb.length() == 0)
            return null;
         return sb.toString();
      }
   }


   //==============================================================================


   // Returns true if 'deviceMediaType' matches one of the media types in 'mediaList'
   private static boolean mediaMatches(List<MediaType> mediaList, MediaType rendererMediaType)
   {
      for (MediaType type: mediaList) {
         if (type == MediaType.all || type == rendererMediaType)
            return true;
      }
      return false;
   }


   private static List<MediaType> parseMediaList(CSSTextScanner scan)
   {
      ArrayList<MediaType>  typeList = new ArrayList<>();
      while (!scan.empty()) {
         String  type = scan.nextWord();
         if (type == null)
            break;
         try {
            typeList.add(MediaType.valueOf(type));
         } catch (IllegalArgumentException e) {
            // Ignore invalid media types
         }
         // If there is a comma, keep looping, otherwise break
         if (!scan.skipCommaWhitespace())
            break;
      }
      return typeList;
   }


   private void  parseAtRule(Ruleset ruleset, CSSTextScanner scan) throws CSSParseException
   {
      String  atKeyword = scan.nextIdentifier();
      scan.skipWhitespace();
      if (atKeyword == null)
         throw new CSSParseException("Invalid '@' rule");
      if (!inMediaRule && atKeyword.equals("media"))
      {
         List<MediaType>  mediaList = parseMediaList(scan);
         if (!scan.consume('{'))
            throw new CSSParseException("Invalid @media rule: missing rule set");
            
         scan.skipWhitespace();
         if (mediaMatches(mediaList, deviceMediaType)) {
            inMediaRule = true;
            ruleset.addAll( parseRuleset(scan) );
            inMediaRule = false;
         } else {
            parseRuleset(scan);  // parse and ignore accompanying ruleset
         }

         if (!scan.empty() && !scan.consume('}'))
            throw new CSSParseException("Invalid @media rule: expected '}' at end of rule set");

      }
      else if (!inMediaRule && atKeyword.equals("import"))
      {
         String  file = scan.nextURL();
         if (file == null)
            file = scan.nextCSSString();
         if (file == null)
            throw new CSSParseException("Invalid @import rule: expected string or url()");

         scan.skipWhitespace();
         List<MediaType>  mediaList = parseMediaList(scan);

         if (!scan.empty() && !scan.consume(';'))
            throw new CSSParseException("Invalid @media rule: expected '}' at end of rule set");

         if (SVG.getFileResolver() != null && mediaMatches(mediaList, deviceMediaType)) {
            String  css = SVG.getFileResolver().resolveCSSStyleSheet(file);
            if (css == null)
               return;
            ruleset.addAll( parse(css) );
         }
      }
      //} else if (atKeyword.equals("charset")) {
      else
      {
         // Unknown/unsupported at-rule
         warn("Ignoring @%s rule", atKeyword);
         skipAtRule(scan);
      }
      scan.skipWhitespace();
   }


   // Skip an unsupported at-rule: "ignore everything up to and including the next semicolon or block".
   private void  skipAtRule(CSSTextScanner scan)
   {
      int depth = 0;
      while (!scan.empty())
      {
         int ch = scan.nextChar();
         if (ch == ';' && depth == 0)
            return;
         if (ch == '{')
            depth++;
         else if (ch == '}' && depth > 0) {
            if (--depth == 0)
               return;
         }
      }
   }


   private Ruleset  parseRuleset(CSSTextScanner scan)
   {
      Ruleset  ruleset = new Ruleset(); 
      try
      {
         while (!scan.empty())
         {
            if (scan.consume("<!--"))
               continue;
            if (scan.consume("-->"))
               continue;

            if (scan.consume('@')) {
               parseAtRule(ruleset, scan);
               continue;
            }
            if (parseRule(ruleset, scan))
               continue;

            // Nothing recognisable found. Could be end of rule set. Return.
            break;
         }
      }
      catch (CSSParseException e)
      {
         Log.e(TAG, "CSS parser terminated early due to error: " + e.getMessage());
         if (LibConfig.DEBUG)
            Log.e(TAG,"Stacktrace:", e);
      }
      return ruleset;
   }


   private boolean  parseRule(Ruleset ruleset, CSSTextScanner scan) throws CSSParseException
   {
      List<Selector>  selectors = parseSelectorGroup(scan);
      if (selectors != null && !selectors.isEmpty())
      {
         if (!scan.consume('{'))
            throw new CSSParseException("Malformed rule block: expected '{'");
         scan.skipWhitespace();
         SVG.Style  ruleStyle = parseDeclarations(scan);
         scan.skipWhitespace();
         for (Selector selector: selectors) {
            ruleset.add( new Rule(selector, ruleStyle, source) );
         }
         return true;
      }
      else
      {
         return false;
      }
   }


   /*
    * Parse a selector group (eg. E, F, G). In many/most cases there will be only one entry.
    */
   private List<Selector>  parseSelectorGroup(CSSTextScanner scan) throws CSSParseException
   {
      if (scan.empty())
         return null;

      ArrayList<Selector>  selectorGroup = new ArrayList<>(1);
      Selector             selector = new Selector();

      while (!scan.empty())
      {
         if (scan.nextSimpleSelector(selector))
         {
            // If there is a comma, keep looping, otherwise break
            if (!scan.skipCommaWhitespace())
               continue;  // if not a comma, go back and check for next part of selector
            selectorGroup.add(selector);
            selector = new Selector();
         }
         else
            break;
      }
      if (!selector.isEmpty())
         selectorGroup.add(selector);
      return selectorGroup;
   }


   // Parse a list of
   private SVG.Style  parseDeclarations(CSSTextScanner scan) throws CSSParseException
   {
      SVG.Style  ruleStyle = new SVG.Style();
      while (true)
      {
         String  propertyName = scan.nextIdentifier();
         scan.skipWhitespace();
         if (!scan.consume(':'))
            throw new CSSParseException("Expected ':'");
         scan.skipWhitespace();
         String  propertyValue = scan.nextPropertyValue();
         if (propertyValue == null)
            throw new CSSParseException("Expected property value");
         // Check for !important flag.
         scan.skipWhitespace();
         if (scan.consume('!')) {
            scan.skipWhitespace();
            if (!scan.consume("important")) {
               throw new CSSParseException("Malformed rule set: found unexpected '!'");
            }
            // We don't do anything with these. We just ignore them.
            scan.skipWhitespace();
         }
         scan.consume(';');
         // TODO: support CSS only values such as "inherit"
         SVGParser.processStyleProperty(ruleStyle, propertyName, propertyValue);
         scan.skipWhitespace();
         if (scan.empty() || scan.consume('}'))
            break;
      }
      return ruleStyle;
   }


   /*
    * Used by SVGParser to parse the "class" attribute.
    * Follows ordered set parser algorithm: https://dom.spec.whatwg.org/#concept-ordered-set-parser
    */
   public static List<String>  parseClassAttribute(String val)
   {
      CSSTextScanner  scan = new CSSTextScanner(val);
      List<String>    classNameList = null;

      while (!scan.empty())
      {
         String  className = scan.nextToken();
         if (className == null)
            continue;
         if (classNameList == null)
            classNameList = new ArrayList<>();
         classNameList.add(className);
         scan.skipWhitespace();
      }
      return classNameList;
   }


   /*
    * Used by renderer to check if a CSS rule matches the current element.
    */
   static boolean  ruleMatch(Selector selector, SvgElementBase obj)
   {
      // Build the list of ancestor objects
      List<SvgContainer> ancestors = new ArrayList<>();
      SvgContainer  parent = obj.parent;
      while (parent != null) {
         ancestors.add(0, parent);
         parent = ((SvgObject) parent).parent;
      }
      
      int  ancestorsPos = ancestors.size() - 1;

      // Check the most common case first as a shortcut.
      if (selector.size() == 1)
         return selectorMatch(selector.get(0), ancestors, ancestorsPos, obj);
      
      // We start at the last part of the selector and loop back through the parts
      // Get the next selector part
      return ruleMatch(selector, selector.size() - 1, ancestors, ancestorsPos, obj);
   }


   private static boolean  ruleMatch(Selector selector, int selPartPos, List<SvgContainer> ancestors, int ancestorsPos, SvgElementBase obj)
   {
      // We start at the last part of the selector and loop back through the parts
      // Get the next selector part
      SimpleSelector  sel = selector.get(selPartPos);
      if (!selectorMatch(sel, ancestors, ancestorsPos, obj))
         return false;

      // Selector part matched, check its combinator
      if (sel.combinator == Combinator.DESCENDANT)
      {
         if (selPartPos == 0)
            return true;
         // Search up the ancestors list for a node that matches the next selector
         while (ancestorsPos >= 0) {
            if (ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, ancestorsPos))
               return true;
            ancestorsPos--;
         }
         return false;
      }
      else if (sel.combinator == Combinator.CHILD)
      {
         return ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, ancestorsPos);
      }
      else //if (sel.combinator == Combinator.FOLLOWS)
      {
         int  childPos = getChildPosition(ancestors, ancestorsPos, obj);
         if (childPos <= 0)
            return false;
         SvgElementBase  prevSibling = (SvgElementBase) obj.parent.getChildren().get(childPos - 1);
         return ruleMatch(selector, selPartPos - 1, ancestors, ancestorsPos, prevSibling);
      }
   }


   private static boolean  ruleMatchOnAncestors(Selector selector, int selPartPos, List<SvgContainer> ancestors, int ancestorsPos)
   {
      SimpleSelector  sel = selector.get(selPartPos);
      SvgElementBase  obj = (SvgElementBase) ancestors.get(ancestorsPos);

      if (!selectorMatch(sel, ancestors, ancestorsPos, obj))
         return false;

      // Selector part matched, check its combinator
      if (sel.combinator == Combinator.DESCENDANT)
      {
         if (selPartPos == 0)
            return true;
         // Search up the ancestors list for a node that matches the next selector
         while (ancestorsPos > 0) {
            if (ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, --ancestorsPos))
               return true;
         }
         return false;
      }
      else if (sel.combinator == Combinator.CHILD)
      {
         return ruleMatchOnAncestors(selector, selPartPos - 1, ancestors, ancestorsPos - 1);
      }
      else //if (sel.combinator == Combinator.FOLLOWS)
      {
         int  childPos = getChildPosition(ancestors, ancestorsPos, obj);
         if (childPos <= 0)
            return false;
         SvgElementBase  prevSibling = (SvgElementBase) obj.parent.getChildren().get(childPos - 1);
         return ruleMatch(selector, selPartPos - 1, ancestors, ancestorsPos, prevSibling);
      }
   }


   private static int getChildPosition(List<SvgContainer> ancestors, int ancestorsPos, SvgElementBase obj)
   {
      if (ancestorsPos < 0)  // Has no parent, so can't have a sibling
         return -1;
      if (ancestors.get(ancestorsPos) != obj.parent)  // parent doesn't match, so obj must be an indirect reference (eg. from a <use>)
         return -1;
      int  childPos = 0;
      for (SvgObject child: obj.parent.getChildren())
      {
         if (child == obj)
            return childPos;
         childPos++;
      }
      return -1;
   }


   private static boolean selectorMatch(SimpleSelector sel, List<SvgContainer> ancestors, int ancestorsPos, SvgElementBase obj)
   {
      // Check tag name. tag==null means tag is "*" which matches everything.
      if (sel.tag != null && !sel.tag.equals(obj.getNodeName().toLowerCase(Locale.US)))
         return false;

      // If here, then tag part matched

      // Check the attributes
      if (sel.attribs != null)
      {
         for (Attrib attr: sel.attribs)
         {
            switch (attr.name) {
               case ID:
                  if (!attr.value.equals(obj.id))
                     return false;
                  break;
               case CLASS:
                  if (obj.classNames == null)
                     return false;
                  if (!obj.classNames.contains(attr.value))
                     return false;
                  break;
               default:
                  // Other attribute selector not yet supported
                  return false;
            }
         }
      }

      // Check the pseudo classes
      if (sel.pseudos != null) {
         for (String pseudo: sel.pseudos) {
            if (pseudo.equals("first-child")) {
               if (getChildPosition(ancestors, ancestorsPos, obj) != 0)
                  return false;
            } else {
               return false;
            }
         }
      }

      // If w reached this point, the selector matched
      return true;
   }


}
