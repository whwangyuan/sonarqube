/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.server.authentication.UserSessionInitializer;
import org.sonar.server.platform.Platform;
import org.sonar.server.settings.ThreadLocalSettings;

public class UserSessionFilter implements Filter {

  private final Platform platform;

  public UserSessionFilter() {
    this.platform = Platform.getInstance();
  }

  @VisibleForTesting
  UserSessionFilter(Platform platform) {
    this.platform = platform;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    ThreadLocalSettings settings = platform.getContainer().getComponentByType(ThreadLocalSettings.class);
    UserSessionInitializer userSessionInitializer = platform.getContainer().getComponentByType(UserSessionInitializer.class);

    settings.load();
    try {
      if (userSessionInitializer == null || userSessionInitializer.initUserSession(request, response)) {
        chain.doFilter(servletRequest, servletResponse);
      }
    } finally {
      if (userSessionInitializer != null) {
        userSessionInitializer.removeUserSession();
      }
      settings.unload();
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // nothing to do
  }

  @Override
  public void destroy() {
    // nothing to do
  }
}
