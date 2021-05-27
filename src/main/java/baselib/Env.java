/*
 * Copyright 2019 Raffaele Ragni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package baselib;

/**
 * Handles environment configuration read.
 * @author Raffaele Ragni
 */
public class Env {

  public interface Var {
    String name();
  }

  public String get(Var envVar) {
    var value = System.getProperty(envVar.name());
    if (value != null) {
      return value;
    }

    // Why is this line ignored:
    // Reading environment variables can cause injection in some situations.
    // The environment variable being read here are custom and not the usual ones
    // that are present in a JVM (such as user name for example).
    // Also, the application is meant to be used in a docker environment where a
    // containers is more easily controlled via environment variables.
    // If you plan to use this application outside docker, then it would be best if
    // this line is removed and the getProperty becomes the only way to receive properties
    // using the command line options, or find alternative ways to load variables.
    return System.getenv(envVar.name());//NOSONAR
  }

}
