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
package com.unboundid.util.json;



import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides an implementation of a JSON value that represents a null
 * value.  The string representation of the null value is {@code null} in all
 * lowercase and without any quotation marks.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class JSONNull
       extends JSONValue
{
  /**
   * A pre-allocated JSON null value object.
   */
  public static final JSONNull NULL = new JSONNull();



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -8359265286375144526L;



  /**
   * Creates a new JSON value capable of representing a {@code null} value.
   */
  public JSONNull()
  {
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int hashCode()
  {
    return 1;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean equals(final Object o)
  {
    return ((o == this) || (o instanceof JSONNull));
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean equals(final JSONValue v, final boolean ignoreFieldNameCase,
                        final boolean ignoreValueCase,
                        final boolean ignoreArrayOrder)
  {
    return (v instanceof JSONNull);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String toString()
  {
    return "null";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("null");
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String toNormalizedString()
  {
    return "null";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toNormalizedString(final StringBuilder buffer)
  {
    buffer.append("null");
  }
}
