/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.variables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiFunction;

import org.eclipse.jdt.annotation.Nullable;

import com.zaxxer.hikari.HikariConfig;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.util.NonNullPair;

public class MySQLStorage extends SQLStorage {

	MySQLStorage(String name) {
		super(name, "CREATE TABLE IF NOT EXISTS %s (" +
				"rowid        BIGINT  NOT NULL  AUTO_INCREMENT  PRIMARY KEY," +
				"name         VARCHAR(" + MAX_VARIABLE_NAME_LENGTH + ")  NOT NULL  UNIQUE," +
				"type         VARCHAR(" + MAX_CLASS_CODENAME_LENGTH + ")," +
				"value        BLOB(" + MAX_VALUE_SIZE + ")" +
				") CHARACTER SET ucs2 COLLATE ucs2_bin");
	}

	@Override
	@Nullable
	public HikariConfig configuration(SectionNode config) {
		String host = getValue(config, "host");
		Integer port = getValue(config, "port", Integer.class);
		String database = getValue(config, "database");
		if (host == null || port == null || database == null)
			return null;

		HikariConfig configuration = new HikariConfig();
		configuration.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
		configuration.setUsername(getValue(config, "user"));
		configuration.setPassword(getValue(config, "password"));

		setTableName(config.get("table", "variables21"));
		return configuration;
	}

	@Override
	protected boolean requiresFile() {
		return false;
	}

	@Override
	protected String getReplaceQuery() {
		return "REPLACE INTO " + getTableName() + " (name, type, value, update_guid) VALUES (?, ?, ?, ?)";
	}

	@Override
	protected @Nullable NonNullPair<String, String> getMonitorQueries() {
		return new NonNullPair<>(
				"SELECT rowid, name, type, value FROM " + getTableName() + " WHERE rowid > ?",
				"DELETE FROM " + getTableName() + " WHERE value IS NULL AND rowid < ?"
		);
	}

	@Override
	protected String getSelectQuery() {
		return "SELECT rowid, name, type, value from " + getTableName();
	}

	@Override
	protected BiFunction<Integer, ResultSet, VariableResult> get() {
		return (index, result) -> {
			int i = 1;
			try {
				long rowid = result.getLong(i++);
				String name = result.getString(i++);
				if (name == null) {
					Skript.error("Variable with NULL name found in the database '" + databaseName + "', ignoring it");
					return null;
				}
				String type = result.getString(i++);
				byte[] value = result.getBytes(i++);
				return new VariableResult(name, type, value);
			} catch (SQLException e) {
				return new VariableResult(e);
			}
		};
	}

}
