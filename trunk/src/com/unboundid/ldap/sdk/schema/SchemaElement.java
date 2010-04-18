/*
 * Copyright 2007-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2010 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk.schema;



import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.NotExtensible;

import static com.unboundid.ldap.sdk.schema.SchemaMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides a superclass for all schema element types, and defines a
 * number of utility methods that may be used when parsing schema element
 * strings.
 */
@NotExtensible()
abstract class SchemaElement
         implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -8249972237068748580L;



  /**
   * Skips over any any spaces in the provided string.
   *
   * @param  s         The string in which to skip the spaces.
   * @param  startPos  The position at which to start skipping spaces.
   * @param  length    The position of the end of the string.
   *
   * @return  The position of the next non-space character in the string.
   *
   * @throws  LDAPException  If the end of the string was reached without
   *                         finding a non-space character.
   */
  static int skipSpaces(final String s, final int startPos, final int length)
         throws LDAPException
  {
    int pos = startPos;
    while ((pos < length) && (s.charAt(pos) == ' '))
    {
      pos++;
    }

    if (pos >= length)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_SCHEMA_ELEM_SKIP_SPACES_NO_CLOSE_PAREN.get(
                                   s));
    }

    return pos;
  }



  /**
   * Reads one or more hex-encoded bytes from the specified portion of the RDN
   * string.
   *
   * @param  s         The string from which the data is to be read.
   * @param  startPos  The position at which to start reading.  This should be
   *                   the first hex character immediately after the initial
   *                   backslash.
   * @param  length    The position of the end of the string.
   * @param  buffer    The buffer to which the decoded string portion should be
   *                   appended.
   *
   * @return  The position at which the caller may resume parsing.
   *
   * @throws  LDAPException  If a problem occurs while reading hex-encoded
   *                         bytes.
   */
  private static int readEscapedHexString(final String s, final int startPos,
                                          final int length,
                                          final StringBuilder buffer)
          throws LDAPException
  {
    int pos    = startPos;

    final ByteBuffer byteBuffer = ByteBuffer.allocate(length - pos);
    while (pos < length)
    {
      byte b;
      switch (s.charAt(pos++))
      {
        case '0':
          b = 0x00;
          break;
        case '1':
          b = 0x10;
          break;
        case '2':
          b = 0x20;
          break;
        case '3':
          b = 0x30;
          break;
        case '4':
          b = 0x40;
          break;
        case '5':
          b = 0x50;
          break;
        case '6':
          b = 0x60;
          break;
        case '7':
          b = 0x70;
          break;
        case '8':
          b = (byte) 0x80;
          break;
        case '9':
          b = (byte) 0x90;
          break;
        case 'a':
        case 'A':
          b = (byte) 0xA0;
          break;
        case 'b':
        case 'B':
          b = (byte) 0xB0;
          break;
        case 'c':
        case 'C':
          b = (byte) 0xC0;
          break;
        case 'd':
        case 'D':
          b = (byte) 0xD0;
          break;
        case 'e':
        case 'E':
          b = (byte) 0xE0;
          break;
        case 'f':
        case 'F':
          b = (byte) 0xF0;
          break;
        default:
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_SCHEMA_ELEM_INVALID_HEX_CHAR.get(s,
                                       s.charAt(pos-1), (pos-1)));
      }

      if (pos >= length)
      {
        throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                ERR_SCHEMA_ELEM_MISSING_HEX_CHAR.get(s));
      }

      switch (s.charAt(pos++))
      {
        case '0':
          // No action is required.
          break;
        case '1':
          b |= 0x01;
          break;
        case '2':
          b |= 0x02;
          break;
        case '3':
          b |= 0x03;
          break;
        case '4':
          b |= 0x04;
          break;
        case '5':
          b |= 0x05;
          break;
        case '6':
          b |= 0x06;
          break;
        case '7':
          b |= 0x07;
          break;
        case '8':
          b |= 0x08;
          break;
        case '9':
          b |= 0x09;
          break;
        case 'a':
        case 'A':
          b |= 0x0A;
          break;
        case 'b':
        case 'B':
          b |= 0x0B;
          break;
        case 'c':
        case 'C':
          b |= 0x0C;
          break;
        case 'd':
        case 'D':
          b |= 0x0D;
          break;
        case 'e':
        case 'E':
          b |= 0x0E;
          break;
        case 'f':
        case 'F':
          b |= 0x0F;
          break;
        default:
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_SCHEMA_ELEM_INVALID_HEX_CHAR.get(s,
                                       s.charAt(pos-1), (pos-1)));
      }

      byteBuffer.put(b);
      if (((pos+1) < length) && (s.charAt(pos) == '\\') &&
          isHex(s.charAt(pos+1)))
      {
        // It appears that there are more hex-encoded bytes to follow, so keep
        // reading.
        pos++;
        continue;
      }
      else
      {
        break;
      }
    }

    byteBuffer.flip();
    final byte[] byteArray = new byte[byteBuffer.limit()];
    byteBuffer.get(byteArray);

    try
    {
      buffer.append(toUTF8String(byteArray));
    }
    catch (final Exception e)
    {
      debugException(e);
      // This should never happen.
      buffer.append(new String(byteArray));
    }

    return pos;
  }



  /**
   * Reads a single-quoted string from the provided string.
   *
   * @param  s         The string from which to read the single-quoted string.
   * @param  startPos  The position at which to start reading.
   * @param  length    The position of the end of the string.
   * @param  buffer    The buffer into which the single-quoted string should be
   *                   placed (without the surrounding single quotes).
   *
   * @return  The position of the first space immediately following the closing
   *          quote.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         read the single-quoted string.
   */
  static int readQDString(final String s, final int startPos, final int length,
                          final StringBuilder buffer)
      throws LDAPException
  {
    // The first character must be a single quote.
    if (s.charAt(startPos) != '\'')
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_SCHEMA_ELEM_EXPECTED_SINGLE_QUOTE.get(s,
                                   startPos));
    }

    // Read until we find the next closing quote.  If we find any hex-escaped
    // characters along the way, then decode them.
    int pos = startPos + 1;
    while (pos < length)
    {
      final char c = s.charAt(pos++);
      if (c == '\'')
      {
        // This is the end of the quoted string.
        break;
      }
      else if (c == '\\')
      {
        // This designates the beginning of one or more hex-encoded bytes.
        if (pos >= length)
        {
          throw new LDAPException(ResultCode.DECODING_ERROR,
                                  ERR_SCHEMA_ELEM_ENDS_WITH_BACKSLASH.get(s));
        }

        pos = readEscapedHexString(s, pos, length, buffer);
      }
      else
      {
        buffer.append(c);
      }
    }

    if ((pos >= length) || ((s.charAt(pos) != ' ') && (s.charAt(pos) != ')')))
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_SCHEMA_ELEM_NO_CLOSING_PAREN.get(s));
    }

    if (buffer.length() == 0)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_SCHEMA_ELEM_EMPTY_QUOTES.get(s));
    }

    return pos;
  }



  /**
   * Reads one a set of one or more single-quoted strings from the provided
   * string.  The value to read may be either a single string enclosed in
   * single quotes, or an opening parenthesis followed by a space followed by
   * one or more space-delimited single-quoted strings, followed by a space and
   * a closing parenthesis.
   *
   * @param  s          The string from which to read the single-quoted strings.
   * @param  startPos   The position at which to start reading.
   * @param  length     The position of the end of the string.
   * @param  valueList  The list into which the values read may be placed.
   *
   * @return  The position of the first space immediately following the end of
   *          the values.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         read the single-quoted strings.
   */
  static int readQDStrings(final String s, final int startPos, final int length,
                           final ArrayList<String> valueList)
      throws LDAPException
  {
    // Look at the first character.  It must be either a single quote or an
    // opening parenthesis.
    char c = s.charAt(startPos);
    if (c == '\'')
    {
      // It's just a single value, so use the readQDString method to get it.
      final StringBuilder buffer = new StringBuilder();
      final int returnPos = readQDString(s, startPos, length, buffer);
      valueList.add(buffer.toString());
      return returnPos;
    }
    else if (c == '(')
    {
      int pos = startPos + 1;
      while (true)
      {
        pos = skipSpaces(s, pos, length);
        c = s.charAt(pos);
        if (c == ')')
        {
          // This is the end of the value list.
          pos++;
          break;
        }
        else if (c == '\'')
        {
          // This is the next value in the list.
          final StringBuilder buffer = new StringBuilder();
          pos = readQDString(s, pos, length, buffer);
          valueList.add(buffer.toString());
        }
        else
        {
          throw new LDAPException(ResultCode.DECODING_ERROR,
                                  ERR_SCHEMA_ELEM_EXPECTED_QUOTE_OR_PAREN.get(
                                       s, startPos));
        }
      }

      if (valueList.isEmpty())
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
                                ERR_SCHEMA_ELEM_EMPTY_STRING_LIST.get(s));
      }

      if ((pos >= length) ||
          ((s.charAt(pos) != ' ') && (s.charAt(pos) != ')')))
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
                                ERR_SCHEMA_ELEM_NO_SPACE_AFTER_QUOTE.get(s));
      }

      return pos;
    }
    else
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_SCHEMA_ELEM_EXPECTED_QUOTE_OR_PAREN.get(s,
                                   startPos));
    }
  }



  /**
   * Reads an OID value from the provided string.  The OID value may be either a
   * numeric OID or a string name.  This implementation will be fairly lenient
   * with regard to the set of characters that may be present, and it will
   * allow the OID to be enclosed in single quotes.
   *
   * @param  s         The string from which to read the OID string.
   * @param  startPos  The position at which to start reading.
   * @param  length    The position of the end of the string.
   * @param  buffer    The buffer into which the OID string should be placed.
   *
   * @return  The position of the first space immediately following the OID
   *          string.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         read the OID string.
   */
  static int readOID(final String s, final int startPos, final int length,
                     final StringBuilder buffer)
      throws LDAPException
  {
    // Read until we find the first space.
    int pos = startPos;
    boolean lastWasQuote = false;
    while (pos < length)
    {
      final char c = s.charAt(pos);
      if ((c == ' ') || (c == '$') || (c == ')'))
      {
        if (buffer.length() == 0)
        {
          throw new LDAPException(ResultCode.DECODING_ERROR,
                                  ERR_SCHEMA_ELEM_EMPTY_OID.get(s));
        }

        return pos;
      }
      else if (((c >= 'a') && (c <= 'z')) ||
               ((c >= 'A') && (c <= 'Z')) ||
               ((c >= '0') && (c <= '9')) ||
               (c == '-') || (c == '.') || (c == '_') ||
               (c == '{') || (c == '}'))
      {
        if (lastWasQuote)
        {
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_SCHEMA_ELEM_UNEXPECTED_CHAR_IN_OID.get(s, (pos-1)));
        }

        buffer.append(c);
      }
      else if (c == '\'')
      {
        if (buffer.length() != 0)
        {
          lastWasQuote = true;
        }
      }
      else
      {
          throw new LDAPException(ResultCode.DECODING_ERROR,
                                  ERR_SCHEMA_ELEM_UNEXPECTED_CHAR_IN_OID.get(s,
                                       pos));
      }

      pos++;
    }


    // We hit the end of the string before finding a space.
    throw new LDAPException(ResultCode.DECODING_ERROR,
                            ERR_SCHEMA_ELEM_NO_SPACE_AFTER_OID.get(s));
  }



  /**
   * Reads one a set of one or more OID strings from the provided string.  The
   * value to read may be either a single OID string or an opening parenthesis
   * followed by a space followed by one or more space-delimited OID strings,
   * followed by a space and a closing parenthesis.
   *
   * @param  s          The string from which to read the OID strings.
   * @param  startPos   The position at which to start reading.
   * @param  length     The position of the end of the string.
   * @param  valueList  The list into which the values read may be placed.
   *
   * @return  The position of the first space immediately following the end of
   *          the values.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         read the OID strings.
   */
  static int readOIDs(final String s, final int startPos, final int length,
                      final ArrayList<String> valueList)
      throws LDAPException
  {
    // Look at the first character.  If it's an opening parenthesis, then read
    // a list of OID strings.  Otherwise, just read a single string.
    char c = s.charAt(startPos);
    if (c == '(')
    {
      int pos = startPos + 1;
      while (true)
      {
        pos = skipSpaces(s, pos, length);
        c = s.charAt(pos);
        if (c == ')')
        {
          // This is the end of the value list.
          pos++;
          break;
        }
        else if (c == '$')
        {
          // This is the delimiter before the next value in the list.
          pos++;
          pos = skipSpaces(s, pos, length);
          final StringBuilder buffer = new StringBuilder();
          pos = readOID(s, pos, length, buffer);
          valueList.add(buffer.toString());
        }
        else if (valueList.isEmpty())
        {
          // This is the first value in the list.
          final StringBuilder buffer = new StringBuilder();
          pos = readOID(s, pos, length, buffer);
          valueList.add(buffer.toString());
        }
        else
        {
          throw new LDAPException(ResultCode.DECODING_ERROR,
                         ERR_SCHEMA_ELEM_UNEXPECTED_CHAR_IN_OID_LIST.get(s,
                              pos));
        }
      }

      if (valueList.isEmpty())
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
                                ERR_SCHEMA_ELEM_EMPTY_OID_LIST.get(s));
      }

      if (pos >= length)
      {
        // Technically, there should be a space after the closing parenthesis,
        // but there are known cases in which servers (like Active Directory)
        // omit this space, so we'll be lenient and allow a missing space.  But
        // it can't possibly be the end of the schema element definition, so
        // that's still an error.
        throw new LDAPException(ResultCode.DECODING_ERROR,
                                ERR_SCHEMA_ELEM_NO_SPACE_AFTER_OID_LIST.get(s));
      }

      return pos;
    }
    else
    {
      final StringBuilder buffer = new StringBuilder();
      final int returnPos = readOID(s, startPos, length, buffer);
      valueList.add(buffer.toString());
      return returnPos;
    }
  }



  /**
   * Appends a properly-encoded representation of the provided value to the
   * given buffer.
   *
   * @param  value   The value to be encoded and placed in the buffer.
   * @param  buffer  The buffer to which the encoded value is to be appended.
   */
  static void encodeValue(final String value, final StringBuilder buffer)
  {
    final int length = value.length();
    for (int i=0; i < length; i++)
    {
      final char c = value.charAt(i);
      if ((c < ' ') || (c > '~') || (c == '\\') || (c == '\''))
      {
        hexEncode(c, buffer);
      }
      else
      {
        buffer.append(c);
      }
    }
  }



  /**
   * Retrieves a string representation of this schema element, in the format
   * described in RFC 4512.
   *
   * @return  A string representation of this schema element, in the format
   *          described in RFC 4512.
   */
  @Override()
  public abstract String toString();
}