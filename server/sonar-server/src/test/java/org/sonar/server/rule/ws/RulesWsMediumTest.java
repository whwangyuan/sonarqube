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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesWsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private static final String API_ENDPOINT = "api/rules";
  private static final String API_SHOW_METHOD = "show";
  private static final String API_TAGS_METHOD = "tags";

  DbClient db;
  RulesWs ws;
  RuleDao ruleDao;
  DbSession session;
  RuleIndexer ruleIndexer;
  ActiveRuleIndexer activeRuleIndexer;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWs.class);
    session = tester.get(DbClient.class).openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    ruleIndexer.setEnabled(true);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    activeRuleIndexer.setEnabled(true);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define() {
    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller(API_ENDPOINT);

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(9);
    assertThat(controller.action("search")).isNotNull();
    assertThat(controller.action(API_SHOW_METHOD)).isNotNull();
    assertThat(controller.action(API_TAGS_METHOD)).isNotNull();
    assertThat(controller.action("update")).isNotNull();
    assertThat(controller.action("create")).isNotNull();
    assertThat(controller.action("delete")).isNotNull();
    assertThat(controller.action("repositories")).isNotNull();
    assertThat(controller.action("app")).isNotNull();
  }

  @Test
  public void show_rule() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = RuleTesting.newXooX1();
    ruleDao.insert(session, rule);

    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(profile, rule).setSeverity("BLOCKER");
    tester.get(ActiveRuleDao.class).insert(session, activeRuleDto);

    session.commit();
    session.clearCache();
    ruleIndexer.index();
    activeRuleIndexer.index();

    // 1. With Activation
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SHOW_METHOD);
    request.setParam(ShowAction.PARAM_KEY, rule.getKey().toString());
    request.setParam(ShowAction.PARAM_ACTIVES, "true");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "show_rule_active.json");

    // 1. Default Activation (defaults to false)
    request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SHOW_METHOD);
    request.setParam(ShowAction.PARAM_KEY, rule.getKey().toString());
    result = request.execute();
    result.assertJson(this.getClass(), "show_rule_no_active.json");
  }

  @Test
  public void get_tags() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = RuleTesting.newXooX1().
      setTags(ImmutableSet.of("hello", "world"))
      .setSystemTags(Collections.<String>emptySet());
    ruleDao.insert(session, rule);

    RuleDto rule2 = RuleTesting.newXooX2()
      .setTags(ImmutableSet.of("hello", "java"))
      .setSystemTags(ImmutableSet.of("sys1"));
    ruleDao.insert(session, rule2);

    session.commit();
    ruleIndexer.index();

    tester.wsTester().newGetRequest(API_ENDPOINT, API_TAGS_METHOD).execute().assertJson(this.getClass(), "get_tags.json");
    tester.wsTester().newGetRequest(API_ENDPOINT, API_TAGS_METHOD)
      .setParam("ps", "1").execute().assertJson(this.getClass(), "get_tags_limited.json");
    tester.wsTester().newGetRequest(API_ENDPOINT, API_TAGS_METHOD)
      .setParam("q", "ll").execute().assertJson(this.getClass(), "get_tags_filtered.json");
  }

}
