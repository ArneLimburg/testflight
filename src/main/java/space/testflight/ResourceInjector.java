/*
 * Copyright 2021 Arne Limburg
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
package space.testflight;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static space.testflight.FlywayExtension.JDBC_PASSWORD;
import static space.testflight.FlywayExtension.JDBC_URL;
import static space.testflight.FlywayExtension.JDBC_USERNAME;
import static space.testflight.FlywayExtension.getContainerStore;
import static space.testflight.FlywayExtension.getConfiguration;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.sql.Connection;
import java.util.List;

import org.junit.jupiter.api.extension.ExtensionContext;

public class ResourceInjector {

  private static final List<String> URL_NAMES = unmodifiableList(asList("jdbcurl", "url"));
  private static final List<String> USER_NAMES = unmodifiableList(asList("jdbcuser", "username", "user"));
  private static final List<String> PASSWORD_NAMES = unmodifiableList(asList("jdbcpassword", "password"));
  private static final String CONNECTION_HOLDER_TYPE_NAME = "com.github.database.rider.core.api.connection.ConnectionHolder";
  private static final String CONNECTION_HOLDER_IMPL_TYPE_NAME = "com.github.database.rider.core.connection.ConnectionHolderImpl";

  public boolean canInject(AnnotatedElement element) {
    return element.isAnnotationPresent(TestResource.class);
  }

  public void inject(Object instance, ExtensionContext context) throws ReflectiveOperationException {
    inject(instance.getClass(), instance, context);
  }

  private void inject(Class<?> type, Object instance, ExtensionContext context) throws ReflectiveOperationException {
    if (type == null) {
      return;
    }
    inject(type.getSuperclass(), instance, context);
    for (Field field: type.getDeclaredFields()) {
      if (canInject(field)) {
        inject(instance, field, context);
      }
    }
  }

  public void inject(Object instance, Field field, ExtensionContext context) throws ReflectiveOperationException {
    field.setAccessible(true);
    field.set(instance, getInjectedValue(field.getType(), field.getName(), context));
  }

  public Object getInjectedValue(Parameter parameter, ExtensionContext context) throws ReflectiveOperationException {
    return getInjectedValue(parameter.getType(), parameter.getName(), context);
  }

  public Object getInjectedValue(Class<?> type, String name, ExtensionContext context) throws ReflectiveOperationException {
    if (type.equals(Connection.class)) {
      return getProxyConnection(context);
    } else if (type.getName().equals(CONNECTION_HOLDER_TYPE_NAME)) {
      return getConnectionHolder(type, context);
    } else if (type.equals(URI.class)) {
      return URI.create(getJdbcUrl(context));
    } else if (type.equals(String.class)) {
      String caseInsensitiveName = name.toLowerCase();
      if (URL_NAMES.contains(caseInsensitiveName)) {
        return getJdbcUrl(context);
      } else if (USER_NAMES.contains(caseInsensitiveName)) {
        return getJdbcUser(context);
      } else if (PASSWORD_NAMES.contains(caseInsensitiveName)) {
        return getJdbcPassword(context);
      }
      throw new IllegalStateException(format("Could not inject %s: name not supported", name));
    } else {
      throw new IllegalStateException(format("Could not inject %s: unsupported type %s", name, type.getName()));
    }
  }

  private Object getProxyConnection(ExtensionContext context) {
    return Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class[] {Connection.class},
      new ConnectionHandler(getJdbcUrl(context), getJdbcUser(context), getJdbcPassword(context)));
  }

  private Object getConnectionHolder(Class<?> type, ExtensionContext context) throws ReflectiveOperationException {
    Class<?> connectionHolderImplClass = type.getClassLoader().loadClass(CONNECTION_HOLDER_IMPL_TYPE_NAME);
    Constructor<?> constructor = connectionHolderImplClass.getConstructor(Connection.class);
    return constructor.newInstance(getProxyConnection(context));
  }

  private String getJdbcUrl(ExtensionContext context) {
    return getContainerStore(context, getScope(context)).get(JDBC_URL, String.class);
  }

  private String getJdbcUser(ExtensionContext context) {
    return getContainerStore(context, getScope(context)).get(JDBC_USERNAME, String.class);
  }

  private String getJdbcPassword(ExtensionContext context) {
    return getContainerStore(context, getScope(context)).get(JDBC_PASSWORD, String.class);
  }

  private DatabaseInstanceScope getScope(ExtensionContext context) {
    return getConfiguration(context).getDatabaseInstanceScope();
  }
}
