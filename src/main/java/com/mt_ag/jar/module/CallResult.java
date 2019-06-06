package com.mt_ag.jar.module;

import java.util.List;

/**
 * The result of the call.
 * The exit value and the written lines.
 */
public class CallResult {

  /**
   * The exit val.
   */
  private final int exitVal;

  /**
   * The out lines.
   */
  private final List<String> outLines;

  /**
   * The constructor with all fields.
   * @param exitValue the exit val.
   * @param lines the lines.
   */
  public CallResult(int exitValue, List<String> lines) {
    exitVal = exitValue;
    outLines = lines;
  }

  /**
   * Gets the exit val.
   * @return the exit val.
   */
  public int getExitVal() {
    return exitVal;
  }

  /**
   * Gets the out lines.
   * @return the out lines.
   */
  public List<String> getOutLines() {
    return outLines;
  }
}
