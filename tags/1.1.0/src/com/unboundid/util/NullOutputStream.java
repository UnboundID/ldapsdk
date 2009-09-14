/*
 * Copyright 2008-2009 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2009 UnboundID Corp.
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
package com.unboundid.util;



import java.io.OutputStream;
import java.io.PrintStream;



/**
 * This class provides an implementation of a {@code java.io.OutputStream} in
 * which any data written to it is simply discarded.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class NullOutputStream
       extends OutputStream
{
  /**
   * The singleton instance of this null output stream that may be reused
   * instead of creating a new instance.
   */
  private static final NullOutputStream INSTANCE = new NullOutputStream();



  /**
   * The singleton instance of a print stream based on this null output stream
   * that may be reused instead of creating a new instance.
   */
  private static final PrintStream PRINT_STREAM = new PrintStream(INSTANCE);



  /**
   * Creates a new null output stream instance.
   */
  public NullOutputStream()
  {
    // No implementation is required.
  }



  /**
   * Retrieves an instance of this null output stream.
   *
   * @return  An instance of this null output stream.
   */
  public static NullOutputStream getInstance()
  {
    return INSTANCE;
  }



  /**
   * Retrieves a print stream based on this null output stream.
   *
   * @return  A print stream based on this null output stream.
   */
  public static PrintStream getPrintStream()
  {
    return PRINT_STREAM;
  }



  /**
   * Closes this output stream.  This has no effect.
   */
  @Override()
  public void close()
  {
    // No implementation is required.
  }



  /**
   * Flushes the contents of this output stream.  This has no effect.
   */
  @Override()
  public void flush()
  {
    // No implementation is required.
  }



  /**
   * Writes the contents of the provided byte array over this output stream.
   * This has no effect.
   *
   * @param  b  The byte array containing the data to be written.
   */
  @Override()
  public void write(final byte[] b)
  {
    // No implementation is required.
  }



  /**
   * Writes the contents of the provided byte array over this output stream.
   * This has no effect.
   *
   * @param  b    The byte array containing the data to be written.
   * @param  off  The position in the array at which to start writing data.
   * @param  len  The number of bytes to be written.
   */
  @Override()
  public void write(final byte[] b, final int off, final int len)
  {
    // No implementation is required.
  }



  /**
   * Writes the provided byte over this input stream.  This has no effect.
   *
   * @param  b  The byte to be written.
   */
  @Override()
  public void write(final int b)
  {
    // No implementation is required.
  }
}
