/*
   Copyright 2013-2020 Paul LeBeau, Cave Rock Software Ltd.

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

package com.caverock.androidsvg.utils;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/*
 * Keeps a list of font feature settings and their values.
 */
public class CSSFontFeatureSettings
{
   public static final CSSFontFeatureSettings  FONT_FEATURE_SETTINGS_NORMAL = makeDefaultSettings();

   private static final String  FONT_VARIANT_NORMAL = "normal";
   private static final String  FONT_VARIANT_AUTO = "auto";
   private static final String  FONT_VARIANT_NONE = "none";

   private static final String  FEATURE_ON  = "on";
   private static final String  FEATURE_OFF = "off";

   private static final int  VALUE_ON  = 1;
   private static final int  VALUE_OFF = 0;

   private static final String  TOKEN_ERROR = "ERR";

   // For font-kerning
   private static FontFeatureEntry  KERNING_ON = null;
   private static FontFeatureEntry  KERNING_OFF = null;

   public static final String FEATURE_KERN = "kern";

   // For font-variant-ligatures
   static CSSFontFeatureSettings  LIGATURES_NORMAL = null;
   private static CSSFontFeatureSettings LIGATURES_ALL_OFF = null;

   private static final String  FONT_VARIANT_COMMON_LIGATURES = "common-ligatures";
   private static final String  FONT_VARIANT_NO_COMMON_LIGATURES = "no-common-ligatures";
   private static final String  FONT_VARIANT_DISCRETIONARY_LIGATURES = "discretionary-ligatures";
   private static final String  FONT_VARIANT_NO_DISCRETIONARY_LIGATURES = "no-discretionary-ligatures";
   private static final String  FONT_VARIANT_HISTORICAL_LIGATURES = "historical-ligatures";
   private static final String  FONT_VARIANT_NO_HISTORICAL_LIGATURES = "no-historical-ligatures";
   private static final String  FONT_VARIANT_CONTEXTUAL_LIGATURES = "contextual";
   private static final String  FONT_VARIANT_NO_CONTEXTUAL_LIGATURES = "no-contextual";

   public static final String FEATURE_CLIG = "clig";
   public static final String FEATURE_LIGA = "liga";
   public static final String FEATURE_DLIG = "dlig";
   public static final String FEATURE_HLIG = "hlig";
   public static final String FEATURE_CALT = "calt";

   // For font-variant-position
   static CSSFontFeatureSettings  POSITION_ALL_OFF = null;

   private static final String  FONT_VARIANT_SUB = "sub";
   private static final String  FONT_VARIANT_SUPER = "super";

   private static final String  FEATURE_SUBS = "subs";
   private static final String  FEATURE_SUPS = "sups";

   // For font-variant-caps
   static CSSFontFeatureSettings  CAPS_ALL_OFF = null;

   private static final String  FONT_VARIANT_SMALL_CAPS = "small-caps";
   private static final String  FONT_VARIANT_ALL_SMALL_CAPS = "all-small-caps";
   private static final String  FONT_VARIANT_PETITE_CAPS = "petite-caps";
   private static final String  FONT_VARIANT_ALL_PETITE_CAPS = "all-petite-caps";
   private static final String  FONT_VARIANT_UNICASE = "unicase";
   private static final String  FONT_VARIANT_TITLING_CAPS = "titling-caps";

   private static final String  FEATURE_SMCP = "smcp";
   private static final String  FEATURE_C2SC = "c2sc";
   private static final String  FEATURE_PCAP = "pcap";
   private static final String  FEATURE_C2PC = "c2pc";
   private static final String  FEATURE_UNIC = "unic";
   private static final String  FEATURE_TITL = "titl";

   // For font-variant-numeric
   static CSSFontFeatureSettings  NUMERIC_ALL_OFF = null;

   private static final String  FONT_VARIANT_LINING_NUMS = "lining-nums";
   private static final String  FONT_VARIANT_OLDSTYLE_NUMS = "oldstyle-nums";
   private static final String  FONT_VARIANT_PROPORTIONAL_NUMS = "proportional-nums";
   private static final String  FONT_VARIANT_TABULAR_NUMS = "tabular-nums";
   private static final String  FONT_VARIANT_DIAGONAL_FRACTIONS = "diagonal-fractions";
   private static final String  FONT_VARIANT_STACKED_FRACTIONS = "stacked-fractions";
   private static final String  FONT_VARIANT_ORDINAL = "ordinal";
   private static final String  FONT_VARIANT_SLASHED_ZERO = "slashed-zero";

   public static final String FEATURE_LNUM = "lnum";
   public static final String FEATURE_ONUM = "onum";
   public static final String FEATURE_PNUM = "pnum";
   public static final String FEATURE_TNUM = "tnum";
   public static final String FEATURE_FRAC = "frac";
   public static final String FEATURE_AFRC = "afrc";
   public static final String FEATURE_ORDN = "ordn";
   public static final String FEATURE_ZERO = "zero";

   // For font-variant-east-asian
   static CSSFontFeatureSettings  EAST_ASIAN_ALL_OFF = null;

   private static final String  FONT_VARIANT_JIS78 = "jis78";
   private static final String  FONT_VARIANT_JIS83 = "jis83";
   private static final String  FONT_VARIANT_JIS90 = "jis90";
   private static final String  FONT_VARIANT_JIS04 = "jis04";
   private static final String  FONT_VARIANT_SIMPLIFIED = "simplified";
   private static final String  FONT_VARIANT_TRADITIONAL = "traditional";
   private static final String  FONT_VARIANT_FULL_WIDTH = "full-width";
   private static final String  FONT_VARIANT_PROPORTIONAL_WIDTH = "proportional-width";
   private static final String  FONT_VARIANT_RUBY = "ruby";

   public static final String FEATURE_JP78 = "jp78";
   public static final String FEATURE_JP83 = "jp83";
   public static final String FEATURE_JP90 = "jp90";
   public static final String FEATURE_JP04 = "jp04";
   public static final String FEATURE_SMPL = "smpl";
   public static final String FEATURE_TRAD = "trad";
   public static final String FEATURE_FWID = "fwid";
   public static final String FEATURE_PWID = "pwid";
   public static final String FEATURE_RUBY = "ruby";


   private final List<FontFeatureEntry>  settings;
//   private final HashMap<String, Integer>  settings;


   private static class FontFeatureEntry {
      String  name;
      int     val;

      public FontFeatureEntry(String name, int val) {
         this.name = name;
         this.val = val;
      }

      @Override
      public String  toString() {
         return "\"" + name + "\" " + val;
      }
   }


   static {
      LIGATURES_NORMAL = new CSSFontFeatureSettings();
      LIGATURES_NORMAL.settings.add(new FontFeatureEntry(FEATURE_LIGA, VALUE_ON));
      LIGATURES_NORMAL.settings.add(new FontFeatureEntry(FEATURE_CLIG, VALUE_ON));
      LIGATURES_NORMAL.settings.add(new FontFeatureEntry(FEATURE_DLIG, VALUE_OFF));
      LIGATURES_NORMAL.settings.add(new FontFeatureEntry(FEATURE_HLIG, VALUE_OFF));
      LIGATURES_NORMAL.settings.add(new FontFeatureEntry(FEATURE_CALT, VALUE_ON));

      POSITION_ALL_OFF = new CSSFontFeatureSettings();
      POSITION_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_SUBS, VALUE_OFF));
      POSITION_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_SUPS, VALUE_OFF));

      CAPS_ALL_OFF = new CSSFontFeatureSettings();
      CAPS_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_SMCP, VALUE_OFF));
      CAPS_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_C2SC, VALUE_OFF));
      CAPS_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_PCAP, VALUE_OFF));
      CAPS_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_C2PC, VALUE_OFF));
      CAPS_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_UNIC, VALUE_OFF));
      CAPS_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_TITL, VALUE_OFF));

      NUMERIC_ALL_OFF = new CSSFontFeatureSettings();
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_LNUM, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_ONUM, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_PNUM, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_TNUM, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_FRAC, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_AFRC, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_ORDN, VALUE_OFF));
      NUMERIC_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_ZERO, VALUE_OFF));

      EAST_ASIAN_ALL_OFF = new CSSFontFeatureSettings();
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_JP78, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_JP83, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_JP90, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_JP04, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_SMPL, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_TRAD, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_FWID, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_PWID, VALUE_OFF));
      EAST_ASIAN_ALL_OFF.settings.add(new FontFeatureEntry(FEATURE_RUBY, VALUE_OFF));
   }


   public CSSFontFeatureSettings()
   {
      this.settings = new ArrayList<>();
   }

   private CSSFontFeatureSettings(List<FontFeatureEntry> initialMap)
   {
      this.settings = initialMap;
   }

   public CSSFontFeatureSettings(CSSFontFeatureSettings other)
   {
      this.settings = new ArrayList<>(other.settings);
   }


   public void  applySettings(CSSFontFeatureSettings featureSettings)
   {
      if (featureSettings == null)
         return;
      for (FontFeatureEntry entry: featureSettings.settings) {
         applySetting(entry);
      }
   }


   private void  applySetting(FontFeatureEntry entry)
   {
      removeIfPresent(entry);
      this.settings.add(entry);
   }


   private void  removeIfPresent(FontFeatureEntry entry)
   {
      Iterator<FontFeatureEntry>  iter = this.settings.iterator();
      while (iter.hasNext()) {
         FontFeatureEntry  existing = iter.next();
         if (existing.name.equals(entry.name)) {
            iter.remove();
            return;
         }
      }
   }


   public void  applyKerning(Style.FontKerning kern)
   {
      if (kern == Style.FontKerning.none ) {
         if (KERNING_OFF == null) {
            KERNING_OFF = new FontFeatureEntry(FEATURE_KERN, VALUE_OFF);
         }
         applySetting(KERNING_OFF);
      } else {
         if (KERNING_ON == null) {
            KERNING_ON = new FontFeatureEntry(FEATURE_KERN, VALUE_ON);
         }
         applySetting(KERNING_ON);
      }
   }


   public boolean  hasSettings()
   {
      return this.settings.size() > 0;
   }


   @Override
   public String toString()
   {
      return TextUtils.join(", ", this.settings);
   }


   //-----------------------------------------------------------------------------------------------
   // Parsing font-feature-settings property value


   /*
    * Parse the value of the CSS property "font-feature-settings".
    *
    * Format is: <feature-tag-value>[comma-wsp <feature-tag-value>]*
    *            <feature-tag-value> = <string> [ <integer> | on | off ]?
    */
   public static CSSFontFeatureSettings  parseFontFeatureSettings(String val)
   {
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings();

      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();

      while (true) {
         if (scan.empty())
            break;
         FontFeatureEntry  entry = nextFeatureEntry(scan);
         if (entry == null)
            return null;
         result.settings.add(entry);
         scan.skipCommaWhitespace();
      }
      return result;
   }


   private static FontFeatureEntry  nextFeatureEntry(TextScanner scan)
   {
      scan.skipWhitespace();
      String name = scan.nextQuotedString();
      if (name == null || name.length() != 4)
         return null;
      scan.skipWhitespace();
      int  val = 1;
      if (!scan.empty()) {
         Integer  num = scan.nextInteger(false);
         if (num == null) {
            if (scan.consume(FEATURE_OFF))
               val = 0;
            else
               scan.consume(FEATURE_ON);  // "on" == 1 == default, so consume quietly if it is present
         } else if (val > 99) {
            return null;
         } else {
            val = num;
         }
      }
      return new FontFeatureEntry(name, val);
   }


   //-----------------------------------------------------------------------------------------------


   // Parse a font-kerning keyword
   static Style.FontKerning  parseFontKerning(String val)
   {
      switch (val)
      {
         case FONT_VARIANT_AUTO:   return Style.FontKerning.auto;
         case FONT_VARIANT_NORMAL: return Style.FontKerning.normal;
         case FONT_VARIANT_NONE:   return Style.FontKerning.none;
         default:                  return null;
      }
   }


   private static List<String>  extractTokensAsList(String val)
   {
      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();
      if (scan.empty())
         return null;
      ArrayList<String>  result = new ArrayList<>();
      while (!scan.empty())
      {
         result.add(scan.nextToken());
         scan.skipWhitespace();
      }
      return result;
   }


   /*
    * Returns:
    *   1 if token list contains token1,
    *   2 if it contains token2,
    *   3 if it contains both, or more than one of either,
    *   0 if it contains neither.
    */
   private static int  containsWhich(List<String> tokens, String token1, String token2)
   {
      if (tokens.remove(token1)) {
         return tokens.contains(token1) || tokens.contains(token2) ? 3 : 1;
      } else if (tokens.remove(token2)) {
         return tokens.contains(token2) ? 3 : 2;
      }
      return 0;
   }


   /*
    * Returns:
    *   1 if token list contains token1,
    *   2 if it contains more than one token1,
    *   0 if it doesn't contains token1.
    */
   private static int  containsOnce(List<String> tokens, String token1)
   {
      if (tokens.remove(token1)) {
         return tokens.contains(token1) ? 2 : 1;
      }
      return 0;
   }


   /*
    * Checks haystack to see which needle is present (if any).  Returns the needle.
    * If there is more than one of the needles present, then returns null.
    */
   private static String  containsOneOf(List<String> haystack, String... needles)
   {
      String found = null;
      for (String needle: needles)
      {
         if (found == null && haystack.remove(needle)) {
            found = needle;
         }
         if (haystack.contains(needle))
            return TOKEN_ERROR;
      }
      return found;
   }


   /*
    * Parse a font-variant-ligatures property
    * Format:
    *   normal | none | [ <common-lig-values> || <discretionary-lig-values> || <historical-lig-values> || <contextual-alt-values> ]
    *   <common-lig-values>        = [ common-ligatures | no-common-ligatures ]
    *   <discretionary-lig-values> = [ discretionary-ligatures | no-discretionary-ligatures ]
    *   <historical-lig-values>    = [ historical-ligatures | no-historical-ligatures ]
    *   <contextual-alt-values>    = [ contextual | no-contextual ]
    */
   static CSSFontFeatureSettings  parseVariantLigatures(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return LIGATURES_NORMAL;
      else if (val.equals(FONT_VARIANT_NONE)) {
         ensureLigaturesNone();
         return LIGATURES_ALL_OFF;
      }

      ensureLigaturesNone();
      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(LIGATURES_ALL_OFF);

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)
         return null;

      if (!parseVariantLigaturesSpecial(result, tokens))
         return null;

      // Unrecognised tokens remain?
      if (tokens.size() > 0)
         return null;

      return result;
   }


   private static boolean  parseVariantLigaturesSpecial(CSSFontFeatureSettings result, List<String> tokens)
   {
      switch (containsWhich(tokens, FONT_VARIANT_COMMON_LIGATURES, FONT_VARIANT_NO_COMMON_LIGATURES))
      {
         case 1: result.addSettings(FEATURE_CLIG, FEATURE_LIGA, VALUE_ON); break;
         case 2: result.addSettings(FEATURE_CLIG, FEATURE_LIGA, VALUE_OFF); break;
         case 3: return false;
      }

      switch (containsWhich(tokens, FONT_VARIANT_DISCRETIONARY_LIGATURES, FONT_VARIANT_NO_DISCRETIONARY_LIGATURES))
      {
         case 1: result.addSetting(FEATURE_DLIG, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_DLIG, VALUE_OFF); break;
         case 3: return false;
      }

      switch (containsWhich(tokens, FONT_VARIANT_HISTORICAL_LIGATURES, FONT_VARIANT_NO_HISTORICAL_LIGATURES))
      {
         case 1: result.addSetting(FEATURE_HLIG, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_HLIG, VALUE_OFF); break;
         case 3: return false;
      }

      switch (containsWhich(tokens, FONT_VARIANT_CONTEXTUAL_LIGATURES, FONT_VARIANT_NO_CONTEXTUAL_LIGATURES))
      {
         case 1: result.addSetting(FEATURE_CALT, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_CALT, VALUE_OFF); break;
         case 3: return false;
      }
      return true;
   }


   private void  addSetting(String feature1, int onOrOff)
   {
      applySetting(new FontFeatureEntry(feature1, onOrOff));
   }


   private void  addSettings(String feature1, String feature2, int onOrOff)
   {
      applySetting(new FontFeatureEntry(feature1, onOrOff));
      applySetting(new FontFeatureEntry(feature2, onOrOff));
   }


   // Parse a font-kerning property
   static CSSFontFeatureSettings  parseVariantPosition(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return POSITION_ALL_OFF;

      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(CAPS_ALL_OFF);
      switch (val)
      {
         case FONT_VARIANT_SUB:    result.addSetting(FEATURE_SUBS, VALUE_ON); break;
         case FONT_VARIANT_SUPER:  result.addSetting(FEATURE_SUPS, VALUE_ON); break;
         default:                  return null;
      }
      return result;
   }


   // Used only by parseFontVariant()
   // Only looks for the values unique to this property
   private static boolean  parseVariantPositionSpecial(CSSFontFeatureSettings result, List<String> tokens)
   {
      switch (containsWhich(tokens, FONT_VARIANT_SUB, FONT_VARIANT_SUPER))
      {
         case 1: result.addSetting(FEATURE_SUBS, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_SUPS, VALUE_OFF); break;
         case 3: return false;
      }
      return true;
   }


   // Parse a font-variant-caps property
   static CSSFontFeatureSettings  parseVariantCaps(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return CAPS_ALL_OFF;

      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(CAPS_ALL_OFF);
      return setCapsFeature(result, val) ? result : null;
   }

   private static boolean  setCapsFeature(CSSFontFeatureSettings result, String val)
   {
      switch (val)
      {
         case FONT_VARIANT_SMALL_CAPS:      result.addSetting(FEATURE_SMCP, VALUE_ON); break;
         case FONT_VARIANT_ALL_SMALL_CAPS:  result.addSettings(FEATURE_SMCP, FEATURE_C2SC, VALUE_ON); break;
         case FONT_VARIANT_PETITE_CAPS:     result.addSetting(FEATURE_PCAP, VALUE_ON); break;
         case FONT_VARIANT_ALL_PETITE_CAPS: result.addSettings(FEATURE_PCAP, FEATURE_C2PC, VALUE_ON); break;
         case FONT_VARIANT_UNICASE:         result.addSetting(FEATURE_UNIC, VALUE_ON); break;
         case FONT_VARIANT_TITLING_CAPS:    result.addSetting(FEATURE_TITL, VALUE_ON); break;
         default:                           return false;
      }
      return true;
   }


   // Used only by parseFontVariant()
   // Only looks for the values unique to this property
   private static boolean  parseVariantCapsSpecial(CSSFontFeatureSettings result, List<String> tokens)
   {
      String which = containsOneOf(tokens, FONT_VARIANT_SMALL_CAPS, FONT_VARIANT_ALL_SMALL_CAPS, FONT_VARIANT_PETITE_CAPS,
                                           FONT_VARIANT_ALL_PETITE_CAPS, FONT_VARIANT_UNICASE, FONT_VARIANT_TITLING_CAPS);
      if (which == null || which.equals(TOKEN_ERROR))
         return false;

      return setCapsFeature(result, which);
   }


   /*
    * Parse a font-variant-numeric property
    * Format:
    *   normal | [ <numeric-figure-values> || <numeric-spacing-values> || <numeric-fraction-values> || ordinal || slashed-zero ]
    *   <numeric-figure-values>   = [ lining-nums | oldstyle-nums ]
    *   <numeric-spacing-values>  = [ proportional-nums | tabular-nums ]
    *   <numeric-fraction-values> = [ diagonal-fractions | stacked-fractions ]
    */
   static CSSFontFeatureSettings  parseVariantNumeric(String val)
   {
      if (val.equals(FONT_VARIANT_NORMAL))
         return NUMERIC_ALL_OFF;

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)
         return null;

      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(NUMERIC_ALL_OFF);

      if (!parseVariantNumericSpecial(result, tokens))
         return null;

      // Unrecognised tokens remain?
      if (tokens.size() > 0)
         return null;

      return result;
   }


   private static boolean  parseVariantNumericSpecial(CSSFontFeatureSettings result, List<String> tokens)
   {
      switch (containsWhich(tokens, FONT_VARIANT_LINING_NUMS, FONT_VARIANT_OLDSTYLE_NUMS))
      {
         case 1: result.addSetting(FEATURE_LNUM, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_ONUM, VALUE_ON); break;
         case 3: return false;
      }

      switch (containsWhich(tokens, FONT_VARIANT_PROPORTIONAL_NUMS, FONT_VARIANT_TABULAR_NUMS))
      {
         case 1: result.addSetting(FEATURE_PNUM, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_TNUM, VALUE_ON); break;
         case 3: return false;
      }

      switch (containsWhich(tokens, FONT_VARIANT_DIAGONAL_FRACTIONS, FONT_VARIANT_STACKED_FRACTIONS))
      {
         case 1: result.addSetting(FEATURE_FRAC, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_AFRC, VALUE_ON); break;
         case 3: return false;
      }

      switch (containsOnce(tokens, FONT_VARIANT_ORDINAL))
      {
         case 1: result.addSetting(FEATURE_ORDN, VALUE_ON); break;
         case 2: return false;
      }

      switch (containsOnce(tokens, FONT_VARIANT_SLASHED_ZERO))
      {
         case 1: result.addSetting(FEATURE_ZERO, VALUE_ON); break;
         case 2: return false;
      }

      return true;
   }


   /*
    * Parse a font-variant-east-asian property
    * Format:
    *   normal | [ <east-asian-variant-values> || <east-asian-width-values> || ruby ]
    *   <east-asian-variant-values> = [ jis78 | jis83 | jis90 | jis04 | simplified | traditional ]
    *   <east-asian-width-values>   = [ full-width | proportional-width ]
    */
   static CSSFontFeatureSettings  parseEastAsian(String val)
   {
/**/Log.d("TEST", "**** pEA val="+val);
      if (val.equals(FONT_VARIANT_NORMAL))
          return EAST_ASIAN_ALL_OFF;

      List<String>  tokens = extractTokensAsList(val);
      if (tokens == null)
         return null;

      CSSFontFeatureSettings  result = new CSSFontFeatureSettings(EAST_ASIAN_ALL_OFF);

      if (!parseVariantEastAsianSpecial(result, tokens))
         return null;

      // Unrecognised tokens remain?
      if (tokens.size() > 0)
         return null;

      return result;
   }


   private static boolean  parseVariantEastAsianSpecial(CSSFontFeatureSettings result, List<String> tokens)
   {
      String which = containsOneOf(tokens, FONT_VARIANT_JIS78, FONT_VARIANT_JIS83, FONT_VARIANT_JIS90,
                                           FONT_VARIANT_JIS04, FONT_VARIANT_SIMPLIFIED, FONT_VARIANT_TRADITIONAL);
/**/Log.d("TEST", "**** pEA: which="+which);
      if (which != null)
      {
         switch (which)
         {
            case FONT_VARIANT_JIS78:       result.addSetting(FEATURE_JP78, VALUE_ON); break;
            case FONT_VARIANT_JIS83:       result.addSetting(FEATURE_JP83, VALUE_ON); break;
            case FONT_VARIANT_JIS90:       result.addSetting(FEATURE_JP90, VALUE_ON); break;
            case FONT_VARIANT_JIS04:       result.addSetting(FEATURE_JP04, VALUE_ON); break;
            case FONT_VARIANT_SIMPLIFIED:  result.addSetting(FEATURE_SMPL, VALUE_ON); break;
            case FONT_VARIANT_TRADITIONAL: result.addSetting(FEATURE_TRAD, VALUE_ON); break;
            case TOKEN_ERROR:              return false;  // more than one, or duplicate, found
         }
      }
/**/Log.d("TEST", "**** pEA: result="+result);

      switch (containsWhich(tokens, FONT_VARIANT_FULL_WIDTH, FONT_VARIANT_PROPORTIONAL_WIDTH))
      {
         case 1: result.addSetting(FEATURE_FWID, VALUE_ON); break;
         case 2: result.addSetting(FEATURE_PWID, VALUE_ON); break;
         case 3: return false;
      }

/**/Log.d("TEST", "**** pEA: check ruby");
      switch (containsOnce(tokens, FONT_VARIANT_RUBY))
      {
         case 1: result.addSetting(FEATURE_RUBY, VALUE_ON); break;
         case 2: return false;
      }

/**/Log.d("TEST", "**** pEA: returning true");
      return true;
   }


   //-----------------------------------------------------------------------------------------------


   static void parseFontVariant(Style style, String val)
   {

   }


   //-----------------------------------------------------------------------------------------------


   private static final CSSFontFeatureSettings  makeDefaultSettings()
   {
      // See: https://www.w3.org/TR/css-fonts-3/#default-features
      CSSFontFeatureSettings result = new CSSFontFeatureSettings();
      result.settings.add(new FontFeatureEntry("rlig", VALUE_ON));
      result.settings.add(new FontFeatureEntry("liga", VALUE_ON));
      result.settings.add(new FontFeatureEntry("clig", VALUE_ON));
      result.settings.add(new FontFeatureEntry("calt", VALUE_ON));
      result.settings.add(new FontFeatureEntry("locl", VALUE_ON));
      result.settings.add(new FontFeatureEntry("ccmp", VALUE_ON));
      result.settings.add(new FontFeatureEntry("mark", VALUE_ON));
      result.settings.add(new FontFeatureEntry("mkmk", VALUE_ON));
      // TODO FIXME  also enable "vert" for vertical runs in complex scripts
      return result;
   }


   private static void  ensureLigaturesNone()
   {
      // all ligatures OFF
      if (LIGATURES_ALL_OFF != null)
         return;
      CSSFontFeatureSettings result = new CSSFontFeatureSettings();
      result.settings.add(new FontFeatureEntry("liga", VALUE_OFF));
      result.settings.add(new FontFeatureEntry("clig", VALUE_OFF));
      result.settings.add(new FontFeatureEntry("dlig", VALUE_OFF));
      result.settings.add(new FontFeatureEntry("hlig", VALUE_OFF));
      result.settings.add(new FontFeatureEntry("calt", VALUE_OFF));
      LIGATURES_ALL_OFF = result;
   }


   private void  ensurePositionNormal()
   {
      // common and contextual ligatures ON; discretionary  and historical ligatures OFF
      if (POSITION_ALL_OFF == null) {
         CSSFontFeatureSettings result = new CSSFontFeatureSettings();
         result.settings.add(new FontFeatureEntry(FEATURE_SUBS, VALUE_OFF));
         result.settings.add(new FontFeatureEntry(FEATURE_SUPS, VALUE_OFF));
         this.POSITION_ALL_OFF = result;
      }
   }

}
