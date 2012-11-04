package com.caverock.androidsvg;

public class SVGParseException extends Exception
{
   public SVGParseException(String msg)
   {
      super(msg);
   }

   public SVGParseException(String msg, Throwable cause)
   {
      super(msg, cause);
   }

   public SVGParseException(Throwable cause)
   {
      super(cause);
   }
}
