/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2010 UnboundID Corp.
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
package com.unboundid.ldap.sdk.persist;



import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;

import static com.unboundid.ldap.sdk.persist.PersistMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class provides a data structure that holds information about an
 * annotated setter method.
 */
final class SetterInfo
      implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -1743750276508505946L;



  // Indicates whether attempts to invoke the associated method should fail if
  // the LDAP attribute has a value that is not valid for the data type of the
  // method argument.
  private final boolean failOnInvalidValue;

  // Indicates whether attempts to invoke the associated method should fail if
  // the LDAP attribute has multiple values but the method argument can only
  // hold a single value.
  private final boolean failOnTooManyValues;

  // Indicates whether the associated method takes an argument that supports
  // multiple values.
  private final boolean supportsMultipleValues;

  // The class that contains the associated method.
  private final Class<?> containingClass;

  // The method with which this object is associated.
  private final Method method;

  // The encoder used for this method.
  private final LDAPFieldEncoder encoder;

  // The name of the associated attribute type.
  private final String attributeName;



  /**
   * Creates a new setter info object from the provided method.
   *
   * @param  m  The method to use to create this object.
   * @param  c  The class which holds the method.
   *
   * @throws  LDAPPersistException  If a problem occurs while processing the
   *                                given method.
   */
  SetterInfo(final Method m, final Class<?> c)
       throws LDAPPersistException
  {
    ensureNotNull(m, c);

    method = m;
    m.setAccessible(true);

    final LDAPFieldSetter  a = m.getAnnotation(LDAPFieldSetter.class);
    if (a == null)
    {
      throw new LDAPPersistException(ERR_SETTER_INFO_METHOD_NOT_ANNOTATED.get(
           m.getName(), c.getName()));
    }

    final LDAPObject o = c.getAnnotation(LDAPObject.class);
    if (o == null)
    {
      throw new LDAPPersistException(ERR_SETTER_INFO_CLASS_NOT_ANNOTATED.get(
           c.getName()));
    }

    containingClass     = c;
    failOnInvalidValue  = a.failOnInvalidValue();
    attributeName       = a.attribute();

    final Type[] params = m.getGenericParameterTypes();
    if (params.length != 1)
    {
      throw new LDAPPersistException(
           ERR_SETTER_INFO_METHOD_DOES_NOT_TAKE_ONE_ARGUMENT.get(m.getName(),
                c.getName()));
    }

    try
    {
      encoder = a.encoderClass().newInstance();
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPPersistException(ERR_SETTER_INFO_CANNOT_GET_ENCODER.get(
           a.encoderClass().getName(), m.getName(), c.getName(),
           getExceptionMessage(e)), e);
    }

    if (! encoder.supportsType(params[0]))
    {
      throw new LDAPPersistException(
           ERR_SETTER_INFO_ENCODER_UNSUPPORTED_TYPE.get(
                encoder.getClass().getName(), m.getName(), c.getName(),
                String.valueOf(params[0])));
    }

    supportsMultipleValues = encoder.supportsMultipleValues(m);
    if (supportsMultipleValues)
    {
      failOnTooManyValues = false;
    }
    else
    {
      failOnTooManyValues = a.failOnTooManyValues();
    }
  }



  /**
   * Retrieves the method with which this object is associated.
   *
   * @return  The method with which this object is associated.
   */
  Method getMethod()
  {
    return method;
  }



  /**
   * Retrieves the class that is marked with the {@link LDAPObject} annotation
   * and contains the associated field.
   *
   * @return  The class that contains the associated field.
   */
  Class<?> getContainingClass()
  {
    return containingClass;
  }



  /**
   * Indicates whether attempts to initialize an object should fail if the LDAP
   * attribute has a value that cannot be represented in the argument type for
   * the associated method.
   *
   * @return  {@code true} if an exception should be thrown if an LDAP attribute
   *          has a value that cannot be provided as an argument to the
   *          associated method, or {@code false} if the method should not be
   *          invoked.
   */
  boolean failOnInvalidValue()
  {
    return failOnInvalidValue;
  }



  /**
   * Indicates whether attempts to initialize an object should fail if the
   * LDAP attribute has multiple values but the associated method argument can
   * only hold a single value.  Note that the value returned from this method
   * may be {@code false} even when the annotation has a value of {@code true}
   * if the associated method takes an argument that supports multiple values.
   *
   * @return  {@code true} if an exception should be thrown if an attribute has
   *          too many values to provide to the associated method, or
   *          {@code false} if the first value returned should be provided as an
   *          argument to the associated method.
   */
  boolean failOnTooManyValues()
  {
    return failOnTooManyValues;
  }



  /**
   * Retrieves the encoder that should be used for the associated method.
   *
   * @return  The encoder that should be used for the associated method.
   */
  LDAPFieldEncoder getEncoder()
  {
    return encoder;
  }



  /**
   * Retrieves the name of the LDAP attribute used to hold values for the
   * associated method.
   *
   * @return  The name of the LDAP attribute used to hold values for the
   *          associated method.
   */
  String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Indicates whether the associated method takes an argument that can hold
   * multiple values.
   *
   * @return  {@code true} if the associated method takes an argument that can
   *          hold multiple values, or {@code false} if not.
   */
  boolean supportsMultipleValues()
  {
    return supportsMultipleValues;
  }



  /**
   * Invokes the setter method on the provided object with the value from the
   * given attribute.
   *
   * @param  o  The object for which to invoke the setter method.
   * @param  e  The entry being decoded.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                invoke the setter method.
   */
  void invokeSetter(final Object o, final Entry e)
       throws LDAPPersistException
  {
    final Attribute a = e.getAttribute(attributeName);
    if (a == null)
    {
      encoder.setNull(method, o);
      return;
    }

    if (failOnTooManyValues && (a.size() > 1))
    {
      throw new LDAPPersistException(ERR_SETTER_INFO_METHOD_NOT_MULTIVALUED.get(
           method.getName(), a.getName(), containingClass.getName()));
    }

    try
    {
      encoder.invokeSetter(method, o, a);
    }
    catch (LDAPPersistException lpe)
    {
      debugException(lpe);
      if (failOnInvalidValue)
      {
        throw lpe;
      }
    }
  }
}