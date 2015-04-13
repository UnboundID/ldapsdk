/*
 * Copyright 2007-2013 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2013 UnboundID Corp.
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
package com.unboundid.ldap.sdk;



import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.matchingrules.MatchingRule;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.ldif.LDIFRecord;
import com.unboundid.ldif.LDIFWriter;
import com.unboundid.util.ByteStringBuffer;
import com.unboundid.util.Mutable;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class provides a data structure for holding information about an LDAP
 * entry.  An entry contains a distinguished name (DN) and a set of attributes.
 * An entry can be created from these components, and it can also be created
 * from its LDIF representation as described in
 * <A HREF="http://www.ietf.org/rfc/rfc2849.txt">RFC 2849</A>.  For example:
 * <BR><BR>
 * <PRE>
 *   Entry entry = new Entry(
 *     "dn: dc=example,dc=com",
 *     "objectClass: top",
 *     "objectClass: domain",
 *     "dc: example");
 * </PRE>
 * <BR><BR>
 * This class also provides methods for retrieving the LDIF representation of
 * an entry, either as a single string or as an array of strings that make up
 * the LDIF lines.
 * <BR><BR>
 * The {@link Entry#diff} method may be used to obtain the set of differences
 * between two entries, and to retrieve a list of {@link Modification} objects
 * that can be used to modify one entry so that it contains the same set of
 * data as another.  The {@link Entry#applyModifications} method may be used to
 * apply a set of modifications to an entry.
 * <BR><BR>
 * Entry objects are mutable, and the DN, set of attributes, and individual
 * attribute values can be altered.
 */
@Mutable()
@NotExtensible()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public class Entry
       implements LDIFRecord
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -4438809025903729197L;



  // The parsed DN for this entry.
  private volatile DN parsedDN;

  // The set of attributes for this entry.
  private final LinkedHashMap<String,Attribute> attributes;

  // The schema to use for this entry.
  private final Schema schema;

  // The DN for this entry.
  private String dn;



  /**
   * Creates a new entry with the provided DN and no attributes.
   *
   * @param  dn  The DN for this entry.  It must not be {@code null}.
   */
  public Entry(final String dn)
  {
    this(dn, (Schema) null);
  }



  /**
   * Creates a new entry with the provided DN and no attributes.
   *
   * @param  dn      The DN for this entry.  It must not be {@code null}.
   * @param  schema  The schema to use for operations involving this entry.  It
   *                 may be {@code null} if no schema is available.
   */
  public Entry(final String dn, final Schema schema)
  {
    ensureNotNull(dn);

    this.dn     = dn;
    this.schema = schema;

    attributes = new LinkedHashMap<String,Attribute>();
  }



  /**
   * Creates a new entry with the provided DN and no attributes.
   *
   * @param  dn  The DN for this entry.  It must not be {@code null}.
   */
  public Entry(final DN dn)
  {
    this(dn, (Schema) null);
  }



  /**
   * Creates a new entry with the provided DN and no attributes.
   *
   * @param  dn      The DN for this entry.  It must not be {@code null}.
   * @param  schema  The schema to use for operations involving this entry.  It
   *                 may be {@code null} if no schema is available.
   */
  public Entry(final DN dn, final Schema schema)
  {
    ensureNotNull(dn);

    parsedDN    = dn;
    this.dn     = parsedDN.toString();
    this.schema = schema;

    attributes = new LinkedHashMap<String,Attribute>();
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final String dn, final Attribute... attributes)
  {
    this(dn, null, attributes);
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  schema      The schema to use for operations involving this entry.
   *                     It may be {@code null} if no schema is available.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final String dn, final Schema schema,
               final Attribute... attributes)
  {
    ensureNotNull(dn, attributes);

    this.dn     = dn;
    this.schema = schema;

    this.attributes = new LinkedHashMap<String,Attribute>(attributes.length);
    for (final Attribute a : attributes)
    {
      final String name = toLowerCase(a.getName());
      final Attribute attr = this.attributes.get(name);
      if (attr == null)
      {
        this.attributes.put(name, a);
      }
      else
      {
        this.attributes.put(name, Attribute.mergeAttributes(attr, a));
      }
    }
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final DN dn, final Attribute... attributes)
  {
    this(dn, null, attributes);
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  schema      The schema to use for operations involving this entry.
   *                     It may be {@code null} if no schema is available.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final DN dn, final Schema schema, final Attribute... attributes)
  {
    ensureNotNull(dn, attributes);

    parsedDN    = dn;
    this.dn     = parsedDN.toString();
    this.schema = schema;

    this.attributes = new LinkedHashMap<String,Attribute>(attributes.length);
    for (final Attribute a : attributes)
    {
      final String name = toLowerCase(a.getName());
      final Attribute attr = this.attributes.get(name);
      if (attr == null)
      {
        this.attributes.put(name, a);
      }
      else
      {
        this.attributes.put(name, Attribute.mergeAttributes(attr, a));
      }
    }
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final String dn, final Collection<Attribute> attributes)
  {
    this(dn, null, attributes);
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  schema      The schema to use for operations involving this entry.
   *                     It may be {@code null} if no schema is available.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final String dn, final Schema schema,
               final Collection<Attribute> attributes)
  {
    ensureNotNull(dn, attributes);

    this.dn     = dn;
    this.schema = schema;

    this.attributes = new LinkedHashMap<String,Attribute>(attributes.size());
    for (final Attribute a : attributes)
    {
      final String name = toLowerCase(a.getName());
      final Attribute attr = this.attributes.get(name);
      if (attr == null)
      {
        this.attributes.put(name, a);
      }
      else
      {
        this.attributes.put(name, Attribute.mergeAttributes(attr, a));
      }
    }
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final DN dn, final Collection<Attribute> attributes)
  {
    this(dn, null, attributes);
  }



  /**
   * Creates a new entry with the provided DN and set of attributes.
   *
   * @param  dn          The DN for this entry.  It must not be {@code null}.
   * @param  schema      The schema to use for operations involving this entry.
   *                     It may be {@code null} if no schema is available.
   * @param  attributes  The set of attributes for this entry.  It must not be
   *                     {@code null}.
   */
  public Entry(final DN dn, final Schema schema,
               final Collection<Attribute> attributes)
  {
    ensureNotNull(dn, attributes);

    parsedDN    = dn;
    this.dn     = parsedDN.toString();
    this.schema = schema;

    this.attributes = new LinkedHashMap<String,Attribute>(attributes.size());
    for (final Attribute a : attributes)
    {
      final String name = toLowerCase(a.getName());
      final Attribute attr = this.attributes.get(name);
      if (attr == null)
      {
        this.attributes.put(name, a);
      }
      else
      {
        this.attributes.put(name, Attribute.mergeAttributes(attr, a));
      }
    }
  }



  /**
   * Creates a new entry from the provided LDIF representation.
   *
   * @param  entryLines  The set of lines that comprise an LDIF representation
   *                     of the entry.  It must not be {@code null} or empty.
   *
   * @throws  LDIFException  If the provided lines cannot be decoded as an entry
   *                         in LDIF format.
   */
  public Entry(final String... entryLines)
         throws LDIFException
  {
    this(null, entryLines);
  }



  /**
   * Creates a new entry from the provided LDIF representation.
   *
   * @param  schema      The schema to use for operations involving this entry.
   *                     It may be {@code null} if no schema is available.
   * @param  entryLines  The set of lines that comprise an LDIF representation
   *                     of the entry.  It must not be {@code null} or empty.
   *
   * @throws  LDIFException  If the provided lines cannot be decoded as an entry
   *                         in LDIF format.
   */
  public Entry(final Schema schema, final String... entryLines)
         throws LDIFException
  {
    final Entry e = LDIFReader.decodeEntry(entryLines);

    this.schema = schema;

    dn         = e.dn;
    parsedDN   = e.parsedDN;
    attributes = e.attributes;
  }



  /**
   * Retrieves the DN for this entry.
   *
   * @return  The DN for this entry.
   */
  public final String getDN()
  {
    return dn;
  }



  /**
   * Specifies the DN for this entry.
   *
   * @param  dn  The DN for this entry.  It must not be {@code null}.
   */
  public void setDN(final String dn)
  {
    ensureNotNull(dn);

    this.dn = dn;
    parsedDN = null;
  }



  /**
   * Specifies the DN for this entry.
   *
   * @param  dn  The DN for this entry.  It must not be {@code null}.
   */
  public void setDN(final DN dn)
  {
    ensureNotNull(dn);

    parsedDN = dn;
    this.dn  = parsedDN.toString();
  }



  /**
   * Retrieves the parsed DN for this entry.
   *
   * @return  The parsed DN for this entry.
   *
   * @throws  LDAPException  If the DN string cannot be parsed as a valid DN.
   */
  public final DN getParsedDN()
         throws LDAPException
  {
    if (parsedDN == null)
    {
      parsedDN = new DN(dn, schema);
    }

    return parsedDN;
  }



  /**
   * Retrieves the RDN for this entry.
   *
   * @return  The RDN for this entry, or {@code null} if the DN is the null DN.
   *
   * @throws  LDAPException  If the DN string cannot be parsed as a valid DN.
   */
  public final RDN getRDN()
         throws LDAPException
  {
    return getParsedDN().getRDN();
  }



  /**
   * Retrieves the parent DN for this entry.
   *
   * @return  The parent DN for this entry, or {@code null} if there is no
   *          parent.
   *
   * @throws  LDAPException  If the DN string cannot be parsed as a valid DN.
   */
  public final DN getParentDN()
         throws LDAPException
  {
    if (parsedDN == null)
    {
      parsedDN = new DN(dn, schema);
    }

    return parsedDN.getParent();
  }



  /**
   * Retrieves the parent DN for this entry as a string.
   *
   * @return  The parent DN for this entry as a string, or {@code null} if there
   *          is no parent.
   *
   * @throws  LDAPException  If the DN string cannot be parsed as a valid DN.
   */
  public final String getParentDNString()
         throws LDAPException
  {
    if (parsedDN == null)
    {
      parsedDN = new DN(dn, schema);
    }

    final DN parentDN = parsedDN.getParent();
    if (parentDN == null)
    {
      return null;
    }
    else
    {
      return parentDN.toString();
    }
  }



  /**
   * Retrieves the schema that will be used for this entry, if any.
   *
   * @return  The schema that will be used for this entry, or {@code null} if
   *          no schema was provided.
   */
  protected Schema getSchema()
  {
    return schema;
  }



  /**
   * Indicates whether this entry contains the specified attribute.
   *
   * @param  attributeName  The name of the attribute for which to make the
   *                        determination.  It must not be {@code null}.
   *
   * @return  {@code true} if this entry contains the specified attribute, or
   *          {@code false} if not.
   */
  public final boolean hasAttribute(final String attributeName)
  {
    return hasAttribute(attributeName, schema);
  }



  /**
   * Indicates whether this entry contains the specified attribute.
   *
   * @param  attributeName  The name of the attribute for which to make the
   *                        determination.  It must not be {@code null}.
   * @param  schema         The schema to use to determine whether there may be
   *                        alternate names for the specified attribute.  It may
   *                        be {@code null} if no schema is available.
   *
   * @return  {@code true} if this entry contains the specified attribute, or
   *          {@code false} if not.
   */
  public final boolean hasAttribute(final String attributeName,
                                    final Schema schema)
  {
    ensureNotNull(attributeName);

    if (attributes.containsKey(toLowerCase(attributeName)))
    {
      return true;
    }

    if (schema != null)
    {
      final String baseName;
      final String options;
      final int semicolonPos = attributeName.indexOf(';');
      if (semicolonPos > 0)
      {
        baseName = attributeName.substring(0, semicolonPos);
        options  = toLowerCase(attributeName.substring(semicolonPos));
      }
      else
      {
        baseName = attributeName;
        options  = "";
      }

      final AttributeTypeDefinition at = schema.getAttributeType(baseName);
      if (at != null)
      {
        if (attributes.containsKey(toLowerCase(at.getOID()) + options))
        {
          return true;
        }

        for (final String name : at.getNames())
        {
          if (attributes.containsKey(toLowerCase(name) + options))
          {
            return true;
          }
        }
      }
    }

    return false;
  }



  /**
   * Indicates whether this entry contains the specified attribute.  It will
   * only return {@code true} if this entry contains an attribute with the same
   * name and exact set of values.
   *
   * @param  attribute  The attribute for which to make the determination.  It
   *                    must not be {@code null}.
   *
   * @return  {@code true} if this entry contains the specified attribute, or
   *          {@code false} if not.
   */
  public final boolean hasAttribute(final Attribute attribute)
  {
    ensureNotNull(attribute);

    final String lowerName = toLowerCase(attribute.getName());
    final Attribute attr = attributes.get(lowerName);
    return ((attr != null) && attr.equals(attribute));
  }



  /**
   * Indicates whether this entry contains an attribute with the given name and
   * value.
   *
   * @param  attributeName   The name of the attribute for which to make the
   *                         determination.  It must not be {@code null}.
   * @param  attributeValue  The value for which to make the determination.  It
   *                         must not be {@code null}.
   *
   * @return  {@code true} if this entry contains an attribute with the
   *          specified name and value, or {@code false} if not.
   */
  public final boolean hasAttributeValue(final String attributeName,
                                         final String attributeValue)
  {
    ensureNotNull(attributeName, attributeValue);

    final Attribute attr = attributes.get(toLowerCase(attributeName));
    return ((attr != null) && attr.hasValue(attributeValue));
  }



  /**
   * Indicates whether this entry contains an attribute with the given name and
   * value.
   *
   * @param  attributeName   The name of the attribute for which to make the
   *                         determination.  It must not be {@code null}.
   * @param  attributeValue  The value for which to make the determination.  It
   *                         must not be {@code null}.
   * @param  matchingRule    The matching rule to use to make the determination.
   *                         It must not be {@code null}.
   *
   * @return  {@code true} if this entry contains an attribute with the
   *          specified name and value, or {@code false} if not.
   */
  public final boolean hasAttributeValue(final String attributeName,
                                         final String attributeValue,
                                         final MatchingRule matchingRule)
  {
    ensureNotNull(attributeName, attributeValue);

    final Attribute attr = attributes.get(toLowerCase(attributeName));
    return ((attr != null) && attr.hasValue(attributeValue, matchingRule));
  }



  /**
   * Indicates whether this entry contains an attribute with the given name and
   * value.
   *
   * @param  attributeName   The name of the attribute for which to make the
   *                         determination.  It must not be {@code null}.
   * @param  attributeValue  The value for which to make the determination.  It
   *                         must not be {@code null}.
   *
   * @return  {@code true} if this entry contains an attribute with the
   *          specified name and value, or {@code false} if not.
   */
  public final boolean hasAttributeValue(final String attributeName,
                                         final byte[] attributeValue)
  {
    ensureNotNull(attributeName, attributeValue);

    final Attribute attr = attributes.get(toLowerCase(attributeName));
    return ((attr != null) && attr.hasValue(attributeValue));
  }



  /**
   * Indicates whether this entry contains an attribute with the given name and
   * value.
   *
   * @param  attributeName   The name of the attribute for which to make the
   *                         determination.  It must not be {@code null}.
   * @param  attributeValue  The value for which to make the determination.  It
   *                         must not be {@code null}.
   * @param  matchingRule    The matching rule to use to make the determination.
   *                         It must not be {@code null}.
   *
   * @return  {@code true} if this entry contains an attribute with the
   *          specified name and value, or {@code false} if not.
   */
  public final boolean hasAttributeValue(final String attributeName,
                                         final byte[] attributeValue,
                                         final MatchingRule matchingRule)
  {
    ensureNotNull(attributeName, attributeValue);

    final Attribute attr = attributes.get(toLowerCase(attributeName));
    return ((attr != null) && attr.hasValue(attributeValue, matchingRule));
  }



  /**
   * Indicates whether this entry contains the specified object class.
   *
   * @param  objectClassName  The name of the object class for which to make the
   *                          determination.  It must not be {@code null}.
   *
   * @return  {@code true} if this entry contains the specified object class, or
   *          {@code false} if not.
   */
  public final boolean hasObjectClass(final String objectClassName)
  {
    return hasAttributeValue("objectClass", objectClassName);
  }



  /**
   * Retrieves the set of attributes contained in this entry.
   *
   * @return  The set of attributes contained in this entry.
   */
  public final Collection<Attribute> getAttributes()
  {
    return Collections.unmodifiableCollection(attributes.values());
  }



  /**
   * Retrieves the attribute with the specified name.
   *
   * @param  attributeName  The name of the attribute to retrieve.  It must not
   *                        be {@code null}.
   *
   * @return  The requested attribute from this entry, or {@code null} if the
   *          specified attribute is not present in this entry.
   */
  public final Attribute getAttribute(final String attributeName)
  {
    return getAttribute(attributeName, schema);
  }



  /**
   * Retrieves the attribute with the specified name.
   *
   * @param  attributeName  The name of the attribute to retrieve.  It must not
   *                        be {@code null}.
   * @param  schema         The schema to use to determine whether there may be
   *                        alternate names for the specified attribute.  It may
   *                        be {@code null} if no schema is available.
   *
   * @return  The requested attribute from this entry, or {@code null} if the
   *          specified attribute is not present in this entry.
   */
  public final Attribute getAttribute(final String attributeName,
                                      final Schema schema)
  {
    ensureNotNull(attributeName);

    Attribute a = attributes.get(toLowerCase(attributeName));
    if ((a == null) && (schema != null))
    {
      final String baseName;
      final String options;
      final int semicolonPos = attributeName.indexOf(';');
      if (semicolonPos > 0)
      {
        baseName = attributeName.substring(0, semicolonPos);
        options  = toLowerCase(attributeName.substring(semicolonPos));
      }
      else
      {
        baseName = attributeName;
        options  = "";
      }

      final AttributeTypeDefinition at = schema.getAttributeType(baseName);
      if (at == null)
      {
        return null;
      }

      a = attributes.get(toLowerCase(at.getOID() + options));
      if (a == null)
      {
        for (final String name : at.getNames())
        {
          a = attributes.get(toLowerCase(name) + options);
          if (a != null)
          {
            return a;
          }
        }
      }

      return a;
    }
    else
    {
      return a;
    }
  }



  /**
   * Retrieves the list of attributes with the given base name and all of the
   * specified options.
   *
   * @param  baseName  The base name (without any options) for the attribute to
   *                   retrieve.  It must not be {@code null}.
   * @param  options   The set of options that should be included in the
   *                   attributes that are returned.  It may be empty or
   *                   {@code null} if all attributes with the specified base
   *                   name should be returned, regardless of the options that
   *                   they contain (if any).
   *
   * @return  The list of attributes with the given base name and all of the
   *          specified options.  It may be empty if there are no attributes
   *          with the specified base name and set of options.
   */
  public final List<Attribute> getAttributesWithOptions(final String baseName,
                                    final Set<String> options)
  {
    ensureNotNull(baseName);

    final ArrayList<Attribute> attrList = new ArrayList<Attribute>(10);

    for (final Attribute a : attributes.values())
    {
      if (a.getBaseName().equalsIgnoreCase(baseName))
      {
        if ((options == null) || options.isEmpty())
        {
          attrList.add(a);
        }
        else
        {
          boolean allFound = true;
          for (final String option : options)
          {
            if (! a.hasOption(option))
            {
              allFound = false;
              break;
            }
          }

          if (allFound)
          {
            attrList.add(a);
          }
        }
      }
    }

    return Collections.unmodifiableList(attrList);
  }



  /**
   * Retrieves the value for the specified attribute, if available.  If the
   * attribute has more than one value, then the first value will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The value for the specified attribute, or {@code null} if that
   *          attribute is not available.
   */
  public String getAttributeValue(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValue();
    }
  }



  /**
   * Retrieves the value for the specified attribute as a byte array, if
   * available.  If the attribute has more than one value, then the first value
   * will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The value for the specified attribute as a byte array, or
   *          {@code null} if that attribute is not available.
   */
  public byte[] getAttributeValueBytes(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueByteArray();
    }
  }



  /**
   * Retrieves the value for the specified attribute as a Boolean, if available.
   * If the attribute has more than one value, then the first value will be
   * returned.  Values of "true", "t", "yes", "y", "on", and "1" will be
   * interpreted as {@code TRUE}.  Values of "false", "f", "no", "n", "off", and
   * "0" will be interpreted as {@code FALSE}.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The Boolean value parsed from the specified attribute, or
   *          {@code null} if that attribute is not available or the value
   *          cannot be parsed as a Boolean.
   */
  public Boolean getAttributeValueAsBoolean(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueAsBoolean();
    }
  }



  /**
   * Retrieves the value for the specified attribute as a Date, formatted using
   * the generalized time syntax, if available.  If the attribute has more than
   * one value, then the first value will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The Date value parsed from the specified attribute, or
   *           {@code null} if that attribute is not available or the value
   *           cannot be parsed as a Date.
   */
  public Date getAttributeValueAsDate(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueAsDate();
    }
  }



  /**
   * Retrieves the value for the specified attribute as a DN, if available.  If
   * the attribute has more than one value, then the first value will be
   * returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The DN value parsed from the specified attribute, or {@code null}
   *          if that attribute is not available or the value cannot be parsed
   *          as a DN.
   */
  public DN getAttributeValueAsDN(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueAsDN();
    }
  }



  /**
   * Retrieves the value for the specified attribute as an Integer, if
   * available.  If the attribute has more than one value, then the first value
   * will be returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The Integer value parsed from the specified attribute, or
   *          {@code null} if that attribute is not available or the value
   *          cannot be parsed as an Integer.
   */
  public Integer getAttributeValueAsInteger(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueAsInteger();
    }
  }



  /**
   * Retrieves the value for the specified attribute as a Long, if available.
   * If the attribute has more than one value, then the first value will be
   * returned.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.  It must not be {@code null}.
   *
   * @return  The Long value parsed from the specified attribute, or
   *          {@code null} if that attribute is not available or the value
   *          cannot be parsed as a Long.
   */
  public Long getAttributeValueAsLong(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueAsLong();
    }
  }



  /**
   * Retrieves the set of values for the specified attribute, if available.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        values.  It must not be {@code null}.
   *
   * @return  The set of values for the specified attribute, or {@code null} if
   *          that attribute is not available.
   */
  public String[] getAttributeValues(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValues();
    }
  }



  /**
   * Retrieves the set of values for the specified attribute as byte arrays, if
   * available.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        values.  It must not be {@code null}.
   *
   * @return  The set of values for the specified attribute as byte arrays, or
   *          {@code null} if that attribute is not available.
   */
  public byte[][] getAttributeValueByteArrays(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Attribute a = attributes.get(toLowerCase(attributeName));
    if (a == null)
    {
      return null;
    }
    else
    {
      return a.getValueByteArrays();
    }
  }



  /**
   * Retrieves the "objectClass" attribute from the entry, if available.
   *
   * @return  The "objectClass" attribute from the entry, or {@code null} if
   *          that attribute not available.
   */
  public final Attribute getObjectClassAttribute()
  {
    return getAttribute("objectClass");
  }



  /**
   * Retrieves the values of the "objectClass" attribute from the entry, if
   * available.
   *
   * @return  The values of the "objectClass" attribute from the entry, or
   *          {@code null} if that attribute is not available.
   */
  public final String[] getObjectClassValues()
  {
    return getAttributeValues("objectClass");
  }



  /**
   * Adds the provided attribute to this entry.  If this entry already contains
   * an attribute with the same name, then their values will be merged.
   *
   * @param  attribute  The attribute to be added.  It must not be {@code null}.
   *
   * @return  {@code true} if the entry was updated, or {@code false} because
   *          the specified attribute already existed with all provided values.
   */
  public boolean addAttribute(final Attribute attribute)
  {
    ensureNotNull(attribute);

    final String lowerName = toLowerCase(attribute.getName());
    final Attribute attr = attributes.get(lowerName);
    if (attr == null)
    {
      attributes.put(lowerName, attribute);
      return true;
    }
    else
    {
      final Attribute newAttr = Attribute.mergeAttributes(attr, attribute);
      attributes.put(lowerName, newAttr);
      return (attr.getRawValues().length != newAttr.getRawValues().length);
    }
  }



  /**
   * Adds the specified attribute value to this entry, if it is not already
   * present.
   *
   * @param  attributeName   The name for the attribute to be added.  It must
   *                         not be {@code null}.
   * @param  attributeValue  The value for the attribute to be added.  It must
   *                         not be {@code null}.
   *
   * @return  {@code true} if the entry was updated, or {@code false} because
   *          the specified attribute already existed with the given value.
   */
  public boolean addAttribute(final String attributeName,
                              final String attributeValue)
  {
    ensureNotNull(attributeName, attributeValue);
    return addAttribute(new Attribute(attributeName, schema, attributeValue));
  }



  /**
   * Adds the specified attribute value to this entry, if it is not already
   * present.
   *
   * @param  attributeName   The name for the attribute to be added.  It must
   *                         not be {@code null}.
   * @param  attributeValue  The value for the attribute to be added.  It must
   *                         not be {@code null}.
   *
   * @return  {@code true} if the entry was updated, or {@code false} because
   *          the specified attribute already existed with the given value.
   */
  public boolean addAttribute(final String attributeName,
                              final byte[] attributeValue)
  {
    ensureNotNull(attributeName, attributeValue);
    return addAttribute(new Attribute(attributeName, schema, attributeValue));
  }



  /**
   * Adds the provided attribute to this entry.  If this entry already contains
   * an attribute with the same name, then their values will be merged.
   *
   * @param  attributeName    The name for the attribute to be added.  It must
   *                          not be {@code null}.
   * @param  attributeValues  The value for the attribute to be added.  It must
   *                          not be {@code null}.
   *
   * @return  {@code true} if the entry was updated, or {@code false} because
   *          the specified attribute already existed with all provided values.
   */
  public boolean addAttribute(final String attributeName,
                              final String... attributeValues)
  {
    ensureNotNull(attributeName, attributeValues);
    return addAttribute(new Attribute(attributeName, schema, attributeValues));
  }



  /**
   * Adds the provided attribute to this entry.  If this entry already contains
   * an attribute with the same name, then their values will be merged.
   *
   * @param  attributeName    The name for the attribute to be added.  It must
   *                          not be {@code null}.
   * @param  attributeValues  The value for the attribute to be added.  It must
   *                          not be {@code null}.
   *
   * @return  {@code true} if the entry was updated, or {@code false} because
   *          the specified attribute already existed with all provided values.
   */
  public boolean addAttribute(final String attributeName,
                              final byte[]... attributeValues)
  {
    ensureNotNull(attributeName, attributeValues);
    return addAttribute(new Attribute(attributeName, schema, attributeValues));
  }



  /**
   * Removes the specified attribute from this entry.
   *
   * @param  attributeName  The name of the attribute to remove.  It must not be
   *                        {@code null}.
   *
   * @return  {@code true} if the attribute was removed from the entry, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttribute(final String attributeName)
  {
    ensureNotNull(attributeName);

    if (schema == null)
    {
      return (attributes.remove(toLowerCase(attributeName)) != null);
    }
    else
    {
      final Attribute a = getAttribute(attributeName,  schema);
      if (a == null)
      {
        return false;
      }
      else
      {
        attributes.remove(toLowerCase(a.getName()));
        return true;
      }
    }
  }



  /**
   * Removes the specified attribute value from this entry if it is present.  If
   * it is the last value for the attribute, then the entire attribute will be
   * removed.  If the specified value is not present, then no change will be
   * made.
   *
   * @param  attributeName   The name of the attribute from which to remove the
   *                         value.  It must not be {@code null}.
   * @param  attributeValue  The value to remove from the attribute.  It must
   *                         not be {@code null}.
   *
   * @return  {@code true} if the attribute value was removed from the entry, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttributeValue(final String attributeName,
                                      final String attributeValue)
  {
    return removeAttributeValue(attributeName, attributeValue, null);
  }



  /**
   * Removes the specified attribute value from this entry if it is present.  If
   * it is the last value for the attribute, then the entire attribute will be
   * removed.  If the specified value is not present, then no change will be
   * made.
   *
   * @param  attributeName   The name of the attribute from which to remove the
   *                         value.  It must not be {@code null}.
   * @param  attributeValue  The value to remove from the attribute.  It must
   *                         not be {@code null}.
   * @param  matchingRule    The matching rule to use for the attribute.  It may
   *                         be {@code null} to use the matching rule associated
   *                         with the attribute.
   *
   * @return  {@code true} if the attribute value was removed from the entry, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttributeValue(final String attributeName,
                                      final String attributeValue,
                                      final MatchingRule matchingRule)
  {
    ensureNotNull(attributeName, attributeValue);

    final Attribute attr = getAttribute(attributeName, schema);
    if (attr == null)
    {
      return false;
    }
    else
    {
      final String lowerName = toLowerCase(attr.getName());
      final Attribute newAttr = Attribute.removeValues(attr,
           new Attribute(attributeName, attributeValue), matchingRule);
      if (newAttr.hasValue())
      {
        attributes.put(lowerName, newAttr);
      }
      else
      {
        attributes.remove(lowerName);
      }

      return (attr.getRawValues().length != newAttr.getRawValues().length);
    }
  }



  /**
   * Removes the specified attribute value from this entry if it is present.  If
   * it is the last value for the attribute, then the entire attribute will be
   * removed.  If the specified value is not present, then no change will be
   * made.
   *
   * @param  attributeName   The name of the attribute from which to remove the
   *                         value.  It must not be {@code null}.
   * @param  attributeValue  The value to remove from the attribute.  It must
   *                         not be {@code null}.
   *
   * @return  {@code true} if the attribute value was removed from the entry, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttributeValue(final String attributeName,
                                      final byte[] attributeValue)
  {
    return removeAttributeValue(attributeName, attributeValue, null);
  }



  /**
   * Removes the specified attribute value from this entry if it is present.  If
   * it is the last value for the attribute, then the entire attribute will be
   * removed.  If the specified value is not present, then no change will be
   * made.
   *
   * @param  attributeName   The name of the attribute from which to remove the
   *                         value.  It must not be {@code null}.
   * @param  attributeValue  The value to remove from the attribute.  It must
   *                         not be {@code null}.
   * @param  matchingRule    The matching rule to use for the attribute.  It may
   *                         be {@code null} to use the matching rule associated
   *                         with the attribute.
   *
   * @return  {@code true} if the attribute value was removed from the entry, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttributeValue(final String attributeName,
                                      final byte[] attributeValue,
                                      final MatchingRule matchingRule)
  {
    ensureNotNull(attributeName, attributeValue);

    final Attribute attr = getAttribute(attributeName, schema);
    if (attr == null)
    {
      return false;
    }
    else
    {
      final String lowerName = toLowerCase(attr.getName());
      final Attribute newAttr = Attribute.removeValues(attr,
           new Attribute(attributeName, attributeValue), matchingRule);
      if (newAttr.hasValue())
      {
        attributes.put(lowerName, newAttr);
      }
      else
      {
        attributes.remove(lowerName);
      }

      return (attr.getRawValues().length != newAttr.getRawValues().length);
    }
  }



  /**
   * Removes the specified attribute values from this entry if they are present.
   * If the attribute does not have any remaining values, then the entire
   * attribute will be removed.  If any of the provided values are not present,
   * then they will be ignored.
   *
   * @param  attributeName    The name of the attribute from which to remove the
   *                          values.  It must not be {@code null}.
   * @param  attributeValues  The set of values to remove from the attribute.
   *                          It must not be {@code null}.
   *
   * @return  {@code true} if any attribute values were removed from the entry,
   *          or {@code false} none of them were present.
   */
  public boolean removeAttributeValues(final String attributeName,
                                       final String... attributeValues)
  {
    ensureNotNull(attributeName, attributeValues);

    final Attribute attr = getAttribute(attributeName, schema);
    if (attr == null)
    {
      return false;
    }
    else
    {
      final String lowerName = toLowerCase(attr.getName());
      final Attribute newAttr = Attribute.removeValues(attr,
           new Attribute(attributeName, attributeValues));
      if (newAttr.hasValue())
      {
        attributes.put(lowerName, newAttr);
      }
      else
      {
        attributes.remove(lowerName);
      }

      return (attr.getRawValues().length != newAttr.getRawValues().length);
    }
  }



  /**
   * Removes the specified attribute values from this entry if they are present.
   * If the attribute does not have any remaining values, then the entire
   * attribute will be removed.  If any of the provided values are not present,
   * then they will be ignored.
   *
   * @param  attributeName    The name of the attribute from which to remove the
   *                          values.  It must not be {@code null}.
   * @param  attributeValues  The set of values to remove from the attribute.
   *                          It must not be {@code null}.
   *
   * @return  {@code true} if any attribute values were removed from the entry,
   *          or {@code false} none of them were present.
   */
  public boolean removeAttributeValues(final String attributeName,
                                       final byte[]... attributeValues)
  {
    ensureNotNull(attributeName, attributeValues);

    final Attribute attr = getAttribute(attributeName, schema);
    if (attr == null)
    {
      return false;
    }
    else
    {
      final String lowerName = toLowerCase(attr.getName());
      final Attribute newAttr = Attribute.removeValues(attr,
           new Attribute(attributeName, attributeValues));
      if (newAttr.hasValue())
      {
        attributes.put(lowerName, newAttr);
      }
      else
      {
        attributes.remove(lowerName);
      }

      return (attr.getRawValues().length != newAttr.getRawValues().length);
    }
  }



  /**
   * Adds the provided attribute to this entry, replacing any existing set of
   * values for the associated attribute.
   *
   * @param  attribute  The attribute to be included in this entry.  It must not
   *                    be {@code null}.
   */
  public void setAttribute(final Attribute attribute)
  {
    ensureNotNull(attribute);

    final String lowerName;
    final Attribute a = getAttribute(attribute.getName(), schema);
    if (a == null)
    {
      lowerName = toLowerCase(attribute.getName());
    }
    else
    {
      lowerName = toLowerCase(a.getName());
    }

    attributes.put(lowerName, attribute);
  }



  /**
   * Adds the provided attribute to this entry, replacing any existing set of
   * values for the associated attribute.
   *
   * @param  attributeName   The name to use for the attribute.  It must not be
   *                         {@code null}.
   * @param  attributeValue  The value to use for the attribute.  It must not be
   *                         {@code null}.
   */
  public void setAttribute(final String attributeName,
                           final String attributeValue)
  {
    ensureNotNull(attributeName, attributeValue);
    setAttribute(new Attribute(attributeName, schema, attributeValue));
  }



  /**
   * Adds the provided attribute to this entry, replacing any existing set of
   * values for the associated attribute.
   *
   * @param  attributeName   The name to use for the attribute.  It must not be
   *                         {@code null}.
   * @param  attributeValue  The value to use for the attribute.  It must not be
   *                         {@code null}.
   */
  public void setAttribute(final String attributeName,
                           final byte[] attributeValue)
  {
    ensureNotNull(attributeName, attributeValue);
    setAttribute(new Attribute(attributeName, schema, attributeValue));
  }



  /**
   * Adds the provided attribute to this entry, replacing any existing set of
   * values for the associated attribute.
   *
   * @param  attributeName    The name to use for the attribute.  It must not be
   *                          {@code null}.
   * @param  attributeValues  The set of values to use for the attribute.  It
   *                          must not be {@code null}.
   */
  public void setAttribute(final String attributeName,
                           final String... attributeValues)
  {
    ensureNotNull(attributeName, attributeValues);
    setAttribute(new Attribute(attributeName, schema, attributeValues));
  }



  /**
   * Adds the provided attribute to this entry, replacing any existing set of
   * values for the associated attribute.
   *
   * @param  attributeName    The name to use for the attribute.  It must not be
   *                          {@code null}.
   * @param  attributeValues  The set of values to use for the attribute.  It
   *                          must not be {@code null}.
   */
  public void setAttribute(final String attributeName,
                           final byte[]... attributeValues)
  {
    ensureNotNull(attributeName, attributeValues);
    setAttribute(new Attribute(attributeName, schema, attributeValues));
  }



  /**
   * Indicates whether this entry falls within the range of the provided search
   * base DN and scope.
   *
   * @param  baseDN  The base DN for which to make the determination.  It must
   *                 not be {@code null}.
   * @param  scope   The scope for which to make the determination.  It must not
   *                 be {@code null}.
   *
   * @return  {@code true} if this entry is within the range of the provided
   *          base and scope, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem occurs while making the determination.
   */
  public boolean matchesBaseAndScope(final String baseDN,
                                     final SearchScope scope)
         throws LDAPException
  {
    return getParsedDN().matchesBaseAndScope(new DN(baseDN), scope);
  }



  /**
   * Indicates whether this entry falls within the range of the provided search
   * base DN and scope.
   *
   * @param  baseDN  The base DN for which to make the determination.  It must
   *                 not be {@code null}.
   * @param  scope   The scope for which to make the determination.  It must not
   *                 be {@code null}.
   *
   * @return  {@code true} if this entry is within the range of the provided
   *          base and scope, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem occurs while making the determination.
   */
  public boolean matchesBaseAndScope(final DN baseDN, final SearchScope scope)
         throws LDAPException
  {
    return getParsedDN().matchesBaseAndScope(baseDN, scope);
  }



  /**
   * Retrieves a set of modifications that can be applied to the source entry in
   * order to make it match the target entry.  The diff will be generated in
   * reversible form (i.e., the same as calling
   * {@code diff(sourceEntry, targetEntry, ignoreRDN, true, attributes)}.
   *
   * @param  sourceEntry  The source entry for which the set of modifications
   *                      should be generated.
   * @param  targetEntry  The target entry, which is what the source entry
   *                      should look like if the returned modifications are
   *                      applied.
   * @param  ignoreRDN    Indicates whether to ignore differences in the RDNs
   *                      of the provided entries.  If this is {@code false},
   *                      then the resulting set of modifications may include
   *                      changes to the RDN attribute.  If it is {@code true},
   *                      then differences in the entry DNs will be ignored.
   * @param  attributes   The set of attributes to be compared.  If this is
   *                      {@code null} or empty, then all attributes will be
   *                      compared.
   *
   * @return  A set of modifications that can be applied to the source entry in
   *          order to make it match the target entry.
   */
  public static List<Modification> diff(final Entry sourceEntry,
                                        final Entry targetEntry,
                                        final boolean ignoreRDN,
                                        final String... attributes)
  {
    return diff(sourceEntry, targetEntry, ignoreRDN, true, attributes);
  }



  /**
   * Retrieves a set of modifications that can be applied to the source entry in
   * order to make it match the target entry.
   *
   * @param  sourceEntry  The source entry for which the set of modifications
   *                      should be generated.
   * @param  targetEntry  The target entry, which is what the source entry
   *                      should look like if the returned modifications are
   *                      applied.
   * @param  ignoreRDN    Indicates whether to ignore differences in the RDNs
   *                      of the provided entries.  If this is {@code false},
   *                      then the resulting set of modifications may include
   *                      changes to the RDN attribute.  If it is {@code true},
   *                      then differences in the entry DNs will be ignored.
   * @param  reversible   Indicates whether to generate the diff in reversible
   *                      form.  In reversible form, only the ADD or DELETE
   *                      modification types will be used so that source entry
   *                      could be reconstructed from the target and the
   *                      resulting modifications.  In non-reversible form, only
   *                      the REPLACE modification type will be used.  Attempts
   *                      to apply the modifications obtained when using
   *                      reversible form are more likely to fail if the entry
   *                      has been modified since the source and target forms
   *                      were obtained.
   * @param  attributes   The set of attributes to be compared.  If this is
   *                      {@code null} or empty, then all attributes will be
   *                      compared.
   *
   * @return  A set of modifications that can be applied to the source entry in
   *          order to make it match the target entry.
   */
  public static List<Modification> diff(final Entry sourceEntry,
                                        final Entry targetEntry,
                                        final boolean ignoreRDN,
                                        final boolean reversible,
                                        final String... attributes)
  {
    HashSet<String> compareAttrs = null;
    if ((attributes != null) && (attributes.length > 0))
    {
      compareAttrs = new HashSet<String>(attributes.length);
      for (final String s : attributes)
      {
        compareAttrs.add(toLowerCase(s));
      }
    }

    final LinkedHashMap<String,Attribute> sourceOnlyAttrs =
         new LinkedHashMap<String,Attribute>();
    final LinkedHashMap<String,Attribute> targetOnlyAttrs =
         new LinkedHashMap<String,Attribute>();
    final LinkedHashMap<String,Attribute> commonAttrs =
         new LinkedHashMap<String,Attribute>();

    for (final Map.Entry<String,Attribute> e :
         sourceEntry.attributes.entrySet())
    {
      final String lowerName = toLowerCase(e.getKey());
      if ((compareAttrs != null) && (! compareAttrs.contains(lowerName)))
      {
        continue;
      }

      sourceOnlyAttrs.put(lowerName, e.getValue());
      commonAttrs.put(lowerName, e.getValue());
    }

    for (final Map.Entry<String,Attribute> e :
         targetEntry.attributes.entrySet())
    {
      final String lowerName = toLowerCase(e.getKey());
      if ((compareAttrs != null) && (! compareAttrs.contains(lowerName)))
      {
        continue;
      }


      if (sourceOnlyAttrs.remove(lowerName) == null)
      {
        // It wasn't in the set of source attributes, so it must be a
        // target-only attribute.
        targetOnlyAttrs.put(lowerName,e.getValue());
      }
    }

    for (final String lowerName : sourceOnlyAttrs.keySet())
    {
      commonAttrs.remove(lowerName);
    }

    RDN sourceRDN = null;
    RDN targetRDN = null;
    if (ignoreRDN)
    {
      try
      {
        sourceRDN = sourceEntry.getRDN();
      }
      catch (Exception e)
      {
        debugException(e);
      }

      try
      {
        targetRDN = targetEntry.getRDN();
      }
      catch (Exception e)
      {
        debugException(e);
      }
    }

    final ArrayList<Modification> mods = new ArrayList<Modification>(10);

    for (final Attribute a : sourceOnlyAttrs.values())
    {
      if (reversible)
      {
        ASN1OctetString[] values = a.getRawValues();
        if ((sourceRDN != null) && (sourceRDN.hasAttribute(a.getName())))
        {
          final ArrayList<ASN1OctetString> newValues =
               new ArrayList<ASN1OctetString>(values.length);
          for (final ASN1OctetString value : values)
          {
            if (! sourceRDN.hasAttributeValue(a.getName(), value.getValue()))
            {
              newValues.add(value);
            }
          }

          if (newValues.isEmpty())
          {
            continue;
          }
          else
          {
            values = new ASN1OctetString[newValues.size()];
            newValues.toArray(values);
          }
        }

        mods.add(new Modification(ModificationType.DELETE, a.getName(),
             values));
      }
      else
      {
        mods.add(new Modification(ModificationType.REPLACE, a.getName()));
      }
    }

    for (final Attribute a : targetOnlyAttrs.values())
    {
      ASN1OctetString[] values = a.getRawValues();
      if ((targetRDN != null) && (targetRDN.hasAttribute(a.getName())))
      {
        final ArrayList<ASN1OctetString> newValues =
             new ArrayList<ASN1OctetString>(values.length);
        for (final ASN1OctetString value : values)
        {
          if (! targetRDN.hasAttributeValue(a.getName(), value.getValue()))
          {
            newValues.add(value);
          }
        }

        if (newValues.isEmpty())
        {
          continue;
        }
        else
        {
          values = new ASN1OctetString[newValues.size()];
          newValues.toArray(values);
        }
      }

      if (reversible)
      {
        mods.add(new Modification(ModificationType.ADD, a.getName(), values));
      }
      else
      {
        mods.add(new Modification(ModificationType.REPLACE, a.getName(),
             values));
      }
    }

    for (final Attribute sourceAttr : commonAttrs.values())
    {
      final Attribute targetAttr =
           targetEntry.getAttribute(sourceAttr.getName());
      if (sourceAttr.equals(targetAttr))
      {
        continue;
      }

      if (reversible ||
          ((targetRDN != null) && targetRDN.hasAttribute(targetAttr.getName())))
      {
        final ASN1OctetString[] sourceValueArray = sourceAttr.getRawValues();
        final LinkedHashMap<ASN1OctetString,ASN1OctetString> sourceValues =
             new LinkedHashMap<ASN1OctetString,ASN1OctetString>(
                  sourceValueArray.length);
        for (final ASN1OctetString s : sourceValueArray)
        {
          try
          {
            sourceValues.put(sourceAttr.getMatchingRule().normalize(s), s);
          }
          catch (final Exception e)
          {
            debugException(e);
            sourceValues.put(s, s);
          }
        }

        final ASN1OctetString[] targetValueArray = targetAttr.getRawValues();
        final LinkedHashMap<ASN1OctetString,ASN1OctetString> targetValues =
             new LinkedHashMap<ASN1OctetString,ASN1OctetString>(
                  targetValueArray.length);
        for (final ASN1OctetString s : targetValueArray)
        {
          try
          {
            targetValues.put(sourceAttr.getMatchingRule().normalize(s), s);
          }
          catch (final Exception e)
          {
            debugException(e);
            targetValues.put(s, s);
          }
        }

        final Iterator<Map.Entry<ASN1OctetString,ASN1OctetString>>
             sourceIterator = sourceValues.entrySet().iterator();
        while (sourceIterator.hasNext())
        {
          final Map.Entry<ASN1OctetString,ASN1OctetString> e =
               sourceIterator.next();
          if (targetValues.remove(e.getKey()) != null)
          {
            sourceIterator.remove();
          }
          else if ((sourceRDN != null) &&
                   sourceRDN.hasAttributeValue(sourceAttr.getName(),
                        e.getValue().getValue()))
          {
            sourceIterator.remove();
          }
        }

        final Iterator<Map.Entry<ASN1OctetString,ASN1OctetString>>
             targetIterator = targetValues.entrySet().iterator();
        while (targetIterator.hasNext())
        {
          final Map.Entry<ASN1OctetString,ASN1OctetString> e =
               targetIterator.next();
          if ((targetRDN != null) &&
              targetRDN.hasAttributeValue(targetAttr.getName(),
                   e.getValue().getValue()))
          {
            targetIterator.remove();
          }
        }

        final ArrayList<ASN1OctetString> addValues =
             new ArrayList<ASN1OctetString>(targetValues.values());
        final ArrayList<ASN1OctetString> delValues =
             new ArrayList<ASN1OctetString>(sourceValues.values());

        if (! addValues.isEmpty())
        {
          final ASN1OctetString[] addArray =
               new ASN1OctetString[addValues.size()];
          mods.add(new Modification(ModificationType.ADD, targetAttr.getName(),
               addValues.toArray(addArray)));
        }

        if (! delValues.isEmpty())
        {
          final ASN1OctetString[] delArray =
               new ASN1OctetString[delValues.size()];
          mods.add(new Modification(ModificationType.DELETE,
               sourceAttr.getName(), delValues.toArray(delArray)));
        }
      }
      else
      {
        mods.add(new Modification(ModificationType.REPLACE,
             targetAttr.getName(), targetAttr.getRawValues()));
      }
    }

    return mods;
  }



  /**
   * Merges the contents of all provided entries so that the resulting entry
   * will contain all attribute values present in at least one of the entries.
   *
   * @param  entries  The set of entries to be merged.  At least one entry must
   *                  be provided.
   *
   * @return  An entry containing all attribute values present in at least one
   *          of the entries.
   */
  public static Entry mergeEntries(final Entry... entries)
  {
    ensureNotNull(entries);
    ensureTrue(entries.length > 0);

    final Entry newEntry = entries[0].duplicate();

    for (int i=1; i < entries.length; i++)
    {
      for (final Attribute a : entries[i].attributes.values())
      {
        newEntry.addAttribute(a);
      }
    }

    return newEntry;
  }



  /**
   * Intersects the contents of all provided entries so that the resulting
   * entry will contain only attribute values present in all of the provided
   * entries.
   *
   * @param  entries  The set of entries to be intersected.  At least one entry
   *                  must be provided.
   *
   * @return  An entry containing only attribute values contained in all of the
   *          provided entries.
   */
  public static Entry intersectEntries(final Entry... entries)
  {
    ensureNotNull(entries);
    ensureTrue(entries.length > 0);

    final Entry newEntry = entries[0].duplicate();

    for (final Attribute a : entries[0].attributes.values())
    {
      final String name = a.getName();
      for (final byte[] v : a.getValueByteArrays())
      {
        for (int i=1; i < entries.length; i++)
        {
          if (! entries[i].hasAttributeValue(name, v))
          {
            newEntry.removeAttributeValue(name, v);
            break;
          }
        }
      }
    }

    return newEntry;
  }



  /**
   * Creates a duplicate of the provided entry with the given set of
   * modifications applied to it.
   *
   * @param  entry          The entry to be modified.  It must not be
   *                        {@code null}.
   * @param  lenient        Indicates whether to exhibit a lenient behavior for
   *                        the modifications, which will cause it to ignore
   *                        problems like trying to add values that already
   *                        exist or to remove nonexistent attributes or values.
   * @param  modifications  The set of modifications to apply to the entry.  It
   *                        must not be {@code null} or empty.
   *
   * @return  An updated version of the entry with the requested modifications
   *          applied.
   *
   * @throws  LDAPException  If a problem occurs while attempting to apply the
   *                         modifications.
   */
  public static Entry applyModifications(final Entry entry,
                                         final boolean lenient,
                                         final Modification... modifications)
         throws LDAPException
  {
    ensureNotNull(entry, modifications);
    ensureFalse(modifications.length == 0);

    return applyModifications(entry, lenient, Arrays.asList(modifications));
  }



  /**
   * Creates a duplicate of the provided entry with the given set of
   * modifications applied to it.
   *
   * @param  entry          The entry to be modified.  It must not be
   *                        {@code null}.
   * @param  lenient        Indicates whether to exhibit a lenient behavior for
   *                        the modifications, which will cause it to ignore
   *                        problems like trying to add values that already
   *                        exist or to remove nonexistent attributes or values.
   * @param  modifications  The set of modifications to apply to the entry.  It
   *                        must not be {@code null} or empty.
   *
   * @return  An updated version of the entry with the requested modifications
   *          applied.
   *
   * @throws  LDAPException  If a problem occurs while attempting to apply the
   *                         modifications.
   */
  public static Entry applyModifications(final Entry entry,
                                         final boolean lenient,
                                         final List<Modification> modifications)
         throws LDAPException
  {
    ensureNotNull(entry, modifications);
    ensureFalse(modifications.isEmpty());

    final Entry e = entry.duplicate();
    final ArrayList<String> errors =
         new ArrayList<String>(modifications.size());
    ResultCode resultCode = null;

    // Get the RDN for the entry to ensure that RDN modifications are not
    // allowed.
    RDN rdn = null;
    try
    {
      rdn = entry.getRDN();
    }
    catch (final LDAPException le)
    {
      debugException(le);
    }

    for (final Modification m : modifications)
    {
      final String   name   = m.getAttributeName();
      final byte[][] values = m.getValueByteArrays();
      switch (m.getModificationType().intValue())
      {
        case ModificationType.ADD_INT_VALUE:
          if (lenient)
          {
            e.addAttribute(m.getAttribute());
          }
          else
          {
            if (values.length == 0)
            {
              errors.add(ERR_ENTRY_APPLY_MODS_ADD_NO_VALUES.get(name));
            }

            for (int i=0; i < values.length; i++)
            {
              if (! e.addAttribute(name, values[i]))
              {
                if (resultCode == null)
                {
                  resultCode = ResultCode.ATTRIBUTE_OR_VALUE_EXISTS;
                }
                errors.add(ERR_ENTRY_APPLY_MODS_ADD_EXISTING.get(
                     m.getValues()[i], name));
              }
            }
          }
          break;

        case ModificationType.DELETE_INT_VALUE:
          if (values.length == 0)
          {
            if ((rdn != null) && rdn.hasAttribute(name))
            {
              final String msg =
                   ERR_ENTRY_APPLY_MODS_TARGETS_RDN.get(entry.getDN());
              if (! errors.contains(msg))
              {
                errors.add(msg);
              }

              if (resultCode == null)
              {
                resultCode = ResultCode.NOT_ALLOWED_ON_RDN;
              }
              break;
            }

            final boolean removed = e.removeAttribute(name);
            if (! (lenient || removed))
            {
              if (resultCode == null)
              {
                resultCode = ResultCode.NO_SUCH_ATTRIBUTE;
              }
              errors.add(ERR_ENTRY_APPLY_MODS_DELETE_NONEXISTENT_ATTR.get(
                   name));
            }
          }
          else
          {
deleteValueLoop:
            for (int i=0; i < values.length; i++)
            {
              if ((rdn != null) && rdn.hasAttributeValue(name, values[i]))
              {
                final String msg =
                     ERR_ENTRY_APPLY_MODS_TARGETS_RDN.get(entry.getDN());
                if (! errors.contains(msg))
                {
                  errors.add(msg);
                }

                if (resultCode == null)
                {
                  resultCode = ResultCode.NOT_ALLOWED_ON_RDN;
                }
                break deleteValueLoop;
              }

              final boolean removed = e.removeAttributeValue(name, values[i]);
              if (! (lenient || removed))
              {
                if (resultCode == null)
                {
                  resultCode = ResultCode.NO_SUCH_ATTRIBUTE;
                }
                errors.add(ERR_ENTRY_APPLY_MODS_DELETE_NONEXISTENT_VALUE.get(
                     m.getValues()[i], name));
              }
            }
          }
          break;

        case ModificationType.REPLACE_INT_VALUE:
          if ((rdn != null) && rdn.hasAttribute(name))
          {
            final String msg =
                 ERR_ENTRY_APPLY_MODS_TARGETS_RDN.get(entry.getDN());
            if (! errors.contains(msg))
            {
              errors.add(msg);
            }

            if (resultCode == null)
            {
              resultCode = ResultCode.NOT_ALLOWED_ON_RDN;
            }
            continue;
          }

          if (values.length == 0)
          {
            e.removeAttribute(name);
          }
          else
          {
            e.setAttribute(m.getAttribute());
          }
          break;

        case ModificationType.INCREMENT_INT_VALUE:
          final Attribute a = e.getAttribute(name);
          if ((a == null) || (! a.hasValue()))
          {
            errors.add(ERR_ENTRY_APPLY_MODS_INCREMENT_NO_SUCH_ATTR.get(name));
            continue;
          }

          if (a.size() > 1)
          {
            errors.add(ERR_ENTRY_APPLY_MODS_INCREMENT_NOT_SINGLE_VALUED.get(
                 name));
            continue;
          }

          if ((rdn != null) && rdn.hasAttribute(name))
          {
            final String msg =
                 ERR_ENTRY_APPLY_MODS_TARGETS_RDN.get(entry.getDN());
            if (! errors.contains(msg))
            {
              errors.add(msg);
            }

            if (resultCode == null)
            {
              resultCode = ResultCode.NOT_ALLOWED_ON_RDN;
            }
            continue;
          }

          final BigInteger currentValue;
          try
          {
            currentValue = new BigInteger(a.getValue());
          }
          catch (NumberFormatException nfe)
          {
            debugException(nfe);
            errors.add(
                 ERR_ENTRY_APPLY_MODS_INCREMENT_ENTRY_VALUE_NOT_INTEGER.get(
                      name, a.getValue()));
            continue;
          }

          if (values.length == 0)
          {
            errors.add(ERR_ENTRY_APPLY_MODS_INCREMENT_NO_MOD_VALUES.get(name));
            continue;
          }
          else if (values.length > 1)
          {
            errors.add(ERR_ENTRY_APPLY_MODS_INCREMENT_MULTIPLE_MOD_VALUES.get(
                 name));
            continue;
          }

          final BigInteger incrementValue;
          final String incrementValueStr = m.getValues()[0];
          try
          {
            incrementValue = new BigInteger(incrementValueStr);
          }
          catch (NumberFormatException nfe)
          {
            debugException(nfe);
            errors.add(ERR_ENTRY_APPLY_MODS_INCREMENT_MOD_VALUE_NOT_INTEGER.get(
                 name, incrementValueStr));
            continue;
          }

          final BigInteger newValue = currentValue.add(incrementValue);
          e.setAttribute(name, newValue.toString());
          break;

        default:
          errors.add(ERR_ENTRY_APPLY_MODS_UNKNOWN_TYPE.get(
               String.valueOf(m.getModificationType())));
          break;
      }
    }

    if (errors.isEmpty())
    {
      return e;
    }

    if (resultCode == null)
    {
      resultCode = ResultCode.CONSTRAINT_VIOLATION;
    }

    throw new LDAPException(resultCode,
         ERR_ENTRY_APPLY_MODS_FAILURE.get(e.getDN(),
              concatenateStrings(errors)));
  }



  /**
   * Generates a hash code for this entry.
   *
   * @return  The generated hash code for this entry.
   */
  @Override()
  public int hashCode()
  {
    int hashCode = 0;
    try
    {
      hashCode += getParsedDN().hashCode();
    }
    catch (LDAPException le)
    {
      debugException(le);
      hashCode += dn.hashCode();
    }

    for (final Attribute a : attributes.values())
    {
      hashCode += a.hashCode();
    }

    return hashCode;
  }



  /**
   * Indicates whether the provided object is equal to this entry.  The provided
   * object will only be considered equal to this entry if it is an entry with
   * the same DN and set of attributes.
   *
   * @param  o  The object for which to make the determination.
   *
   * @return  {@code true} if the provided object is considered equal to this
   *          entry, or {@code false} if not.
   */
  @Override()
  public boolean equals(final Object o)
  {
    if (o == null)
    {
      return false;
    }

    if (o == this)
    {
      return true;
    }

    if (! (o instanceof Entry))
    {
      return false;
    }

    final Entry e = (Entry) o;

    try
    {
      final DN thisDN = getParsedDN();
      final DN thatDN = e.getParsedDN();
      if (! thisDN.equals(thatDN))
      {
        return false;
      }
    }
    catch (LDAPException le)
    {
      debugException(le);
      if (! dn.equals(e.dn))
      {
        return false;
      }
    }

    if (attributes.size() != e.attributes.size())
    {
      return false;
    }

    for (final Attribute a : attributes.values())
    {
      if (! e.hasAttribute(a))
      {
        return false;
      }
    }

    return true;
  }



  /**
   * Creates a new entry that is a duplicate of this entry.
   *
   * @return  A new entry that is a duplicate of this entry.
   */
  public Entry duplicate()
  {
    return new Entry(dn, schema, attributes.values());
  }



  /**
   * Retrieves an LDIF representation of this entry, with each attribute value
   * on a separate line.  Long lines will not be wrapped.
   *
   * @return  An LDIF representation of this entry.
   */
  public final String[] toLDIF()
  {
    return toLDIF(0);
  }



  /**
   * Retrieves an LDIF representation of this entry, with each attribute value
   * on a separate line.  Long lines will be wrapped at the specified column.
   *
   * @param  wrapColumn  The column at which long lines should be wrapped.  A
   *                     value less than or equal to two indicates that no
   *                     wrapping should be performed.
   *
   * @return  An LDIF representation of this entry.
   */
  public final String[] toLDIF(final int wrapColumn)
  {
    List<String> ldifLines = new ArrayList<String>(2*attributes.size());
    ldifLines.add(LDIFWriter.encodeNameAndValue("dn", new ASN1OctetString(dn)));

    for (final Attribute a : attributes.values())
    {
      final String name = a.getName();
      for (final ASN1OctetString value : a.getRawValues())
      {
        ldifLines.add(LDIFWriter.encodeNameAndValue(name, value));
      }
    }

    if (wrapColumn > 2)
    {
      ldifLines = LDIFWriter.wrapLines(wrapColumn, ldifLines);
    }

    final String[] lineArray = new String[ldifLines.size()];
    ldifLines.toArray(lineArray);
    return lineArray;
  }



  /**
   * Appends an LDIF representation of this entry to the provided buffer.  Long
   * lines will not be wrapped.
   *
   * @param  buffer The buffer to which the LDIF representation of this entry
   *                should be written.
   */
  public final void toLDIF(final ByteStringBuffer buffer)
  {
    toLDIF(buffer, 0);
  }



  /**
   * Appends an LDIF representation of this entry to the provided buffer.
   *
   * @param  buffer      The buffer to which the LDIF representation of this
   *                     entry should be written.
   * @param  wrapColumn  The column at which long lines should be wrapped.  A
   *                     value less than or equal to two indicates that no
   *                     wrapping should be performed.
   */
  public final void toLDIF(final ByteStringBuffer buffer, final int wrapColumn)
  {
    LDIFWriter.encodeNameAndValue("dn", new ASN1OctetString(dn), buffer,
                       wrapColumn);
    buffer.append(EOL_BYTES);

    for (final Attribute a : attributes.values())
    {
      final String name = a.getName();
      for (final ASN1OctetString value : a.getRawValues())
      {
        LDIFWriter.encodeNameAndValue(name, value, buffer, wrapColumn);
        buffer.append(EOL_BYTES);
      }
    }
  }



  /**
   * Retrieves an LDIF-formatted string representation of this entry.  No
   * wrapping will be performed, and no extra blank lines will be added.
   *
   * @return  An LDIF-formatted string representation of this entry.
   */
  public final String toLDIFString()
  {
    final StringBuilder buffer = new StringBuilder();
    toLDIFString(buffer, 0);
    return buffer.toString();
  }



  /**
   * Retrieves an LDIF-formatted string representation of this entry.  No
   * extra blank lines will be added.
   *
   * @param  wrapColumn  The column at which long lines should be wrapped.  A
   *                     value less than or equal to two indicates that no
   *                     wrapping should be performed.
   *
   * @return  An LDIF-formatted string representation of this entry.
   */
  public final String toLDIFString(final int wrapColumn)
  {
    final StringBuilder buffer = new StringBuilder();
    toLDIFString(buffer, wrapColumn);
    return buffer.toString();
  }



  /**
   * Appends an LDIF-formatted string representation of this entry to the
   * provided buffer.  No wrapping will be performed, and no extra blank lines
   * will be added.
   *
   * @param  buffer  The buffer to which to append the LDIF representation of
   *                 this entry.
   */
  public final void toLDIFString(final StringBuilder buffer)
  {
    toLDIFString(buffer, 0);
  }



  /**
   * Appends an LDIF-formatted string representation of this entry to the
   * provided buffer.  No extra blank lines will be added.
   *
   * @param  buffer      The buffer to which to append the LDIF representation
   *                     of this entry.
   * @param  wrapColumn  The column at which long lines should be wrapped.  A
   *                     value less than or equal to two indicates that no
   *                     wrapping should be performed.
   */
  public final void toLDIFString(final StringBuilder buffer,
                                 final int wrapColumn)
  {
    LDIFWriter.encodeNameAndValue("dn", new ASN1OctetString(dn), buffer,
                                  wrapColumn);
    buffer.append(EOL);

    for (final Attribute a : attributes.values())
    {
      final String name = a.getName();
      for (final ASN1OctetString value : a.getRawValues())
      {
        LDIFWriter.encodeNameAndValue(name, value, buffer, wrapColumn);
        buffer.append(EOL);
      }
    }
  }



  /**
   * Retrieves a string representation of this entry.
   *
   * @return  A string representation of this entry.
   */
  @Override()
  public final String toString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this entry to the provided buffer.
   *
   * @param  buffer  The buffer to which to append the string representation of
   *                 this entry.
   */
  public void toString(final StringBuilder buffer)
  {
    buffer.append("Entry(dn='");
    buffer.append(dn);
    buffer.append("', attributes={");

    final Iterator<Attribute> iterator = attributes.values().iterator();

    while (iterator.hasNext())
    {
      iterator.next().toString(buffer);
      if (iterator.hasNext())
      {
        buffer.append(", ");
      }
    }

    buffer.append("})");
  }
}