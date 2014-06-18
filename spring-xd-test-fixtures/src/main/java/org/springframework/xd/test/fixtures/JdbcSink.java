/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.test.fixtures;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;


/**
 * Represents the {@code jdbc} sink module. Maintains an in memory relational database and exposes a
 * {@link JdbcTemplate} so that assertions can be made against it.
 *
 * @author Florent Biville
 * @author Glenn Renfro
 */
public class JdbcSink extends AbstractModuleFixture<JdbcSink> implements Disposable {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSink.class);

	private JdbcTemplate jdbcTemplate;

	private String tableName;

	private String columns;

	private boolean testOnBorrow;

	private String validationQuery;

	private volatile DataSource dataSource;

	/**
	 * Initializes a JdbcSink with the {@link DataSource}. Using this DataSource a JDBCTemplate is created.
	 *
	 * @param dataSource
	 */
	public JdbcSink(DataSource dataSource) {
		Assert.notNull(dataSource, "Datasource can not be null");
		this.dataSource = dataSource;
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		StringBuilder dsl = new StringBuilder();
		try {
			dsl.append("jdbc --initializeDatabase=true --url=" + dataSource.getConnection().getMetaData().getURL());
		}
		catch (SQLException e) {
			throw new IllegalStateException("Could not get URL from connection metadata", e);
		}
		if (tableName != null) {
			dsl.append(" --tableName=" + tableName);
		}
		if (columns != null) {
			dsl.append(" --columns=" + columns);
		}
		if (testOnBorrow) {
			dsl.append(" --testOnBorrow=" + testOnBorrow);
		}
		if (validationQuery != null) {
			dsl.append(" --validationQuery='" + validationQuery + "'");
		}

		return dsl.toString();
	}

	/**
	 * If using a in memory database this method will shutdown the database.
	 */
	@Override
	public void cleanup() {
		jdbcTemplate.execute("SHUTDOWN");
	}

	public void dropTable(String tableName) {
		Assert.hasText(tableName, "The tableName can not be empty nor null");
		try {
			jdbcTemplate.execute("drop table if exists " + tableName);
		}
		catch (DataAccessException ex) {
			LOGGER.error("Could not drop table [" + tableName + "]", ex);
		}
	}

	/**
	 * Sets the table that the sink will write to.
	 *
	 * @param tableName the name of the table.
	 * @return an instance to this jdbc sink.
	 */
	public JdbcSink tableName(String tableName) {
		this.tableName = tableName;
		return this;
	}


	/**
	 * allows a user to set the columns (comma delimited list) that the sink will write its results to.
	 *
	 * @param columns a comma delimited list of column names.
	 * @return an instance to this jdbc sink.
	 */
	public JdbcSink columns(String columns) {
		Assert.hasText(columns, "columns must not be empty nor null");
		this.columns = columns;
		return this;
	}

	/**
	 * allows a user establish that a connection will be validated before being borrowed from the pool.
	 *
	 * @param testOnBorrow a boolean .
	 * @return an instance to this jdbc sink.
	 */
	public JdbcSink testOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
		return this;
	}

	/**
	 * allows a user to set the validation query that will be used verify a connection before it is borrowed from the pool.
	 *
	 * @param validationQuery a sql query to be used to verify the db connection.
	 * @return an instance to this jdbc sink.
	 */
	public JdbcSink validationQuery(String validationQuery) {
		Assert.hasText(validationQuery, "validationQuery must not be empty nor null");
		this.validationQuery = validationQuery;
		return this;
	}

	/**
	 * Determines if a connection to the designated database can be made.
	 *
	 * @return true if a connection can be made. False if not.
	 */
	public boolean isReady() {
		boolean result = true;
		Connection conn = null;
		try {
			conn = getJdbcTemplate().getDataSource().getConnection();
		}
		catch (Exception ex) {
			result = false;
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (SQLException se) {
					// ignore exception. Sorry PMD.
				}
			}
		}
		return result;
	}

}
