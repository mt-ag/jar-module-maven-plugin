package com.mt_ag.jar.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface TestTools {
  /**
   * Deletes a dir and its files.
   *
   * @param dir the dir to delete.
   */
  static void deleteDir(Path dir) {
    try (Stream<Path> pathStream = Files.list(dir)) {
      pathStream.forEach(path -> {
        if (Files.isDirectory(path)) {
          deleteDir(path);
        } else {
          try {
            Files.delete(path);
          } catch (IOException e) {
            System.out.println("Error in removing file: " + path);
          }
        }
      });
    } catch (IOException e) {
      System.out.println("Error in reading files in dir: " + dir);
    }
    try {
      Files.delete(dir);
    } catch (IOException e) {
      System.out.println("Error in removing dir: " + dir);
    }
  }
}
