/*
 * Copyright 2015 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2015 UnboundID Corp.
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
package com.unboundid.util.args;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.util.Base64;
import com.unboundid.util.Debug;
import com.unboundid.util.Mutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.util.args.ArgsMessages.*;



/**
 * This class defines an argument that is intended to hold information about one
 * or more LDAP controls.  Values for this argument must be in one of the
 * following formats:
 * <UL>
 *   <LI>
 *     oid -- The numeric OID for the control.  The control will not be critical
 *     and will not have a value.
 *   </LI>
 *   <LI>
 *     oid:criticality -- The numeric OID followed by a colon and the
 *     criticality.  The control will be critical if the criticality value is
 *     any of the following:  {@code true}, {@code t}, {@code yes}, {@code y},
 *     {@code on}, or {@code 1}.  The control will be non-critical if the
 *     criticality value is any of the following:  {@code false}, {@code f},
 *     {@code no}, {@code n}, {@code off}, or {@code 0}.  No other criticality
 *     values will be accepted.
 *   </LI>
 *   <LI>
 *     oid:criticality:value -- The numeric OID followed by a colon and the
 *     criticality, then a colon and then a string that represents the value for
 *     the control.
 *   </LI>
 *   <LI>
 *     oid:criticality::base64value -- The numeric OID  followed by a colon and
 *     the criticality, then two colons and then a string that represents the
 *     base64-encoded value for the control.
 *   </LI>
 * </UL>
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class ControlArgument
       extends Argument
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -1889200072476038957L;



  // The argument value validators that have been registered for this argument.
  private final List<ArgumentValueValidator> validators;

  // The list of default values for this argument.
  private final List<Control> defaultValues;

  // The set of values assigned to this argument.
  private final List<Control> values;



  /**
   * Creates a new control argument with the provided information.  It will not
   * have a default value.
   *
   * @param  shortIdentifier   The short identifier for this argument.  It may
   *                           not be {@code null} if the long identifier is
   *                           {@code null}.
   * @param  longIdentifier    The long identifier for this argument.  It may
   *                           not be {@code null} if the short identifier is
   *                           {@code null}.
   * @param  isRequired        Indicates whether this argument is required to
   *                           be provided.
   * @param  maxOccurrences    The maximum number of times this argument may be
   *                           provided on the command line.  A value less than
   *                           or equal to zero indicates that it may be present
   *                           any number of times.
   * @param  valuePlaceholder  A placeholder to display in usage information to
   *                           indicate that a value must be provided.  It may
   *                           be {@code null} to use a default placeholder that
   *                           describes the expected syntax for values.
   * @param  description       A human-readable description for this argument.
   *                           It must not be {@code null}.
   *
   * @throws  ArgumentException  If there is a problem with the definition of
   *                             this argument.
   */
  public ControlArgument(final Character shortIdentifier,
                         final String longIdentifier, final boolean isRequired,
                         final int maxOccurrences,
                         final String valuePlaceholder,
                         final String description)
         throws ArgumentException
  {
    this(shortIdentifier, longIdentifier, isRequired,  maxOccurrences,
         valuePlaceholder, description, (List<Control>) null);
  }



  /**
   * Creates a new control argument with the provided information.
   *
   * @param  shortIdentifier   The short identifier for this argument.  It may
   *                           not be {@code null} if the long identifier is
   *                           {@code null}.
   * @param  longIdentifier    The long identifier for this argument.  It may
   *                           not be {@code null} if the short identifier is
   *                           {@code null}.
   * @param  isRequired        Indicates whether this argument is required to
   *                           be provided.
   * @param  maxOccurrences    The maximum number of times this argument may be
   *                           provided on the command line.  A value less than
   *                           or equal to zero indicates that it may be present
   *                           any number of times.
   * @param  valuePlaceholder  A placeholder to display in usage information to
   *                           indicate that a value must be provided.  It may
   *                           be {@code null} to use a default placeholder that
   *                           describes the expected syntax for values.
   * @param  description       A human-readable description for this argument.
   *                           It must not be {@code null}.
   * @param  defaultValue      The default value to use for this argument if no
   *                           values were provided.  It may be {@code null} if
   *                           there should be no default values.
   *
   * @throws  ArgumentException  If there is a problem with the definition of
   *                             this argument.
   */
  public ControlArgument(final Character shortIdentifier,
                         final String longIdentifier, final boolean isRequired,
                         final int maxOccurrences,
                         final String valuePlaceholder,
                         final String description, final Control defaultValue)
         throws ArgumentException
  {
    this(shortIdentifier, longIdentifier, isRequired, maxOccurrences,
         valuePlaceholder, description,
         ((defaultValue == null)
              ? null :
              Collections.singletonList(defaultValue)));
  }



  /**
   * Creates a new control argument with the provided information.
   *
   * @param  shortIdentifier   The short identifier for this argument.  It may
   *                           not be {@code null} if the long identifier is
   *                           {@code null}.
   * @param  longIdentifier    The long identifier for this argument.  It may
   *                           not be {@code null} if the short identifier is
   *                           {@code null}.
   * @param  isRequired        Indicates whether this argument is required to
   *                           be provided.
   * @param  maxOccurrences    The maximum number of times this argument may be
   *                           provided on the command line.  A value less than
   *                           or equal to zero indicates that it may be present
   *                           any number of times.
   * @param  valuePlaceholder  A placeholder to display in usage information to
   *                           indicate that a value must be provided.  It may
   *                           be {@code null} to use a default placeholder that
   *                           describes the expected syntax for values.
   * @param  description       A human-readable description for this argument.
   *                           It must not be {@code null}.
   * @param  defaultValues     The set of default values to use for this
   *                           argument if no values were provided.
   *
   * @throws  ArgumentException  If there is a problem with the definition of
   *                             this argument.
   */
  public ControlArgument(final Character shortIdentifier,
                         final String longIdentifier, final boolean isRequired,
                         final int maxOccurrences,
                         final String valuePlaceholder,
                         final String description,
                         final List<Control> defaultValues)
         throws ArgumentException
  {
    super(shortIdentifier, longIdentifier, isRequired,  maxOccurrences,
         (valuePlaceholder == null)
              ? "{oid}[:{criticality}[:{stringValue}|::{base64Value}]]"
              : valuePlaceholder,
         description);

    if ((defaultValues == null) || defaultValues.isEmpty())
    {
      this.defaultValues = null;
    }
    else
    {
      this.defaultValues = Collections.unmodifiableList(defaultValues);
    }

    values = new ArrayList<Control>(5);
    validators = new ArrayList<ArgumentValueValidator>(5);
  }



  /**
   * Creates a new control argument that is a "clean" copy of the provided
   * source argument.
   *
   * @param  source  The source argument to use for this argument.
   */
  private ControlArgument(final ControlArgument source)
  {
    super(source);

    defaultValues = source.defaultValues;
    validators    = new ArrayList<ArgumentValueValidator>(source.validators);
    values        = new ArrayList<Control>(5);
  }



  /**
   * Retrieves the list of default values for this argument, which will be used
   * if no values were provided.
   *
   * @return   The list of default values for this argument, or {@code null} if
   *           there are no default values.
   */
  public List<Control> getDefaultValues()
  {
    return defaultValues;
  }



  /**
   * Updates this argument to ensure that the provided validator will be invoked
   * for any values provided to this argument.  This validator will be invoked
   * after all other validation has been performed for this argument.
   *
   * @param  validator  The argument value validator to be invoked.  It must not
   *                    be {@code null}.
   */
  public void addValueValidator(final ArgumentValueValidator validator)
  {
    validators.add(validator);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected void addValue(final String valueString)
            throws ArgumentException
  {
    final String oid;
    boolean isCritical = false;
    ASN1OctetString value = null;

    final int firstColonPos = valueString.indexOf(':');
    if (firstColonPos < 0)
    {
      oid = valueString;
    }
    else
    {
      oid = valueString.substring(0, firstColonPos);

      final String criticalityStr;
      final int secondColonPos = valueString.indexOf(':', (firstColonPos+1));
      if (secondColonPos < 0)
      {
        criticalityStr = valueString.substring(firstColonPos+1);
      }
      else
      {
        criticalityStr = valueString.substring(firstColonPos+1, secondColonPos);

        final int doubleColonPos = valueString.indexOf("::");
        if (doubleColonPos == secondColonPos)
        {
          try
          {
            value = new ASN1OctetString(
                 Base64.decode(valueString.substring(doubleColonPos+2)));
          }
          catch (final Exception e)
          {
            Debug.debugException(e);
            throw new ArgumentException(
                 ERR_CONTROL_ARG_INVALID_BASE64_VALUE.get(valueString,
                      getIdentifierString(),
                      valueString.substring(doubleColonPos+2)),
                 e);
          }
        }
        else
        {
          value = new ASN1OctetString(valueString.substring(secondColonPos+1));
        }
      }

      final String lowerCriticalityStr =
           StaticUtils.toLowerCase(criticalityStr);
      if (lowerCriticalityStr.equals("true") ||
          lowerCriticalityStr.equals("t") ||
          lowerCriticalityStr.equals("yes") ||
          lowerCriticalityStr.equals("y") ||
          lowerCriticalityStr.equals("on") ||
          lowerCriticalityStr.equals("1"))
      {
        isCritical = true;
      }
      else if (lowerCriticalityStr.equals("false") ||
               lowerCriticalityStr.equals("f") ||
               lowerCriticalityStr.equals("no") ||
               lowerCriticalityStr.equals("n") ||
               lowerCriticalityStr.equals("off") ||
               lowerCriticalityStr.equals("0"))
      {
        isCritical = false;
      }
      else
      {
        throw new ArgumentException(ERR_CONTROL_ARG_INVALID_CRITICALITY.get(
             valueString, getIdentifierString(), criticalityStr));
      }
    }

    if (! StaticUtils.isNumericOID(oid))
    {
      throw new ArgumentException(ERR_CONTROL_ARG_INVALID_OID.get(
           valueString, getIdentifierString(), oid));
    }

    if (values.size() >= getMaxOccurrences())
    {
      throw new ArgumentException(ERR_ARG_MAX_OCCURRENCES_EXCEEDED.get(
                                       getIdentifierString()));
    }

    for (final ArgumentValueValidator v : validators)
    {
      v.validateArgumentValue(this, valueString);
    }

    values.add(new Control(oid, isCritical, value));
  }



  /**
   * Retrieves the value for this argument, or the default value if none was
   * provided.  If there are multiple values, then the first will be returned.
   *
   * @return  The value for this argument, or the default value if none was
   *          provided, or {@code null} if there is no value and no default
   *          value.
   */
  public Control getValue()
  {
    if (values.isEmpty())
    {
      if ((defaultValues == null) || defaultValues.isEmpty())
      {
        return null;
      }
      else
      {
        return defaultValues.get(0);
      }
    }
    else
    {
      return values.get(0);
    }
  }



  /**
   * Retrieves the set of values for this argument, or the default values if
   * none were provided.
   *
   * @return  The set of values for this argument, or the default values if none
   *          were provided.
   */
  public List<Control> getValues()
  {
    if (values.isEmpty() && (defaultValues != null))
    {
      return defaultValues;
    }

    return Collections.unmodifiableList(values);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected boolean hasDefaultValue()
  {
    return ((defaultValues != null) && (! defaultValues.isEmpty()));
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getDataTypeName()
  {
    return INFO_CONTROL_TYPE_NAME.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getValueConstraints()
  {
    return INFO_CONTROL_CONSTRAINTS.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ControlArgument getCleanCopy()
  {
    return new ControlArgument(this);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("ControlArgument(");
    appendBasicToStringInfo(buffer);

    if ((defaultValues != null) && (! defaultValues.isEmpty()))
    {
      if (defaultValues.size() == 1)
      {
        buffer.append(", defaultValue='");
        buffer.append(defaultValues.get(0).toString());
      }
      else
      {
        buffer.append(", defaultValues={");

        final Iterator<Control> iterator = defaultValues.iterator();
        while (iterator.hasNext())
        {
          buffer.append('\'');
          buffer.append(iterator.next().toString());
          buffer.append('\'');

          if (iterator.hasNext())
          {
            buffer.append(", ");
          }
        }

        buffer.append('}');
      }
    }

    buffer.append(')');
  }
}
