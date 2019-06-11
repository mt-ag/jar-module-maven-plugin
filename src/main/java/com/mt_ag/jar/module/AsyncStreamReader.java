package com.mt_ag.jar.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A StreamReader to read the input stream as Reader.
 */
public class AsyncStreamReader implements Runnable {

  /**
   * The used reader.
   */
  private final Reader reader;

  /**
   * The writer to hold the String.
   */
  private final StringWriter writer;

  /**
   * The constructor with the reader as parameter.
   *
   * @param in the stream to read.
   */
  public AsyncStreamReader(InputStream in) {
    reader = new InputStreamReader(in, StandardCharsets.UTF_8);
    writer = new StringWriter();
  }

  /**
   * The transfer method as run implementation.
   */
  @Override
  public void run() {
    try {
      reader.transferTo(writer);
    } catch (IOException e) {
      //
    }
  }

  /**
   * Gets the result.
   *
   * @return the data as List of lines.
   */
  public List<String> getOutput() {
    return List.of(writer.toString().split("\\v+"));
  }
}
