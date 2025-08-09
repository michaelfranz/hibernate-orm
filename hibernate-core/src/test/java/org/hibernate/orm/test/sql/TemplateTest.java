/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;


import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.Template;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel
public class TemplateTest {

	@Test
	@JiraKey("HHH-18256")
	public void templateLiterals(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();
		assertWhereStringTemplate( "'Knock, knock! Who''s there?'",
				"'Knock, knock! Who''s there?'", factory );
		assertWhereStringTemplate( "1e-5 + 2 * 3.0",
				"1e-5 + 2 * 3.0", factory );
		assertWhereStringTemplate( "hello",
				"{@}.hello", factory );
		assertWhereStringTemplate( "`hello`",
				"{@}." + dialect.quote("`hello`"), factory );
		assertWhereStringTemplate( dialect.openQuote() + "hello" + dialect.closeQuote(),
				"{@}." + dialect.quote("`hello`"), factory );
		assertWhereStringTemplate( "hello.world",
				"hello.world", factory );
		assertWhereStringTemplate( "'hello there' || ' ' || 'world'",
				"'hello there' || ' ' || 'world'", factory );
		assertWhereStringTemplate( "hello + world",
				"{@}.hello + {@}.world", factory );
		assertWhereStringTemplate( "upper(hello) || lower(world)",
				"upper({@}.hello) || lower({@}.world)", factory );
		assertWhereStringTemplate( "extract(hour from time)",
				"extract(hour from {@}.time)", factory );
		assertWhereStringTemplate( "extract(day from date)",
				"extract(day from {@}.date)", factory );
		assertWhereStringTemplate( "left(hello,4) || right(world,5)",
				"left({@}.hello,4) || right({@}.world,5)", factory );
		assertWhereStringTemplate( "N'a'", factory );
		assertWhereStringTemplate( "X'a'", factory );
		assertWhereStringTemplate( "BX'a'", factory);
		assertWhereStringTemplate( "VARBYTE'a'", factory );
		assertWhereStringTemplate( "bytea 'a'", factory );
		assertWhereStringTemplate( "bytea  'a'", factory );
		assertWhereStringTemplate( "date 'a'", factory );
		assertWhereStringTemplate( "time 'a'", factory );
		assertWhereStringTemplate( "timestamp 'a'", factory );
		assertWhereStringTemplate( "timestamp with time zone 'a'", factory );
		assertWhereStringTemplate( "time with time zone 'a'", factory );
		assertWhereStringTemplate( "date", "{@}.date", factory );
		assertWhereStringTemplate( "time", "{@}.time", factory );
		assertWhereStringTemplate( "zone", "{@}.zone", factory );
		assertWhereStringTemplate("select date from thetable",
				"select {@}.date from thetable", factory );
		assertWhereStringTemplate("select date '2000-12-1' from thetable",
				"select date '2000-12-1' from thetable", factory );
		assertWhereStringTemplate("where date between date '2000-12-1' and date '2002-12-2'",
				"where {@}.date between date '2000-12-1' and date '2002-12-2'", factory );
		assertWhereStringTemplate("where foo>10 and bar is not null",
				"where {@}.foo>10 and {@}.bar is not null", factory );
		assertWhereStringTemplate("select t.foo, o.bar from table as t left join other as o on t.id = o.id where t.foo>10 and o.bar is not null order by o.bar",
				"select t.foo, o.bar from table as t left join other as o on t.id = o.id where t.foo>10 and o.bar is not null order by o.bar", factory );
		assertWhereStringTemplate( "CAST(foo AS unsigned)",
				"CAST({@}.foo AS unsigned)", factory );
		assertWhereStringTemplate( "CAST(foo AS signed)",
				"CAST({@}.foo AS signed)", factory );
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFetchGrammarVsColumnNames(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();

		// Test that "first" and "next" are treated as keywords when part of FETCH grammar
		assertWhereStringTemplate( "fetch first 10 rows only", "fetch first 10 rows only", factory );
		assertWhereStringTemplate( "fetch next 5 rows only", "fetch next 5 rows only", factory );
		assertWhereStringTemplate( "select * from table fetch first 1 row only",
				"select * from table fetch first 1 row only", factory );

		// Mixed scenarios: ensure identifiers around FETCH grammar are still qualified
		assertWhereStringTemplate( "select first_name from users fetch first 10 rows only",
				"select {@}.first_name from users fetch first 10 rows only", factory );
		assertWhereStringTemplate( "where fetch_count > 5 and fetch next 1 row only",
				"where {@}.fetch_count > 5 and fetch next 1 row only", factory );
		assertWhereStringTemplate( "select first from users fetch first 10 rows only",
				"select {@}.first from users fetch first 10 rows only", factory );
		assertWhereStringTemplate( "select next from users fetch next 10 rows only",
				"select {@}.next from users fetch next 10 rows only", factory );
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFetchGrammarVariants(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();

		// Variants of FETCH FIRST/NEXT
		assertWhereStringTemplate( "fetch first 1 row only", "fetch first 1 row only", factory );
		assertWhereStringTemplate( "fetch next 10 rows only", "fetch next 10 rows only", factory );

		// Parameterized row count
		assertWhereStringTemplate( "fetch next ? rows only", "fetch next ? rows only", factory );

		// Casing variants
		assertWhereStringTemplate( "FETCH First 10 ROWS ONLY", "FETCH First 10 ROWS ONLY", factory );

		// Extra whitespace and newlines
		assertWhereStringTemplate( "fetch    first   10   rows   only", "fetch    first   10   rows   only", factory );
		assertWhereStringTemplate( "fetch\nfirst 3 rows only", "fetch\nfirst 3 rows only", factory );

		// State reset after ONLY: trailing 'next' should be qualified
		assertWhereStringTemplate( "fetch next 1 rows only and next > 5",
				"fetch next 1 rows only and {@}.next > 5", factory );

		// Qualified identifier should remain as-is
		assertWhereStringTemplate( "select u.first from users u fetch first 1 row only",
				"select u.first from users u fetch first 1 row only", factory );

		// Quoted identifier should be qualified, while FETCH clause remains unqualified
		assertWhereStringTemplate( "select `first` from users fetch first 1 row only",
				"select {@}." + dialect.quote("`first`") + " from users fetch first 1 row only", factory );
	}

	@Test
	@JiraKey("HHH-19695")
	public void testNamedParametersAndBooleans(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();
		final String expectedTrue = dialect.toBooleanValueString(true);
		final String expectedFalse = dialect.toBooleanValueString(false);
		assertWhereStringTemplate(
				"where active = true and disabled = false and status = :p",
				"where {@}.active = " + expectedTrue + " and {@}.disabled = " + expectedFalse + " and {@}.status = :p",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testQuotedIdentifierAfterDot(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();
		assertWhereStringTemplate(
				"t.`hello` = 1",
				"t." + dialect.quote("`hello`") + " = 1",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testMultipleTablesCommaSeparated(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		assertWhereStringTemplate(
				"select * from t, u where t.id = u.id",
				"select * from t, u where t.id = u.id",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testTrimAndCastClosures(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();
		// trim with FROM
		assertWhereStringTemplate(
				"trim(from name)",
				"trim(from {@}.name)",
				factory
		);
		// cast with AS and closing parenthesis resetting state
		assertWhereStringTemplate(
				"cast(`val` as varchar(10))",
				"cast({@}." + dialect.quote("`val`") + " as varchar(10))",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testLiteralPrefixesWithBlanks(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		// extra blanks between literal prefix and quote
		assertWhereStringTemplate(
				"where date between date    '2000-12-1' and date    '2002-12-2'",
				"where {@}.date between date    '2000-12-1' and date    '2002-12-2'",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testRenderTransformerReadFragment() {
		String frag = "col1 + col2";
		String rendered = Template.renderTransformerReadFragment(frag, "col1", "col2");
		org.junit.jupiter.api.Assertions.assertEquals("{@}.col1 + {@}.col2", rendered);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testCollectColumnNames(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		String rendered = Template.renderWhereStringTemplate(
				"where a = 1 and b = 2",
				factory.getJdbcServices().getDialect(),
				factory.getTypeConfiguration()
		);
		java.util.List<String> names = Template.collectColumnNames(rendered);
		org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b"), names);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testDialectQuoteTokensOpenClose(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();
		// Use dialect quote tokens directly, not backticks
		String s = dialect.openQuote() + "hello" + dialect.closeQuote() + " = 1";
		assertWhereStringTemplate(
				s,
				"{@}." + dialect.quote("`hello`") + " = 1",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testEndsWithDotPreventsAliasBeforeQuoted(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		Dialect dialect = factory.getJdbcServices().getDialect();
		assertWhereStringTemplate(
				"t." + dialect.openQuote() + "c" + dialect.closeQuote(),
				"t." + dialect.quote("`c`"),
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFetchAloneNotAClause(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		// 'fetch' alone should not start a fetch clause and should remain as-is
		assertWhereStringTemplate("fetch", "fetch", factory);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFetchClauseEndsOnOtherToken(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		// After a valid sequence, an unexpected token ends the clause; identifier should then be qualified
		assertWhereStringTemplate(
				"fetch first 1 rows nonsense",
				"fetch first 1 rows {@}.nonsense",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testIsTypePreventsQualification(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		// A known type token should not be qualified
		assertWhereStringTemplate(
				"varchar + foo",
				"varchar + {@}.foo",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testTimeZoneLiteralWithoutWith(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		assertWhereStringTemplate(
				"time zone 'a'",
				"time zone 'a'",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testFunctionDetectionByParen(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		assertWhereStringTemplate(
				"coalesce(foo,bar)",
				"coalesce({@}.foo,{@}.bar)",
				factory
		);
	}

	@Test
	@JiraKey("HHH-19695")
	public void testCollectColumnNamesEndOfString() {
		// Directly exercise end-of-string capture branch
		java.util.List<String> names = Template.collectColumnNames("{@}.last");
		org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("last"), names);
	}

	private static void assertWhereStringTemplate(String sql, SessionFactoryImplementor sf) {
		assertEquals( sql,
				Template.renderWhereStringTemplate(
						sql,
						sf.getJdbcServices().getDialect(),
						sf.getTypeConfiguration()
				));
	}

	private static void assertWhereStringTemplate(String sql, String result, SessionFactoryImplementor factory) {
		assertEquals( result,
				Template.renderWhereStringTemplate(
						sql,
						factory.getJdbcServices().getDialect(),
						factory.getTypeConfiguration()
				) );
	}

}
