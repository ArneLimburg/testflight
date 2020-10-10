/*
 * Copyright 2020 René Frerichs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.openknowledge.extensions;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class User {

  @Column
  String userName;

  @Column
  String email;

  public User(String userName, String email) {
    this.userName = userName;
    this.email = email;
  }
}

