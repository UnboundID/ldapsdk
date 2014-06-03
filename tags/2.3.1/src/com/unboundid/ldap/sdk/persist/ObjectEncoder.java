/*
 * Copyright 2009-2012 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2012 UnboundID Corp.
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.util.Extensible;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.persist.PersistMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides an API for converting between Java objects and LDAP
 * attributes.  Concrete instances of this class must provide a default
 * zero-argument constructor.
 */
@Extensible()
@ThreadSafety(level=ThreadSafetyLevel.INTERFACE_THREADSAFE)
public abstract class ObjectEncoder
       implements Serializable
{
  /**
   * Indicates whether this object encoder may be used to encode or decode
   * objects of the specified type.
   *
   * @param  t  The type of object for which to make the determination.
   *
   * @return  {@code true} if this object encoder may be used for objects of
   *          the specified type, or {@code false} if not.
   */
  public abstract boolean supportsType(final Type t);



  /**
   * Constructs a definition for an LDAP attribute type which may be added to
   * the directory server schema to allow it to hold the value of the specified
   * field.  Note that the object identifier used for the constructed attribute
   * type definition is not required to be valid or unique.
   *
   * @param  f  The field for which to construct an LDAP attribute type
   *            definition.  It will include the {@link LDAPField} annotation
   *            type.
   *
   * @return  The constructed attribute type definition.
   *
   * @throws  LDAPPersistException  If this object encoder does not support
   *                                encoding values for the associated field
   *                                type.
   */
  public final AttributeTypeDefinition constructAttributeType(final Field f)
         throws LDAPPersistException
  {
    return constructAttributeType(f, DefaultOIDAllocator.getInstance());
  }



  /**
   * Constructs a definition for an LDAP attribute type which may be added to
   * the directory server schema to allow it to hold the value of the specified
   * field.
   *
   * @param  f  The field for which to construct an LDAP attribute type
   *            definition.  It will include the {@link LDAPField} annotation
   *            type.
   * @param  a  The OID allocator to use to generate the object identifier.  It
   *            must not be {@code null}.
   *
   * @return  The constructed attribute type definition.
   *
   * @throws  LDAPPersistException  If this object encoder does not support
   *                                encoding values for the associated field
   *                                type.
   */
  public abstract AttributeTypeDefinition constructAttributeType(final Field f,
                                               final OIDAllocator a)
         throws LDAPPersistException;



  /**
   * Constructs a definition for an LDAP attribute type which may be added to
   * the directory server schema to allow it to hold the value returned by the
   * specified method.  Note that the object identifier used for the constructed
   * attribute type definition is not required to be valid or unique.
   *
   * @param  m  The method for which to construct an LDAP attribute type
   *            definition.  It will include the {@link LDAPGetter}
   *            annotation type.
   *
   * @return  The constructed attribute type definition.
   *
   * @throws  LDAPPersistException  If this object encoder does not support
   *                                encoding values for the associated method
   *                                type.
   */
  public final AttributeTypeDefinition constructAttributeType(final Method m)
         throws LDAPPersistException
  {
    return constructAttributeType(m, DefaultOIDAllocator.getInstance());
  }



  /**
   * Constructs a definition for an LDAP attribute type which may be added to
   * the directory server schema to allow it to hold the value returned by the
   * specified method.  Note that the object identifier used for the constructed
   * attribute type definition is not required to be valid or unique.
   *
   * @param  m  The method for which to construct an LDAP attribute type
   *            definition.  It will include the {@link LDAPGetter}
   *            annotation type.
   * @param  a  The OID allocator to use to generate the object identifier.  It
   *            must not be {@code null}.
   *
   * @return  The constructed attribute type definition.
   *
   * @throws  LDAPPersistException  If this object encoder does not support
   *                                encoding values for the associated method
   *                                type.
   */
  public abstract AttributeTypeDefinition constructAttributeType(final Method m,
                                               final OIDAllocator a)
         throws LDAPPersistException;



  /**
   * Indicates whether the provided field can hold multiple values.
   *
   * @param  field  The field for which to make the determination.  It must be
   *                marked with the {@link LDAPField} annotation.
   *
   * @return  {@code true} if the provided field can hold multiple values, or
   *          {@code false} if not.
   */
  public abstract boolean supportsMultipleValues(final Field field);



  /**
   * Indicates whether the provided setter method takes an argument that can
   * hold multiple values.
   *
   * @param  method  The setter method for which to make the determination.  It
   *                 must be marked with the {@link LDAPSetter} annotation
   *                 type and conform to the constraints associated with that
   *                 annotation.
   *
   * @return  {@code true} if the provided method takes an argument that can
   *          hold multiple values, or {@code false} if not.
   */
  public abstract boolean supportsMultipleValues(final Method method);



  /**
   * Encodes the provided field to an LDAP attribute.
   *
   * @param  field  The field to be encoded.
   * @param  value  The value for the field in the object to be encoded.
   * @param  name   The name to use for the constructed attribute.
   *
   * @return  The attribute containing the encoded representation of the
   *          provided field.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                construct an attribute for the field.
   */
  public abstract Attribute encodeFieldValue(final Field field,
                                             final Object value,
                                             final String name)
         throws LDAPPersistException;



  /**
   * Encodes the provided method to an LDAP attribute.
   *
   * @param  method  The method to be encoded.
   * @param  value   The value returned by the method in the object to be
   *                 encoded.
   * @param  name    The name to use for the constructed attribute.
   *
   * @return  The attribute containing the encoded representation of the
   *          provided method value.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                construct an attribute for the method.
   */
  public abstract Attribute encodeMethodValue(final Method method,
                                              final Object value,
                                              final String name)
         throws LDAPPersistException;



  /**
   * Updates the provided object to assign a value for the specified field from
   * the contents of the given attribute.
   *
   * @param  field      The field to update in the provided object.
   * @param  object     The object to be updated.
   * @param  attribute  The attribute whose value(s) should be used to update
   *                    the specified field in the given object.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                assign a value to the specified field.
   */
  public abstract void decodeField(final Field field, final Object object,
                                   final Attribute attribute)
         throws LDAPPersistException;



  /**
   * Assigns a {@code null} value to the provided field, if possible.  If the
   * field type is primitive and cannot be assigned a {@code null} value, then a
   * default primitive value will be assigned instead (0 for numeric values,
   * false for {@code boolean} values, and the null character for {@code char}
   * values).
   *
   * @param  f  The field to which the {@code null} value should be assigned.
   *            It must not be {@code null} and must be marked with the
   *            {@link LDAPField} annotation.
   * @param  o  The object to be updated.  It must not be {@code null}, and the
   *            class must be marked with the {@link LDAPObject annotation}.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                assign a {@code null} value to the specified
   *                                field.
   */
  public void setNull(final Field f, final Object o)
         throws LDAPPersistException
  {
    try
    {
      f.setAccessible(true);

      final Class<?> type = f.getType();
      if (type.equals(Boolean.TYPE))
      {
        f.set(o, Boolean.FALSE);
      }
      else if (type.equals(Byte.TYPE))
      {
        f.set(o, (byte) 0);
      }
      else if (type.equals(Character.TYPE))
      {
        f.set(o, '\u0000');
      }
      else if (type.equals(Double.TYPE))
      {
        f.set(o, 0.0d);
      }
      else if (type.equals(Float.TYPE))
      {
        f.set(o, 0.0f);
      }
      else if (type.equals(Integer.TYPE))
      {
        f.set(o, 0);
      }
      else if (type.equals(Long.TYPE))
      {
        f.set(o, 0L);
      }
      else if (type.equals(Short.TYPE))
      {
        f.set(o, (short) 0);
      }
      else
      {
        f.set(o, null);
      }
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPPersistException(
           ERR_ENCODER_CANNOT_SET_NULL_FIELD_VALUE.get(f.getName(),
                o.getClass().getName(), getExceptionMessage(e)), e);
    }
  }



  /**
   * Invokes the provided setter method with a single argument that will set a
   * {@code null} value for that method, if possible.  If the argument type is
   * and cannot be assigned a {@code null} value, then a default primitive value
   * will be assigned instead (0 for numeric values, false for {@code boolean}
   * values, and the null character for {@code char} values).
   *
   * @param  m  The setter method that should be used to set the {@code null}
   *            value.  It must not be {@code null}, and must have the
   *            {@code LDAPSetter} annotation.
   * @param  o  The object to be updated.  It must not be {@code null}, and the
   *            class must be marked with the {@link LDAPObject annotation}.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                assign a {@code null} value to the specified
   *                                field.
   */
  public void setNull(final Method m, final Object o)
         throws LDAPPersistException
  {
    try
    {
      m.setAccessible(true);

      final Class<?> type = m.getParameterTypes()[0];
      if (type.equals(Boolean.TYPE))
      {
        m.invoke(o, Boolean.FALSE);
      }
      else if (type.equals(Byte.TYPE))
      {
        m.invoke(o, (byte) 0);
      }
      else if (type.equals(Character.TYPE))
      {
        m.invoke(o, '\u0000');
      }
      else if (type.equals(Double.TYPE))
      {
        m.invoke(o, 0.0d);
      }
      else if (type.equals(Float.TYPE))
      {
        m.invoke(o, 0.0f);
      }
      else if (type.equals(Integer.TYPE))
      {
        m.invoke(o, 0);
      }
      else if (type.equals(Long.TYPE))
      {
        m.invoke(o, 0L);
      }
      else if (type.equals(Short.TYPE))
      {
        m.invoke(o, (short) 0);
      }
      else
      {
        m.invoke(o, type.cast(null));
      }
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPPersistException(
           ERR_ENCODER_CANNOT_SET_NULL_METHOD_VALUE.get(m.getName(),
                o.getClass().getName(), getExceptionMessage(e)), e);
    }
  }



  /**
   * Updates the provided object to invoke the specified method to set a value
   * from the contents of the given attribute.
   *
   * @param  method     The method to invoke in the provided object.
   * @param  object     The object to be updated.
   * @param  attribute  The attribute whose value(s) should be used to update
   *                    the specified method in the given object.
   *
   * @throws  LDAPPersistException  If a problem occurs while attempting to
   *                                determine the value or invoke the specified
   *                                method.
   */
  public abstract void invokeSetter(final Method method, final Object object,
                                    final Attribute attribute)
         throws LDAPPersistException;
}