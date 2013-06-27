/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user;

import org.junit.Test;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.exceptions.ForbiddenException;

import java.util.Arrays;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserSessionTest {
  @Test
  public void getSession_get_anonymous_by_default() throws Exception {
    UserSession.remove();

    UserSession session = UserSession.get();

    assertThat(session).isNotNull();
    assertThat(session.login()).isNull();
    assertThat(session.userId()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
    // default locale
    assertThat(session.locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void getSession() throws Exception {
    UserSession.set(new UserSession().setUserId(123).setLogin("karadoc").setLocale(Locale.FRENCH));

    UserSession session = UserSession.get();
    assertThat(session).isNotNull();
    assertThat(session.userId()).isEqualTo(123);
    assertThat(session.login()).isEqualTo("karadoc");
    assertThat(session.isLoggedIn()).isTrue();
    assertThat(session.locale()).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void hasPermission() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao);

    when(authorizationDao.selectGlobalPermissions("marius")).thenReturn(Arrays.asList("shareIssueFilter", "admin"));

    assertThat(session.hasPermission("shareIssueFilter")).isTrue();
    assertThat(session.hasPermission("admin")).isTrue();
    assertThat(session.hasPermission("shareDashboard")).isFalse();
  }

  @Test
  public void login_should_not_be_empty() throws Exception {
    UserSession session = new UserSession().setLogin("");
    assertThat(session.login()).isNull();
    assertThat(session.isLoggedIn()).isFalse();
  }

  @Test
  public void checkPermission_ok() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao);

    when(authorizationDao.selectGlobalPermissions("marius")).thenReturn(Arrays.asList("shareIssueFilter", "admin"));

    session.checkPermission("shareIssueFilter");
  }

  @Test(expected = ForbiddenException.class)
  public void checkPermission_ko() throws Exception {
    AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
    UserSession session = new SpyUserSession("marius", authorizationDao);

    when(authorizationDao.selectGlobalPermissions("marius")).thenReturn(Arrays.asList("shareIssueFilter", "admin"));

    session.checkPermission("shareDashboard");
  }

  static class SpyUserSession extends UserSession {
    private AuthorizationDao authorizationDao;

    SpyUserSession(String login, AuthorizationDao authorizationDao) {
      this.authorizationDao = authorizationDao;
      setLogin(login);
    }

    @Override
    AuthorizationDao authorizationDao() {
      return authorizationDao;
    }
  }
}
