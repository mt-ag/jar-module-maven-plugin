package com.mt_ag.jar.module;

/**
 * Enum of the compress config of the image in jlink.
 */
public enum CompressEnum {
  /**
   * No compression is used.
   */
  NoCompress(0),
  /**
   * Constant string sharing is used.
   */
  ConstantStringSharing(1),
  /**
   * Zips the data.
   */
  ZIP(2);

  /**
   * The rate for the jlink command.
   */
  private final int rate;

  /**
   * The constructor with the rate.
   * @param pRate the rate.
   */
   CompressEnum(int pRate) {
    this.rate = pRate;
  }

  /**
   * The getter of rate.
   * @return the rate.
   */
  public int getRate() {
     return rate;
  }
}
